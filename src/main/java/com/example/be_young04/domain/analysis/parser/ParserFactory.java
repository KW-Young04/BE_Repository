package com.example.be_young04.domain.analysis.parser;

public class ParserFactory {

    public static CodeParser getParser(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("파일 이름이 비어 있습니다.");
        }

        String lowerCaseFileName = fileName.toLowerCase();

        if (lowerCaseFileName.endsWith(".java")) {
            return new JavaCodeParser();
        }

        if (lowerCaseFileName.endsWith(".js")
                || lowerCaseFileName.endsWith(".jsx")
                || lowerCaseFileName.endsWith(".ts")
                || lowerCaseFileName.endsWith(".tsx")
                || lowerCaseFileName.endsWith(".html")
                || lowerCaseFileName.endsWith(".htm")) {
            return new JsCodeParser();
        }

        throw new IllegalArgumentException("지원하지 않는 파일 형식입니다: " + fileName);
    }
}
