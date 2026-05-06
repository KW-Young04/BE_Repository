package com.example.be_young04.domain.analysis.parser;

import com.example.be_young04.domain.analysis.accessibility.AccessibilityAnalyzer;
import com.example.be_young04.domain.analysis.dto.AccessibilityCheckResult;
import com.example.be_young04.domain.analysis.dto.CodeAnalysisResult;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsCodeParser implements CodeParser {

    private final AccessibilityAnalyzer accessibilityAnalyzer = new AccessibilityAnalyzer();

    @Override
    public CodeAnalysisResult parse(String code) {
        List<String> imports = extractImports(code);
        List<String> methods = extractFunctions(code);
        List<String> components = extractComponents(code);
        List<String> jsxElements = extractJsxElements(code);
        List<AccessibilityCheckResult> accessibilityChecks = accessibilityAnalyzer.analyze(code);
        List<String> accessibilityIssues = accessibilityAnalyzer.analyzeIssueMessages(code);
        int lineCount = code == null || code.isBlank() ? 0 : code.split("\n").length;

        return CodeAnalysisResult.builder()
                .classes(List.of())
                .methods(methods)
                .imports(imports)
                .components(components)
                .jsxElements(jsxElements)
                .accessibilityIssues(accessibilityIssues)
                .accessibilityChecks(accessibilityChecks)
                .lineCount(lineCount)
                .build();
    }

    private List<String> extractImports(String code) {
        Set<String> result = new LinkedHashSet<>();

        Pattern fromImportPattern = Pattern.compile("import\\s+.*?from\\s+[\"']([^\"']+)[\"']");
        Matcher fromImportMatcher = fromImportPattern.matcher(code);
        while (fromImportMatcher.find()) {
            result.add(fromImportMatcher.group(1));
        }

        Pattern sideEffectImportPattern = Pattern.compile("import\\s+[\"']([^\"']+)[\"']");
        Matcher sideEffectImportMatcher = sideEffectImportPattern.matcher(code);
        while (sideEffectImportMatcher.find()) {
            result.add(sideEffectImportMatcher.group(1));
        }

        return new ArrayList<>(result);
    }

    private List<String> extractFunctions(String code) {
        Set<String> result = new LinkedHashSet<>();

        // function hello() {}
        Pattern functionPattern = Pattern.compile("function\\s+(\\w+)\\s*\\(");
        Matcher functionMatcher = functionPattern.matcher(code);
        while (functionMatcher.find()) {
            result.add(functionMatcher.group(1));
        }

        // const hello = () => {}
        Pattern arrowPattern = Pattern.compile("(const|let|var)\\s+(\\w+)\\s*=\\s*(\\([^)]*\\)|\\w+)\\s*=>");
        Matcher arrowMatcher = arrowPattern.matcher(code);
        while (arrowMatcher.find()) {
            result.add(arrowMatcher.group(2));
        }

        // const hello = function() {}
        Pattern functionExprPattern = Pattern.compile("(const|let|var)\\s+(\\w+)\\s*=\\s*function\\s*\\(");
        Matcher functionExprMatcher = functionExprPattern.matcher(code);
        while (functionExprMatcher.find()) {
            result.add(functionExprMatcher.group(2));
        }

        return new ArrayList<>(result);
    }

    private List<String> extractComponents(String code) {
        Set<String> result = new LinkedHashSet<>();

        // function App() { return <div/> }
        Pattern functionComponentPattern = Pattern.compile("function\\s+([A-Z][A-Za-z0-9_]*)\\s*\\(");
        Matcher functionComponentMatcher = functionComponentPattern.matcher(code);
        while (functionComponentMatcher.find()) {
            result.add(functionComponentMatcher.group(1));
        }

        // const App = () => ...
        Pattern arrowComponentPattern = Pattern.compile("(const|let|var)\\s+([A-Z][A-Za-z0-9_]*)\\s*=\\s*(\\([^)]*\\)|\\w+)\\s*=>");
        Matcher arrowComponentMatcher = arrowComponentPattern.matcher(code);
        while (arrowComponentMatcher.find()) {
            result.add(arrowComponentMatcher.group(2));
        }

        // const App = function() ...
        Pattern functionExprComponentPattern = Pattern.compile("(const|let|var)\\s+([A-Z][A-Za-z0-9_]*)\\s*=\\s*function\\s*\\(");
        Matcher functionExprComponentMatcher = functionExprComponentPattern.matcher(code);
        while (functionExprComponentMatcher.find()) {
            result.add(functionExprComponentMatcher.group(2));
        }

        // export default App;
        Pattern exportDefaultPattern = Pattern.compile("export\\s+default\\s+([A-Z][A-Za-z0-9_]*)");
        Matcher exportDefaultMatcher = exportDefaultPattern.matcher(code);
        while (exportDefaultMatcher.find()) {
            result.add(exportDefaultMatcher.group(1));
        }

        return new ArrayList<>(result);
    }

    private List<String> extractJsxElements(String code) {
        Set<String> result = new LinkedHashSet<>();

        // <div>, <App>, <Header />, </div> 전부 대응
        Pattern jsxPattern = Pattern.compile("<\\s*([A-Za-z][A-Za-z0-9_]*)\\b");
        Matcher jsxMatcher = jsxPattern.matcher(code);
        while (jsxMatcher.find()) {
            String tag = jsxMatcher.group(1);

            // Fragment shorthand <> 는 애초에 안 잡힘
            // 소문자 태그도, 컴포넌트명도 둘 다 수집
            result.add(tag);
        }

        return new ArrayList<>(result);
    }
}
