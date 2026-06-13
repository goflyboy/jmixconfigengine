package com.jmix.ruleunit;

import com.jmix.executor.bmodel.Module;
import com.jmix.ruletrans.RuleTransException;
import com.jmix.ruletrans.testgen.business.BusinessRuleTestCase;
import com.jmix.ruletrans.testgen.business.BusinessRuleTestCaseSet;
import com.jmix.ruletrans.testgen.business.RuleUnitServiceMethod;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Default service for executing business-readable rule unit cases.
 */
public final class DefaultRuleUnitTestExecutorService implements RuleUnitTestExecutorService {

    private final RuleUnitModuleRegistry moduleRegistry;
    private final ModuleConstraintExecutor4SingleRule singleRuleExecutor;
    private final RuleUnitInputConverter inputConverter = new RuleUnitInputConverter();
    private final RuleUnitResultComparator comparator = new RuleUnitResultComparator();

    public DefaultRuleUnitTestExecutorService(Module module) {
        this(new StaticRuleUnitModuleRegistry(module), new DefaultModuleConstraintExecutor4SingleRule());
    }

    public DefaultRuleUnitTestExecutorService(
            RuleUnitModuleRegistry moduleRegistry,
            ModuleConstraintExecutor4SingleRule singleRuleExecutor) {
        this.moduleRegistry = moduleRegistry;
        this.singleRuleExecutor = singleRuleExecutor == null
                ? new DefaultModuleConstraintExecutor4SingleRule()
                : singleRuleExecutor;
    }

    @Override
    public RuleUnitTestReport executeCase(BusinessRuleTestCase testCase) {
        RuleUnitServiceMethod method = RuleUnitServiceMethod.from(testCase.serviceMethod());
        return switch (method) {
            case testAssignment -> testAssignment(testCase);
            case testCompatibility -> testCompatibility(testCase);
            case testPriority -> testPriority(testCase);
            case testPostAssignment -> testPostAssignment(testCase);
        };
    }

    @Override
    public RuleUnitTestReport executeCaseFile(String caseFilePath) {
        BusinessRuleTestCaseSet caseSet = BusinessRuleTestCaseSet.fromFile(Path.of(caseFilePath));
        RuleUnitTestCaseSetReport report = executeCaseSet(caseSet);
        if (report.caseReports().isEmpty()) {
            return RuleUnitTestReport.failed(caseFilePath, RuleUnitActualResult.failed("No cases found"),
                    List.of("No cases found in file: " + caseFilePath));
        }
        if (report.caseReports().size() == 1) {
            return report.caseReports().get(0);
        }
        List<String> failures = report.caseReports().stream()
                .filter(caseReport -> !caseReport.passed())
                .flatMap(caseReport -> caseReport.failures().stream())
                .toList();
        RuleUnitActualResult actual = report.passed()
                ? report.caseReports().get(0).actual()
                : RuleUnitActualResult.failed(String.join("; ", failures));
        return new RuleUnitTestReport(caseFilePath, report.passed(), actual, failures);
    }

    @Override
    public RuleUnitTestCaseSetReport executeCaseSet(BusinessRuleTestCaseSet caseSet) {
        List<RuleUnitTestReport> reports = new ArrayList<>();
        for (BusinessRuleTestCase testCase : caseSet.cases()) {
            reports.add(executeCase(testCase));
        }
        boolean passed = reports.stream().allMatch(RuleUnitTestReport::passed);
        return new RuleUnitTestCaseSetReport(passed, reports);
    }

    @Override
    public RuleUnitTestReport testAssignment(BusinessRuleTestCase testCase) {
        Module module = module(testCase);
        RuleUnitActualResult actual = singleRuleExecutor.testAssignment(
                module.getId(),
                module,
                inputConverter.toInput(testCase));
        return toReport(testCase, actual);
    }

    @Override
    public RuleUnitTestReport testCompatibility(BusinessRuleTestCase testCase) {
        Module module = module(testCase);
        RuleUnitActualResult actual = singleRuleExecutor.testCompatibility(
                module.getId(),
                module,
                inputConverter.toInput(testCase));
        return toReport(testCase, actual);
    }

    @Override
    public RuleUnitTestReport testPriority(BusinessRuleTestCase testCase) {
        Module module = module(testCase);
        RuleUnitActualResult actual = singleRuleExecutor.testPriority(
                module.getId(),
                module,
                inputConverter.toInput(testCase));
        return toReport(testCase, actual);
    }

    @Override
    public RuleUnitTestReport testPostAssignment(BusinessRuleTestCase testCase) {
        Module module = module(testCase);
        RuleUnitActualResult actual = singleRuleExecutor.testPostAssignment(
                module.getId(),
                module,
                inputConverter.toInput(testCase));
        return toReport(testCase, actual);
    }

    private RuleUnitTestReport toReport(BusinessRuleTestCase testCase, RuleUnitActualResult actual) {
        List<String> failures = comparator.compare(testCase.expect(), actual);
        if (failures.isEmpty()) {
            return RuleUnitTestReport.passed(testCase.id(), actual);
        }
        return RuleUnitTestReport.failed(testCase.id(), actual, failures);
    }

    private Module module(BusinessRuleTestCase testCase) {
        if (moduleRegistry == null) {
            throw new RuleTransException("RuleUnitModuleRegistry is required");
        }
        Module module = moduleRegistry.moduleFor(testCase.id());
        if (module == null) {
            throw new RuleTransException("Module not found for case: " + testCase.id());
        }
        return module;
    }

}
