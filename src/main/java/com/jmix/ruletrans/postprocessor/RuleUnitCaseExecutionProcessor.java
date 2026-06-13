package com.jmix.ruletrans.postprocessor;

import com.jmix.executor.ModuleConstraintExecutor;
import com.jmix.executor.bmodel.Module;
import com.jmix.executor.model.ConstraintConfig;
import com.jmix.executor.model.Result;
import com.jmix.ruletrans.RuleTransException;
import com.jmix.ruletrans.RuleTransFailureKind;
import com.jmix.ruletrans.assembler.AssembledRuleClass;
import com.jmix.ruletrans.assembler.RuleTransTempFileManager;
import com.jmix.ruletrans.testgen.business.BusinessRuleTestCaseSet;

import lombok.extern.slf4j.Slf4j;
import com.jmix.ruleunit.DefaultRuleUnitTestExecutorService;
import com.jmix.ruleunit.RuleUnitTestCaseSetReport;
import com.jmix.ruleunit.RuleUnitTestReport;
import com.jmix.tool.bbuilder.ModuleGenneratorByAnno;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
 * Executes business-readable RuleUnit cases without generating or running JUnit tests.
 */
@Slf4j
public final class RuleUnitCaseExecutionProcessor {

    private final RuleTransTempFileManager tempFileManager;

    public RuleUnitCaseExecutionProcessor() {
        this(new RuleTransTempFileManager());
    }

    public RuleUnitCaseExecutionProcessor(RuleTransTempFileManager tempFileManager) {
        this.tempFileManager = tempFileManager == null ? new RuleTransTempFileManager() : tempFileManager;
    }

    public RuleUnitExecutionResult execute(
            AssembledRuleClass assembledRuleClass,
            BusinessRuleTestCaseSet caseSet) {
        if (assembledRuleClass == null) {
            throw new IllegalArgumentException("assembledRuleClass must not be null");
        }
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader loader = generatedClassLoader(originalClassLoader)) {
            Thread.currentThread().setContextClassLoader(loader);
            Class<?> ruleClass = Class.forName(assembledRuleClass.qualifiedClassName(), true, loader);
            return execute(ruleClass, caseSet);
        } catch (Exception e) {
            log.error("RuleUnit case execution failed for class=[{}]", assembledRuleClass.qualifiedClassName(), e);
            return failed(RuleTransFailureKind.RULE_UNIT_INFRA_FAILED, messageOf(e), false);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    public RuleUnitExecutionResult execute(
            Class<?> generatedRuleClass,
            BusinessRuleTestCaseSet caseSet) {
        if (generatedRuleClass == null) {
            throw new IllegalArgumentException("generatedRuleClass must not be null");
        }
        if (caseSet == null || caseSet.isEmpty()) {
            return failed(RuleTransFailureKind.BUSINESS_CASE_GENERATION_FAILED,
                    "business case set must not be empty", false);
        }

        try {
            Module module = ModuleGenneratorByAnno.buildModule(generatedRuleClass);
            RuleUnitExecutionResult initFailure = initModule(module);
            if (initFailure != null) {
                return initFailure;
            }
            DefaultRuleUnitTestExecutorService service = new DefaultRuleUnitTestExecutorService(module);
            RuleUnitTestCaseSetReport report = service.executeCaseSet(caseSet);
            if (report.passed()) {
                return new RuleUnitExecutionResult(true, report, RuleTransFailureKind.NONE, List.of(), List.of());
            }
            return new RuleUnitExecutionResult(false, report, RuleTransFailureKind.RULE_LOGIC_FAILED,
                    failuresFrom(report), failureMessages(report));
        } catch (RuleTransException e) {
            RuleTransFailureKind kind = isBusinessCaseSchemaError(e)
                    ? RuleTransFailureKind.BUSINESS_CASE_SCHEMA_INVALID
                    : RuleTransFailureKind.RULE_UNIT_INFRA_FAILED;
            log.error("RuleUnit case execution failed: {}", e.getMessage(), e);
            return failed(kind, messageOf(e), kind == RuleTransFailureKind.RULE_LOGIC_FAILED);
        } catch (Exception e) {
            log.error("RuleUnit case execution failed for class=[{}]", generatedRuleClass.getName(), e);
            return failed(RuleTransFailureKind.RULE_UNIT_INFRA_FAILED, messageOf(e), false);
        } finally {
            ModuleConstraintExecutor.INST.fini();
        }
    }

    private RuleUnitExecutionResult initModule(Module module) {
        Result<Void> initResult = ModuleConstraintExecutor.INST.init(ruleTransConstraintConfig());
        if (initResult.getCode() != Result.SUCCESS) {
            return failed(RuleTransFailureKind.RULE_UNIT_INFRA_FAILED,
                    "RuleUnit init failed: " + initResult.getMessage(), false);
        }
        Result<Void> addResult = ModuleConstraintExecutor.INST.addModule(module.getId(), module);
        if (addResult.getCode() != Result.SUCCESS) {
            return failed(RuleTransFailureKind.RULE_UNIT_INFRA_FAILED,
                    "RuleUnit add module failed: " + addResult.getMessage(), false);
        }
        return null;
    }

    private URLClassLoader generatedClassLoader(ClassLoader parent) throws Exception {
        URL classesRoot = tempFileManager.classesRoot().toUri().toURL();
        return new URLClassLoader(new URL[] {classesRoot}, parent);
    }

    private ConstraintConfig ruleTransConstraintConfig() {
        ConstraintConfig config = new ConstraintConfig();
        config.setAttachedDebug(true);
        config.setLoadType(ConstraintConfig.LOAD_TYPE_FULL);
        return config;
    }

    private RuleUnitExecutionResult failed(
            RuleTransFailureKind kind,
            String message,
            boolean likelyRuleLogicError) {
        RuleTransVerificationFailure failure = new RuleTransVerificationFailure(
                kind.name(),
                kind.name(),
                "",
                "",
                "",
                message,
                likelyRuleLogicError);
        return new RuleUnitExecutionResult(false, null, kind, List.of(failure), List.of(message));
    }

    private List<RuleTransVerificationFailure> failuresFrom(RuleUnitTestCaseSetReport report) {
        return report.caseReports().stream()
                .filter(caseReport -> !caseReport.passed())
                .map(this::failureFrom)
                .toList();
    }

    private RuleTransVerificationFailure failureFrom(RuleUnitTestReport report) {
        return new RuleTransVerificationFailure(
                report.caseId(),
                report.caseId(),
                "",
                "",
                report.actual() == null ? "" : report.actual().toString(),
                String.join("\n", report.failures()),
                true);
    }

    private List<String> failureMessages(RuleUnitTestCaseSetReport report) {
        return report.caseReports().stream()
                .filter(caseReport -> !caseReport.passed())
                .flatMap(caseReport -> caseReport.failures().stream())
                .toList();
    }

    private boolean isBusinessCaseSchemaError(RuleTransException e) {
        String message = messageOf(e);
        return message.contains("Unsupported serviceMethod")
                || message.contains("serviceMethod must not be blank")
                || message.contains("Unsupported aggregate type");
    }

    private String messageOf(Exception e) {
        return e.getMessage() == null ? e.getClass().getName() : e.getMessage();
    }
}
