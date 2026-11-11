package com.example.be_young04.domain.chat.service;

import com.example.be_young04.domain.analysis.service.AiAnalysisClient;
import com.example.be_young04.domain.analysis.service.CodePatchSupport;
import com.example.be_young04.domain.analysis.service.WcagAnalysisService;
import com.example.be_young04.domain.chat.dto.ChatDto.*;
import com.example.be_young04.domain.chat.entity.ChatMessage;
import com.example.be_young04.domain.chat.repository.ChatMessageRepository;
import com.example.be_young04.domain.git.dto.request.GitFileWriteRequest;
import com.example.be_young04.domain.git.service.GitWorkingTreeService;
import com.example.be_young04.domain.git.service.GitWorkspaceManager;
import com.example.be_young04.domain.realtime_analysis.dto.IssueDetailDto;
import com.example.be_young04.domain.realtime_analysis.dto.RealtimeAnalysisResponse;
import com.example.be_young04.domain.realtime_analysis.service.RealtimeAnalysisService;
import com.example.be_young04.domain.repository.repository.RepositoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 챗봇: 저장소 분석 결과·열린 파일·이전 대화를 맥락으로 질문에 답하고, 요청 시 코드 수정안을 제안한다.
 * - 맥락: 분석 결과 요약(항상) + 최근 대화(항상) + 선택한 이슈·열린 파일(조건부)
 * - 수정안은 열린 파일 전체를 AI가 본 경우에만 만들고, before 검증을 통과한 것만 반환한다.
 * - AI 응답이 실패하면 질문/답변 모두 저장하지 않는다 (오류 문장이 대화 기록에 남지 않도록).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    // 이보다 큰 파일은 자르지 않고(truncate 금지 원칙) 위반 코드 블록만 보내며 수정안은 만들지 않는다.
    private static final int MAX_FULL_FILE_CHARS = 60_000;
    // 위반 위치가 이보다 많으면 한 줄씩 나열하지 않고 SC별로 묶어 요약한다.
    private static final int MAX_LISTED_ISSUES = 100;
    private static final int MAX_SUGGESTION_CHARS = 80;

    private static final String SENDER_USER = "USER";
    private static final String SENDER_AI = "AI";

    private final AiAnalysisClient aiAnalysisClient;
    private final WcagAnalysisService wcagAnalysisService;
    private final ChatMessageRepository chatMessageRepository;
    private final RepositoryRepository repositoryRepository;
    private final GitWorkspaceManager gitWorkspaceManager;
    private final GitWorkingTreeService gitWorkingTreeService;
    private final RealtimeAnalysisService realtimeAnalysisService;
    private final CodePatchSupport codePatchSupport;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private enum FileContext { FULL, SNIPPETS, NONE }

    private record FileSection(FileContext mode, String filePath, String content) {
        static FileSection none() {
            return new FileSection(FileContext.NONE, null, null);
        }
    }

    private record RawFix(String before, String after, String description) {
    }

    private record AiReply(String answer, List<RawFix> fixes) {
    }

    // ===================== 대화 기록 =====================

    @Transactional(readOnly = true)
    public List<MessageItem> getMessages(Long githubId, Long repositoryId) {
        verifyOwner(githubId, repositoryId);
        return chatMessageRepository.findByRepositoryIdOrderByCreatedAtAsc(repositoryId).stream()
                .map(MessageItem::from)
                .toList();
    }

    // ===================== 질문 → 답변 =====================

    public SendResponse send(Long githubId, Long repositoryId, SendRequest request) {
        verifyOwner(githubId, repositoryId);
        String question = request.message().trim();

        List<IssueDetailDto> issues = Optional.ofNullable(
                wcagAnalysisService.getStoredAnalysis(githubId, repositoryId).getIssues()
        ).orElse(List.of());
        IssueDetailDto selected = findSelected(issues, request.selectedLocationId());
        FileSection file = loadFileSection(githubId, request, issues);
        List<ChatMessage> history = recentHistory(repositoryId);

        String raw = aiAnalysisClient.analyze(buildPrompt(issues, selected, file, history, question), List.of());
        AiReply reply = parseReply(raw);
        if (reply == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, toFailureMessage(raw));
        }

        List<FixProposal> fixes = validateFixes(file, reply.fixes());

        // AI 응답이 정상일 때만 질문과 답변을 함께 저장한다.
        ChatMessage userMessage = chatMessageRepository.save(ChatMessage.builder()
                .repositoryId(repositoryId).senderType(SENDER_USER).content(question).build());
        ChatMessage aiMessage = chatMessageRepository.save(ChatMessage.builder()
                .repositoryId(repositoryId).senderType(SENDER_AI).content(reply.answer()).build());

        return new SendResponse(MessageItem.from(userMessage), MessageItem.from(aiMessage),
                fixes, file.mode().name());
    }

    private IssueDetailDto findSelected(List<IssueDetailDto> issues, Long selectedLocationId) {
        if (selectedLocationId == null) return null;
        return issues.stream()
                .filter(i -> selectedLocationId.equals(i.getAnalysisIssueLocationId()))
                .findFirst()
                .orElse(null);
    }

    private FileSection loadFileSection(Long githubId, SendRequest request, List<IssueDetailDto> issues) {
        String path = request.activeFilePath();
        if (path == null || path.isBlank()) return FileSection.none();

        String content;
        try {
            content = gitWorkspaceManager.withWorkspace(
                    githubId, request.repositoryUrl(), request.branchName(),
                    workspace -> codePatchSupport.readFile(workspace.repositoryPath(), path)
            );
        } catch (RuntimeException e) {
            // 파일을 못 읽어도 대화 자체는 가능하므로 파일 맥락 없이 진행한다.
            log.warn("[Chat] 열린 파일 읽기 실패: {} / {}", path, e.getMessage());
            return FileSection.none();
        }

        if (content.length() <= MAX_FULL_FILE_CHARS) {
            return new FileSection(FileContext.FULL, path, content);
        }

        String snippets = issues.stream()
                .filter(i -> path.equals(i.getTargetFilePath()))
                .map(IssueDetailDto::getOriginalCodeBlock)
                .filter(code -> code != null && !code.isBlank())
                .distinct()
                .collect(Collectors.joining("\n...\n"));
        return new FileSection(FileContext.SNIPPETS, path, snippets);
    }

    private List<ChatMessage> recentHistory(Long repositoryId) {
        List<ChatMessage> latest = new ArrayList<>(
                chatMessageRepository.findTop10ByRepositoryIdOrderByCreatedAtDescChatMessageIdDesc(repositoryId));
        Collections.reverse(latest);
        return latest;
    }

    // 응답 형식 지시문은 최상단에 둔다 (프롬프트가 길어져도 지시문이 밀리지 않게).
    private String buildPrompt(List<IssueDetailDto> issues, IssueDetailDto selected, FileSection file,
                               List<ChatMessage> history, String question) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                너는 웹 접근성 QA 도구 CODEE의 AI 어시스턴트다.
                사용자의 GitHub 저장소를 WCAG 2.2 기준으로 분석한 결과를 바탕으로 질문에 답하고, 요청 시 코드 수정안을 제안한다.

                [응답 형식] 아래 JSON 객체만 출력하고, 다른 텍스트는 절대 쓰지 마라.
                {"answer": "사용자에게 보여줄 답변 (마크다운 가능)", "fixes": [{"before": "원본 코드 구간", "after": "수정된 코드 구간", "description": "변경 요약 한 줄"}]}

                [규칙]
                1. answer는 한국어로 간결하게 쓰고, 관련 WCAG 성공 기준(SC 번호)을 근거로 든다.
                2. [분석 결과 요약], [선택한 이슈], [현재 파일]에 없는 사실을 지어내지 않는다. 모르면 모른다고 답한다.
                3. fixes는 사용자가 코드 수정을 요청했고 [현재 파일]이 "전체"로 제공된 경우에만 작성한다. 그 외에는 빈 배열 []로 둔다.
                4. before는 [현재 파일]에 공백·줄바꿈·따옴표까지 글자 하나 다르지 않게 존재하는 연속 구간이며, 파일 안에서 정확히 한 번만 등장해야 한다. 같은 코드가 여러 번 나오면 앞뒤 줄을 포함해 유일하게 만든다.
                5. before는 필요한 최소 범위로 잡고, 서로 다른 수정안의 before 구간이 겹치면 안 된다.
                6. after는 before를 그대로 대체할 코드다. 요청과 무관한 부분은 바꾸지 않고 들여쓰기·코드 스타일·기존 동작을 유지한다.
                7. 수정안을 만들었다면 answer에 무엇을 왜 바꿨는지 요약한다. [현재 파일]이 없거나 일부만 제공되어 수정안을 만들 수 없으면, 그 이유와 함께 수정할 파일을 연 뒤 다시 요청하라고 안내한다.

                """);

        appendIssueSummary(sb, issues);
        appendSelectedIssue(sb, selected);
        appendFileSection(sb, file);
        appendHistory(sb, history);

        sb.append("[사용자 질문]\n").append(question).append('\n');
        return sb.toString();
    }

    private void appendIssueSummary(StringBuilder sb, List<IssueDetailDto> issues) {
        sb.append("[분석 결과 요약]\n");
        if (issues.isEmpty()) {
            sb.append("저장된 분석 결과가 없거나 위반 항목이 없다.\n\n");
            return;
        }

        if (issues.size() <= MAX_LISTED_ISSUES) {
            for (IssueDetailDto issue : issues) {
                sb.append("- SC ").append(issue.getSc()).append(' ').append(issue.getTitle())
                        .append(" | ").append(Objects.toString(issue.getTargetFilePath(), "-"))
                        .append(" | ").append(Objects.toString(issue.getTargetSelector(), "-"))
                        .append(" | ").append(shorten(issue.getSuggestion()))
                        .append('\n');
            }
        } else {
            // 위치가 너무 많으면 SC별 건수와 파일 목록으로 묶는다 (정보를 자르지 않고 요약).
            Map<String, List<IssueDetailDto>> bySc = issues.stream()
                    .collect(Collectors.groupingBy(i -> "SC " + i.getSc() + " " + i.getTitle(),
                            LinkedHashMap::new, Collectors.toList()));
            sb.append("총 ").append(issues.size()).append("건 (SC별 요약)\n");
            bySc.forEach((sc, list) -> sb.append("- ").append(sc).append(": ").append(list.size()).append("건 (")
                    .append(list.stream().map(IssueDetailDto::getTargetFilePath).filter(Objects::nonNull)
                            .distinct().collect(Collectors.joining(", ")))
                    .append(")\n"));
        }
        sb.append('\n');
    }

    private void appendSelectedIssue(StringBuilder sb, IssueDetailDto selected) {
        if (selected == null) return;
        sb.append("[선택한 이슈] (사용자가 화면에서 보고 있는 이슈. \"이거\", \"이 문제\"는 이 이슈를 가리킨다)\n")
                .append("- SC ").append(selected.getSc()).append(' ').append(selected.getTitle()).append('\n')
                .append("- 설명: ").append(Objects.toString(selected.getDescription(), "")).append('\n')
                .append("- 파일: ").append(Objects.toString(selected.getTargetFilePath(), "-"))
                .append(" / 선택자: ").append(Objects.toString(selected.getTargetSelector(), "-")).append('\n')
                .append("- 기존 안내: ").append(Objects.toString(selected.getSuggestion(), "")).append('\n')
                .append("- 위반 코드:\n```\n").append(Objects.toString(selected.getOriginalCodeBlock(), ""))
                .append("\n```\n\n");
    }

    private void appendFileSection(StringBuilder sb, FileSection file) {
        switch (file.mode()) {
            case FULL -> sb.append("[현재 파일: ").append(file.filePath()).append(" (전체)]\n```\n")
                    .append(file.content()).append("\n```\n\n");
            case SNIPPETS -> sb.append("[현재 파일: ").append(file.filePath())
                    .append(" (일부 - 파일이 커서 위반 코드만 제공됨. 이 파일의 수정안은 만들지 마라)]\n```\n")
                    .append(file.content()).append("\n```\n\n");
            case NONE -> sb.append("[현재 파일] 없음 (사용자가 연 파일이 없다. 수정안은 만들지 마라)\n\n");
        }
    }

    private void appendHistory(StringBuilder sb, List<ChatMessage> history) {
        if (history.isEmpty()) return;
        sb.append("[이전 대화]\n");
        for (ChatMessage message : history) {
            sb.append(SENDER_USER.equals(message.getSenderType()) ? "사용자: " : "AI: ")
                    .append(message.getContent()).append('\n');
        }
        sb.append('\n');
    }

    private AiReply parseReply(String raw) {
        if (raw == null || raw.isBlank()) return null;

        String json = raw.trim()
                .replaceFirst("^```(?:json)?\\s*", "")
                .replaceFirst("\\s*```$", "");
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start < 0 || end <= start) {
            log.warn("[Chat] JSON 객체 없음: {}", raw);
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(json.substring(start, end + 1));
            String answer = root.path("answer").asText("").trim();
            if (answer.isEmpty()) return null;

            List<RawFix> fixes = new ArrayList<>();
            for (JsonNode node : root.path("fixes")) {
                fixes.add(new RawFix(textOrNull(node, "before"), textOrNull(node, "after"),
                        textOrNull(node, "description")));
            }
            return new AiReply(answer, fixes);
        } catch (Exception e) {
            log.warn("[Chat] 응답 파싱 실패: {} / 응답 길이={}", e.getMessage(), raw.length());
            return null;
        }
    }

    private List<FixProposal> validateFixes(FileSection file, List<RawFix> rawFixes) {
        // AI가 전체 내용을 본 파일만 수정 대상으로 인정한다 (보지 않은 코드의 before 환각 방지).
        if (file.mode() != FileContext.FULL || rawFixes.isEmpty()) return List.of();

        List<FixProposal> fixes = new ArrayList<>();
        List<int[]> takenRanges = new ArrayList<>();
        for (RawFix fix : rawFixes) {
            String failReason = codePatchSupport.validate(file.content(), fix.before(), fix.after(), takenRanges);
            if (failReason != null) {
                log.info("[Chat] 수정안 제외: {}", failReason);
                continue;
            }
            fixes.add(new FixProposal(file.filePath(), fix.before(), fix.after(),
                    Objects.toString(fix.description(), "")));
        }
        return fixes;
    }

    private String toFailureMessage(String raw) {
        if (raw == null || raw.isBlank()) return "AI 응답이 비어 있습니다. 다시 시도해 주세요.";
        // Bedrock 클라이언트는 실패 시 오류 문장을 반환하므로, JSON이 아니면 그 문장을 사유로 보여준다.
        if (!raw.contains("{")) return "AI 호출에 실패했습니다: " + abbreviate(raw);
        return "AI 응답이 잘렸거나 형식이 올바르지 않습니다. 다시 시도해 주세요.";
    }

    // ===================== 수정안 적용 =====================

    public ApplyResponse applyFixes(Long githubId, Long repositoryId, ApplyRequest request) {
        verifyOwner(githubId, repositoryId);

        return gitWorkspaceManager.withWorkspace(
                githubId, request.repositoryUrl(), request.branchName(),
                workspace -> {
                    Path repoPath = workspace.repositoryPath();
                    String content = codePatchSupport.readFile(repoPath, request.filePath());

                    int applied = 0;
                    List<String> failReasons = new ArrayList<>();
                    for (FixInput fix : request.fixes()) {
                        // 앞선 수정이 반영된 내용 기준으로 다시 검증한다 (정확히 1회 등장할 때만 교체).
                        String failReason = codePatchSupport.validate(content, fix.before(), fix.after());
                        if (failReason != null) {
                            failReasons.add(failReason);
                            continue;
                        }
                        content = content.replace(fix.before(), fix.after());
                        applied++;
                    }

                    if (applied > 0) {
                        gitWorkingTreeService.writeFile(repoPath, new GitFileWriteRequest(
                                request.repositoryUrl(), request.branchName(), request.filePath(), content));
                    }

                    // 적용 결과를 정적 룰로 즉시 재검사 (AI 호출 없음)
                    RealtimeAnalysisResponse remaining = realtimeAnalysisService.analyzeCode(content, request.filePath());
                    return new ApplyResponse(request.filePath(), applied, failReasons, remaining, content);
                }
        );
    }

    // ===================== 공통 =====================

    private void verifyOwner(Long githubId, Long repositoryId) {
        if (githubId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        repositoryRepository.findByRepositoryIdAndGithubId(repositoryId, githubId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "저장소를 찾을 수 없습니다."));
    }

    private String textOrNull(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asText() : null;
    }

    private String shorten(String text) {
        if (text == null) return "";
        String oneLine = text.replaceAll("\\s+", " ").trim();
        return oneLine.length() > MAX_SUGGESTION_CHARS ? oneLine.substring(0, MAX_SUGGESTION_CHARS) + "…" : oneLine;
    }

    private String abbreviate(String text) {
        String oneLine = text.replaceAll("\\s+", " ").trim();
        return oneLine.length() > 120 ? oneLine.substring(0, 120) + "…" : oneLine;
    }
}