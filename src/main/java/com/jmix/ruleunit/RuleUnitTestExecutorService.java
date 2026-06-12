package com.jmix.ruleunit;

import com.jmix.ruletrans.testgen.business.BusinessRuleTestCase;
import com.jmix.ruletrans.testgen.business.BusinessRuleTestCaseSet;

/**
 * Executes business-readable rule unit JSON cases.
 */
public interface RuleUnitTestExecutorService {

    RuleUnitTestReport executeCase(BusinessRuleTestCase testCase);

    RuleUnitTestReport executeCaseFile(String caseFilePath);

    RuleUnitTestCaseSetReport executeCaseSet(BusinessRuleTestCaseSet caseSet);

    RuleUnitTestReport testAssignment(BusinessRuleTestCase testCase);

    RuleUnitTestReport testCompatibility(BusinessRuleTestCase testCase);

    RuleUnitTestReport testPriority(BusinessRuleTestCase testCase);

    RuleUnitTestReport testPostAssignment(BusinessRuleTestCase testCase);
}
