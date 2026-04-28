package com.example.be_young04.domain.snapshot.service;

import com.example.be_young04.domain.snapshot.dto.SnapshotResponse;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class SnapshotService {

    public SnapshotResponse capture(String deploymentUrl) {
        validateUrl(deploymentUrl);

        try {
            Path outputDir = Path.of("snapshots");
            Files.createDirectories(outputDir);

            String fileName = "snapshot-" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) +
                    ".png";

            Path outputPath = outputDir.resolve(fileName);

            try (Playwright playwright = Playwright.create()) {
                Browser browser = playwright.chromium().launch(
                        new BrowserType.LaunchOptions().setHeadless(true)
                );

                BrowserContext context = browser.newContext(
                        new Browser.NewContextOptions()
                                .setViewportSize(1440, 900)
                );

                Page page = context.newPage();

                page.navigate(
                        deploymentUrl,
                        new Page.NavigateOptions()
                                .setWaitUntil(WaitUntilState.NETWORKIDLE)
                                .setTimeout(30000)
                );

                page.screenshot(
                        new Page.ScreenshotOptions()
                                .setPath(outputPath)
                                .setFullPage(true)
                );

                browser.close();
            }

            return SnapshotResponse.builder()
                    .deploymentUrl(deploymentUrl)
                    .imagePath(outputPath.toString())
                    .width(1440)
                    .height(900)
                    .build();

        } catch (Exception e) {
            throw new IllegalStateException("렌더링 스냅샷 생성에 실패했습니다.", e);
        }
    }

    private void validateUrl(String deploymentUrl) {
        if (deploymentUrl == null || deploymentUrl.isBlank()) {
            throw new IllegalArgumentException("배포 URL이 비어 있습니다.");
        }

        if (!deploymentUrl.startsWith("http://") && !deploymentUrl.startsWith("https://")) {
            throw new IllegalArgumentException("배포 URL은 http:// 또는 https://로 시작해야 합니다.");
        }
    }
}