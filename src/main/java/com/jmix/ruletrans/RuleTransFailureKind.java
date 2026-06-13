package com.jmix.ruletrans;

/**
 * Failure categories surfaced by the RuleTrans end-to-end pipeline.
 */
public enum RuleTransFailureKind {
    NONE,
    INVALID_REQUEST,
    CATEGORY_IDENTIFICATION_FAILED,
    CODE_GENERATION_FAILED,
    COMPILATION_FAILED,
    BUSINESS_CASE_GENERATION_FAILED,
    BUSINESS_CASE_SCHEMA_INVALID,
    RULE_UNIT_INFRA_FAILED,
    RULE_LOGIC_FAILED,
    RETRY_EXHAUSTED
}
