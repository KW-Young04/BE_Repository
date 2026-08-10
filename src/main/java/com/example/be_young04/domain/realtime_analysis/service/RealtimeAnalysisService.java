package com.example.be_young04.domain.realtime_analysis.service;

import com.example.be_young04.domain.realtime_analysis.dto.IssueDetailDto;
import com.example.be_young04.domain.realtime_analysis.dto.RealtimeAnalysisResponse;
import com.example.be_young04.domain.wcag.repository.WcagItemRepository;
import com.example.be_young04.domain.wcag.entity.WcagItem;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RealtimeAnalysisService {

    private final WcagItemRepository wcagItemRepository;

    // 1. DB 조회를 최소화하기 위해 Spring Cache 적용 (최초 1회 조회 후 메모리 유지)
    @Cacheable(value = "wcagRules")
    public Map<String, WcagItem> getCachedWcagRules() {
        return wcagItemRepository.findAll().stream()
                .collect(Collectors.toMap(
                        WcagItem::getSc,                 // Key: SC 번호 (예: "1.1.1")
                        Function.identity(),             // Value: WcagItem 객체
                        (existing, replacement) -> existing // 💡 중복 키 발생 시 기존 값 유지 (에러 방지!)
                ));
    }

    // 2. 실시간 코드 정적 분석 (DB INSERT 없이 메모리 상에서만 실행)
    public RealtimeAnalysisResponse analyzeCode(String code, String filePath) {
        Map<String, WcagItem> rules = getCachedWcagRules();
        List<IssueDetailDto> issues = new ArrayList<>();

        if (code == null || code.isBlank()) {
            return buildResponse(issues);
        }

        Document doc = Jsoup.parse(code);

        // [검사 1] WCAG 1.1.1 (Non-text Content): img 태그의 alt 속성 유무
        WcagItem rule111 = rules.get("1.1.1");
        if (rule111 != null) {
            Elements images = doc.select("img");
            for (Element img : images) {
                if (!img.hasAttr("alt") || img.attr("alt").trim().isEmpty()) {
                    issues.add(IssueDetailDto.builder()
                            .wcagItemId(rule111.getWcagItemId())
                            .sc(rule111.getSc())
                            .title(rule111.getTitle())
                            .levelType(rule111.getLevelType())
                            .description(rule111.getDescription())
                            .status("FAIL")
                            .targetFilePath(filePath != null ? filePath : "index.html")
                            .targetSelector("img[src=\"" + img.attr("src") + "\"]")
                            .originalCodeBlock(img.outerHtml())
                            .suggestion("img 태그에 적절한 대체 텍스트(alt 속성)를 추가하세요.")
                            .measuredValue("alt 속성 누락")
                            .thresholdValue("alt 속성 필수 존재")
                            .build());
                }
            }
        }

        // [검사 2] WCAG 4.1.2 (Name, Role, Value): button 또는 a 태그의 Accessible Name 유무
        WcagItem rule412 = rules.get("4.1.2");
        if (rule412 != null) {
            Elements buttonsAndLinks = doc.select("button, a");
            for (Element el : buttonsAndLinks) {
                boolean hasText = !el.text().trim().isEmpty();
                boolean hasAriaLabel = el.hasAttr("aria-label") && !el.attr("aria-label").trim().isEmpty();

                if (!hasText && !hasAriaLabel) {
                    issues.add(IssueDetailDto.builder()
                            .wcagItemId(rule412.getWcagItemId())
                            .sc(rule412.getSc())
                            .title(rule412.getTitle())
                            .levelType(rule412.getLevelType())
                            .description(rule412.getDescription())
                            .status("FAIL")
                            .targetFilePath(filePath != null ? filePath : "index.html")
                            .targetSelector(el.tagName())
                            .originalCodeBlock(el.outerHtml())
                            .suggestion("버튼 또는 링크 내부에 텍스트를 넣거나 aria-label 속성을 지정하세요.")
                            .measuredValue("Accessible Name 없음")
                            .thresholdValue("텍스트 또는 aria-label 필수")
                            .build());
                }
            }
        }

        return buildResponse(issues);
    }

    private RealtimeAnalysisResponse buildResponse(List<IssueDetailDto> issues) {
        return RealtimeAnalysisResponse.builder()
                .success(true)
                .timestamp(LocalDateTime.now())
                .issueCount(issues.size())
                .issues(issues)
                .build();
    }
}
