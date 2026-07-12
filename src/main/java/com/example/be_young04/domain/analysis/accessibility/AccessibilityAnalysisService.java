package com.example.be_young04.domain.analysis.accessibility;

import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.rule.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccessibilityAnalysisService {

    private final AccessibilityAnalyzer accessibilityAnalyzer = new AccessibilityAnalyzer();
    private final AccessibilityAiReviewService accessibilityAiReviewService;

    // 1.1.1 이미지 대체 텍스트 - 시간 기반 미디어
    public AccessibilityCheckResult analyzeSc111(String code) {
        return new Sc111NonTextContentRule().analyze(code);
    }

    // 1.1.1 이미지 대체 텍스트 - 순수 장식
    public AccessibilityCheckResult analyzeSc111Decorative(String code) {
        return new Sc111DecorativeContentRule().analyze(code);
    }

    // 1.1.1 이미지 대체 텍스트 - 컨트롤/입력
    public AccessibilityCheckResult analyzeSc111ControlInput(String code) {
        AccessibilityCheckResult staticResult = new Sc111ControlInputNameRule().analyze(code);
        return accessibilityAiReviewService.reviewControlInputName(code, staticResult);
    }

    // 1.2.1 오디오 전용 (사전녹음)
    public AccessibilityCheckResult analyzeSc121Audio(String code) {
        return new Sc121AudioOnlyPrerecordedRule().analyze(code);
    }

    // 1.2.1 비디오 전용 (사전녹음)
    public AccessibilityCheckResult analyzeSc121VideoOnly(String code) {
        return new Sc121VideoOnlyPrerecordedRule().analyze(code);
    }

    // 1.2.2 자막 제공 (사전녹음)
    public AccessibilityCheckResult analyzeSc122(String code) {
        return new Sc122CaptionsPrerecordedRule().analyze(code);
    }

    // 1.2.3 음성 해설 또는 미디어 대안 (사전녹음)
    public AccessibilityCheckResult analyzeSc123(String code) {
        return new Sc123AudioDescriptionRule().analyze(code);
    }

    // 1.3.1 정보와 관계 (구조적 마크업)
    public AccessibilityCheckResult analyzeSc131(String code) {
        return new Sc131InformationRelationshipRule().analyze(code);
    }

    // 1.4.1 색상 사용 - 색상 인식 전용
    public AccessibilityCheckResult analyzeSc141(String code) {
        return new Sc141ColorOnlyRule().analyze(code);
    }

    // 1.4.2 오디오 제어 - 비간섭 요건
    public AccessibilityCheckResult analyzeSc142(String code) {
        return new Sc142AudioControlRule().analyze(code);
    }

    // 2.1.2 키보드 트랩 없음
    public AccessibilityCheckResult analyzeSc212(String code) {
        return new Sc212KeyboardTrapRule().analyze(code);
    }

    // 2.2.1 타이밍 조정
    public AccessibilityCheckResult analyzeSc221(String code) {
        return new Sc221TimingAdjustableRule().analyze(code);
    }

    // 2.2.2 일시정지/정지/숨기기 - 자동 업데이트
    public AccessibilityCheckResult analyzeSc222(String code) {
        return new Sc222AutoUpdateControlRule().analyze(code);
    }

    // 2.3.1 세 번 이하 점멸
    public AccessibilityCheckResult analyzeSc231(String code) {
        return new Sc231BlinkingRule().analyze(code);
    }

    // 2.4.1 블록 건너뛰기
    public AccessibilityCheckResult analyzeSc241(String code) {
        return new Sc241SkipBlockRule().analyze(code);
    }

    // 2.4.2 페이지 제목
    public AccessibilityCheckResult analyzeSc242(String code) {
        return new Sc242PageTitleRule().analyze(code);
    }

    // 2.4.3 포커스 순서
    public AccessibilityCheckResult analyzeSc243(String code) {
        return new Sc243FocusOrderRule().analyze(code);
    }

    // 3.1.1 페이지 언어
    public AccessibilityCheckResult analyzeSc311(String code) {
        return new Sc311PageLanguageRule().analyze(code);
    }

    // 3.2.1 포커스 시 변경 없음
    public AccessibilityCheckResult analyzeSc321(String code) {
        return new Sc321FocusChangeRule().analyze(code);
    }

    // 3.2.2 입력 시 변경 없음
    public AccessibilityCheckResult analyzeSc322(String code) {
        return new Sc322InputChangeRule().analyze(code);
    }

    // 3.3.1 오류 식별
    public AccessibilityCheckResult analyzeSc331(String code) {
        return new Sc331ErrorIdentificationRule().analyze(code);
    }

    // 3.3.2 레이블 또는 설명
    public AccessibilityCheckResult analyzeSc332(String code) {
        return new Sc332LabelDescriptionRule().analyze(code);
    }

    // 4.1.2 이름·역할·값
    public AccessibilityCheckResult analyzeSc412(String code) {
        return new Sc412NameRoleValueRule().analyze(code);
    }
}
