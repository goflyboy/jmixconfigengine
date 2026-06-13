package com.jmix.ruletrans;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jmix.ruletrans.postprocessor.CompilationResult;
import com.jmix.ruletrans.testgen.business.BusinessRuleTestCase;
import com.jmix.ruletrans.testgen.business.BusinessRuleTestCaseSet;
import com.jmix.ruleunit.RuleUnitTestCaseSetReport;
import com.jmix.ruleunit.RuleUnitTestReport;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Prints a compact system-test diagnostic report for RuleTrans pipeline runs.
 */
public final class RuleTransPipelineResultPrinter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private RuleTransPipelineResultPrinter() {
    }

    public static void print(RuleTransPipelineRunResult result, PrintStream out) {
        RuleTransPipelineResult pipelineResult = result.pipelineResult();
        PrintStream utf8Out = new PrintStream(out, true, StandardCharsets.UTF_8);
        utf8Out.println("========== RuleTransPipeline ==========");
        printInput(result, utf8Out);
        printLlmCalls(result.diagnostics().llmCalls(), utf8Out);
        printAttempts(pipelineResult, utf8Out);
        printBusinessTestCases(pipelineResult.businessCaseSet(), pipelineResult.ruleUnitReport(), utf8Out);
        printSummary(pipelineResult, utf8Out);
        utf8Out.println();
        utf8Out.println("[Final Method Body]");
        utf8Out.println(value(pipelineResult.methodBody()));
        utf8Out.println("=======================================");
    }

    private static void printInput(RuleTransPipelineRunResult result, PrintStream out) {
        out.println("[Input]");
        out.println("Natural language: " + result.naturalLanguage());
        out.println("Context: " + result.contextSummary());
        out.println("Diagnostics dir: " + result.diagnostics().outputDir());
        out.println();
    }

    private static void printLlmCalls(List<RuleTransLlmCallDiagnostic> calls, PrintStream out) {
        out.println("[LLM Calls]");
        if (calls.isEmpty()) {
            out.println("(none)");
            out.println();
            return;
        }
        for (RuleTransLlmCallDiagnostic call : calls) {
            out.println("#" + call.index()
                    + " stage=" + call.stage()
                    + " duration=" + call.durationMillis() + "ms"
                    + " success=" + call.success());
            if (!call.errorMessage().isBlank()) {
                out.println("error: " + call.errorMessage());
            }
            out.println("Prompt summary:");
            out.println(indent(call.promptSummary()));
            out.println("Full prompt: " + call.fullPromptFile());
            out.println("Response summary:");
            out.println(indent(call.responseSummary()));
            out.println("Full response: " + call.fullResponseFile());
        }
        out.println();
    }

    private static void printAttempts(RuleTransPipelineResult result, PrintStream out) {
        out.println("[Attempts]");
        if (result.attemptStates().isEmpty()) {
            printAttempt(result.attempts(), result.methodBody(), result.compilationResult(),
                    result.businessCaseSet(), result.ruleUnitReport(), result.failureKind(), out);
            out.println();
            return;
        }
        for (RuleTransAttemptState state : result.attemptStates()) {
            printAttempt(state.attempt(), state.methodBody(), state.compilationResult(),
                    state.businessCaseSet(), state.ruleUnitReport(), state.failureKind(), out);
        }
        Set<Integer> printedAttempts = result.attemptStates().stream()
                .map(RuleTransAttemptState::attempt)
                .collect(Collectors.toSet());
        if (result.success() && !printedAttempts.contains(result.attempts())) {
            printAttempt(result.attempts(), result.methodBody(), result.compilationResult(),
                    result.businessCaseSet(), result.ruleUnitReport(), RuleTransFailureKind.NONE, out);
        }
        out.println();
    }

    private static void printAttempt(
            int attempt,
            String methodBody,
            CompilationResult compilationResult,
            BusinessRuleTestCaseSet businessCaseSet,
            RuleUnitTestCaseSetReport report,
            RuleTransFailureKind failureKind,
            PrintStream out) {
        out.println("#" + attempt + " failureKind=" + failureKind);
        out.println("methodBody:");
        out.println(indent(value(methodBody)));
        printCompilation(compilationResult, out);
        printBusinessCaseSummary(businessCaseSet, out);
        printRuleUnit(report, out);
    }

    private static void printCompilation(CompilationResult result, PrintStream out) {
        if (result == null) {
            out.println("compile: skipped");
            return;
        }
        out.println("compile: success=" + result.success()
                + " errors=" + result.errors().size()
                + " source=" + path(result.sourceFile()));
        if (!result.errors().isEmpty()) {
            out.println("compile errors:");
            out.println(indent(String.join("\n", result.errors())));
        }
    }

    private static void printBusinessCaseSummary(BusinessRuleTestCaseSet caseSet, PrintStream out) {
        if (caseSet == null) {
            out.println("businessCases: skipped");
            return;
        }
        List<String> ids = caseSet.cases().stream()
                .map(testCase -> value(testCase.id()))
                .toList();
        out.println("businessCases: count=" + caseSet.cases().size() + " ids=" + ids);
    }

    private static void printBusinessTestCases(
            BusinessRuleTestCaseSet caseSet,
            RuleUnitTestCaseSetReport report,
            PrintStream out) {
        out.println("[Business Test Cases]");
        if (caseSet == null) {
            out.println("skipped");
            out.println();
            return;
        }
        if (caseSet.cases().isEmpty()) {
            out.println("(none)");
            out.println();
            return;
        }
        Map<String, RuleUnitTestReport> reportById = reportByCaseId(report);
        for (int i = 0; i < caseSet.cases().size(); i++) {
            BusinessRuleTestCase testCase = caseSet.cases().get(i);
            out.println("#" + (i + 1)
                    + " id=" + value(testCase.id())
                    + " family=" + value(testCase.businessFamily())
                    + " serviceMethod=" + value(testCase.serviceMethod()));
            if (!value(testCase.title()).isBlank()) {
                out.println("title=" + testCase.title());
            }
            if (!value(testCase.scenario()).isBlank()) {
                out.println("scenario=" + testCase.scenario());
            }
            out.println("given:");
            out.println(indent(json(testCase.given())));
            out.println("expect:");
            out.println(indent(json(testCase.expect())));
            RuleUnitTestReport caseReport = reportById.get(testCase.id());
            printCaseActual(caseReport, out);
        }
        out.println();
    }

    private static Map<String, RuleUnitTestReport> reportByCaseId(RuleUnitTestCaseSetReport report) {
        if (report == null) {
            return Map.of();
        }
        return report.caseReports().stream()
                .collect(Collectors.toMap(
                        RuleUnitTestReport::caseId,
                        Function.identity(),
                        (first, second) -> first));
    }

    private static void printCaseActual(RuleUnitTestReport caseReport, PrintStream out) {
        if (caseReport == null) {
            out.println("actual: skipped");
            return;
        }
        out.println("actual:");
        out.println(indent(json(caseReport.actual())));
        out.println("passed=" + caseReport.passed());
        if (!caseReport.failures().isEmpty()) {
            out.println("failures:");
            out.println(indent(String.join("\n", caseReport.failures())));
        }
    }

    private static void printRuleUnit(RuleUnitTestCaseSetReport report, PrintStream out) {
        if (report == null) {
            out.println("ruleUnit: skipped");
            return;
        }
        long passed = report.caseReports().stream().filter(RuleUnitTestReport::passed).count();
        long failed = report.caseReports().size() - passed;
        out.println("ruleUnit: passed=" + report.passed()
                + " total=" + report.caseReports().size()
                + " passedCount=" + passed
                + " failed=" + failed);
        report.caseReports().stream()
                .filter(caseReport -> !caseReport.passed())
                .forEach(caseReport -> out.println("  failedCase=" + caseReport.caseId()
                        + " failures=" + caseReport.failures()));
    }

    private static void printSummary(RuleTransPipelineResult result, PrintStream out) {
        out.println("[Summary]");
        out.println("success=" + result.success());
        out.println("attempts=" + result.attempts());
        out.println("failureKind=" + result.failureKind());
        out.println("messages=" + result.messages());
    }

    private static String indent(String value) {
        String safe = value(value);
        if (safe.isBlank()) {
            return "  <empty>";
        }
        return safe.lines()
                .map(line -> "  " + line)
                .collect(Collectors.joining("\n"));
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }

    private static String value(Object value) {
        return value == null ? "" : value.toString();
    }

    private static String path(Path path) {
        return path == null ? "" : path.toString();
    }

    private static String json(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }
}
