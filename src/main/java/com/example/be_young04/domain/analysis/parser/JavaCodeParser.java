package com.example.be_young04.domain.analysis.parser;

import com.example.be_young04.domain.analysis.dto.CodeAnalysisResult;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaCodeParser implements CodeParser {

    @Override
    public CodeAnalysisResult parse(String code) {
        List<String> classes = extractClasses(code);
        List<String> methods = extractMethods(code);
        List<String> imports = extractImports(code);
        int lineCount = code == null || code.isBlank() ? 0 : code.split("\n").length;

        return CodeAnalysisResult.builder()
                .classes(classes)
                .methods(methods)
                .imports(imports)
                .components(List.of())
                .jsxElements(List.of())
                .accessibilityIssues(List.of())
                .accessibilityChecks(List.of())
                .lineCount(lineCount)
                .build();
    }

    private List<String> extractClasses(String code) {
        List<String> result = new ArrayList<>();
        Pattern pattern = Pattern.compile("class\\s+(\\w+)");
        Matcher matcher = pattern.matcher(code);

        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }

    private List<String> extractMethods(String code) {
        List<String> result = new ArrayList<>();
        Pattern pattern = Pattern.compile("(public|private|protected)?\\s+\\w+\\s+(\\w+)\\s*\\(");
        Matcher matcher = pattern.matcher(code);

        while (matcher.find()) {
            result.add(matcher.group(2));
        }
        return result;
    }

    private List<String> extractImports(String code) {
        List<String> result = new ArrayList<>();
        Pattern pattern = Pattern.compile("import\\s+([\\w\\.\\*]+);");
        Matcher matcher = pattern.matcher(code);

        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }
}
