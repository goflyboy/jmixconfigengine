package com.jmix.ruletrans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.executor.bmodel.attr.DynamicAttributeType;
import com.jmix.executor.bmodel.para.ParaType;
import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.var.ParaVar;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.ruletrans.testgen.business.BusinessRuleFamily;
import com.jmix.ruletrans.testgen.business.BusinessRuleTestCase;
import com.jmix.ruletrans.testgen.business.RuleUnitParameter;
import com.jmix.ruletrans.testgen.business.RuleUnitPart;
import com.jmix.ruletrans.testgen.business.RuleUnitPartCategory;
import com.jmix.ruletrans.testgen.business.TestEnvironment;
import com.jmix.tool.bbuilder.anno.DAttrAnno1;
import com.jmix.tool.bbuilder.anno.DAttrAnno2;
import com.jmix.tool.bbuilder.anno.DAttrAnno3;
import com.jmix.tool.bbuilder.anno.DAttrAnno4;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.ParaAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;
import com.jmix.tool.impl.llm.LLMInvoker;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Smart-home system test for Assignment rules running through the full RuleTrans pipeline.
 */
public class SmartHomeAssignmentRuleTransSystemTest extends RuleTransPipelineTestBase {

    private static final String AWAY_ENHANCED_CAMERA_RULE =
            "当家庭模式为 Away（离家）且安防等级为 Enhanced（增强）时，"
                    + "室外 4K 摄像头数量等于 1；否则数量等于 0。";

    @Override
    protected boolean diagnosticsEnabled() {
        return true;
    }

    @Test
    public void testAwayEnhancedAssignsOutdoorCamera() {
        assignmentRule(
                AWAY_ENHANCED_CAMERA_RULE,
                expectJavaContainsAll("addBoolAnd", "addBoolOr", "addEquality", "onlyEnforceIf"),
                caseNumsEqualThan(3),
                assignmentCasesOnly(),
                caseContain(
                        params("homeMode", "Away", "securityLevel", "Enhanced"),
                        parts("cameraOutdoor4k", 1),
                        "命中 if 分支"),
                caseContain(
                        params("homeMode", "Home", "securityLevel", "Basic"),
                        parts("cameraOutdoor4k", 0),
                        "命中 else 分支"),
                caseContain(
                        given(
                                params("homeMode", "Away", "securityLevel", "Enhanced"),
                                parts("cameraOutdoor4k", 2)),
                        compatible(false),
                        "无解边界"));
    }

    private RuleTransPipelineRunResult assignmentRule(
            String naturalLanguage,
            RuleTransJavaExpectation javaExpectation,
            RuleTransBusinessCaseExpectation... caseExpectations) {
        RuleTransPipelineRunResult result = fakeLlmBase().assertRuleTrans(
                SmartHomeAssignmentFacts.class,
                naturalLanguage,
                javaExpectation,
                RuleTransBusinessCaseExpectation.allOf(caseExpectations));
        print(result);
        assertNotNull(result.pipelineResult().ruleUnitReport(), "RuleUnit report");
        assertTrue(result.pipelineResult().ruleUnitReport().passed(),
                () -> result.pipelineResult().ruleUnitReport().caseReports().toString());
        return result;
    }

    private RuleTransPipelineTestBase fakeLlmBase() {
        return new RuleTransPipelineTestBase() {

            @Override
            protected LLMInvoker llmInvoker() {
                return new QueueInvoker(
                        """
                                ["camera"]
                                """,
                        awayEnhancedCameraMethodBody(),
                        awayEnhancedCameraBusinessCases());
            }

            @Override
            protected boolean diagnosticsEnabled() {
                return true;
            }
        };
    }

    private RuleTransJavaExpectation expectJavaContainsAll(String... snippets) {
        return methodBody -> {
            for (String snippet : snippets) {
                assertTrue(methodBody.contains(snippet),
                        () -> "Generated method body should contain " + snippet + ":\n" + methodBody);
            }
        };
    }

    private RuleTransBusinessCaseExpectation caseNumsEqualThan(int minCaseCount) {
        return caseSet -> {
            assertNotNull(caseSet, "businessCaseSet");
            assertTrue(caseSet.cases().size() >= minCaseCount,
                    () -> "Expected at least " + minCaseCount + " business cases:\n" + caseSet.toJson());
        };
    }

    private RuleTransBusinessCaseExpectation assignmentCasesOnly() {
        return caseSet -> {
            assertNotNull(caseSet, "businessCaseSet");
            for (BusinessRuleTestCase testCase : caseSet.cases()) {
                assertEquals(BusinessRuleFamily.ASSIGNMENT, testCase.businessFamily(), testCase.id());
                assertEquals(TestEnvironment.CONSTRAINT, testCase.environment(), testCase.id());
                assertEquals("testAssignment", testCase.serviceMethod(), testCase.id());
            }
        };
    }

    private RuleTransBusinessCaseExpectation caseContain(
            CaseShape expectedGiven,
            CaseShape expectedExpect,
            String reason) {
        return caseSet -> {
            assertNotNull(caseSet, "businessCaseSet");
            boolean found = caseSet.cases().stream().anyMatch(testCase ->
                    containsParameters(testCase.given().parameters(), expectedGiven.parameters())
                            && containsParts(testCase.given().parts(), expectedGiven.parts())
                            && containsPartCategories(testCase.given().partCategories(), expectedGiven.partCategories())
                            && compatibleMatches(testCase.expect().compatible(), expectedExpect.compatible())
                            && containsParameters(testCase.expect().parameters(), expectedExpect.parameters())
                            && containsParts(testCase.expect().parts(), expectedExpect.parts())
                            && containsReason(testCase, reason));
            assertTrue(found, () -> "Business case not found for reason [" + reason + "]\n" + caseSet.toJson());
        };
    }

    private CaseShape params(String... codeValuePairs) {
        assertEquals(0, codeValuePairs.length % 2, "params must be code/value pairs");
        List<RuleUnitParameter> parameters = new ArrayList<>();
        for (int i = 0; i < codeValuePairs.length; i += 2) {
            parameters.add(new RuleUnitParameter(codeValuePairs[i], codeValuePairs[i + 1]));
        }
        return new CaseShape(null, parameters, List.of(), List.of());
    }

    private CaseShape parts(Object... codeQuantityPairs) {
        assertEquals(0, codeQuantityPairs.length % 2, "parts must be code/quantity pairs");
        List<RuleUnitPart> parts = new ArrayList<>();
        for (int i = 0; i < codeQuantityPairs.length; i += 2) {
            String code = String.valueOf(codeQuantityPairs[i]);
            Integer quantity = ((Number) codeQuantityPairs[i + 1]).intValue();
            parts.add(new RuleUnitPart(code, quantity));
        }
        return new CaseShape(null, List.of(), parts, List.of());
    }

    private CaseShape compatible(boolean compatible) {
        return new CaseShape(compatible, List.of(), List.of(), List.of());
    }

    private CaseShape given(CaseShape... shapes) {
        return merge(shapes);
    }

    private CaseShape merge(CaseShape... shapes) {
        Boolean compatible = null;
        List<RuleUnitParameter> parameters = new ArrayList<>();
        List<RuleUnitPart> parts = new ArrayList<>();
        List<RuleUnitPartCategory> partCategories = new ArrayList<>();
        for (CaseShape shape : shapes) {
            if (shape == null) {
                continue;
            }
            compatible = shape.compatible() == null ? compatible : shape.compatible();
            parameters.addAll(shape.parameters());
            parts.addAll(shape.parts());
            partCategories.addAll(shape.partCategories());
        }
        return new CaseShape(compatible, parameters, parts, partCategories);
    }

    private boolean compatibleMatches(Boolean actual, Boolean expected) {
        return expected == null || Objects.equals(actual, expected);
    }

    private boolean containsReason(BusinessRuleTestCase testCase, String reason) {
        return reason == null || reason.isBlank()
                || (testCase.note() != null && testCase.note().contains(reason));
    }

    private boolean containsParameters(List<RuleUnitParameter> actual, List<RuleUnitParameter> expected) {
        for (RuleUnitParameter expectedParameter : expected) {
            boolean matched = actual.stream().anyMatch(parameter ->
                    Objects.equals(parameter.code(), expectedParameter.code())
                            && nullableEquals(expectedParameter.value(), parameter.value())
                            && nullableEquals(expectedParameter.hidden(), parameter.hidden()));
            if (!matched) {
                return false;
            }
        }
        return true;
    }

    private boolean containsParts(List<RuleUnitPart> actual, List<RuleUnitPart> expected) {
        for (RuleUnitPart expectedPart : expected) {
            boolean matched = actual.stream().anyMatch(part ->
                    Objects.equals(part.code(), expectedPart.code())
                            && nullableEquals(expectedPart.quantity(), part.quantity())
                            && nullableEquals(expectedPart.isSelected(), part.isSelected())
                            && nullableEquals(expectedPart.hidden(), part.hidden())
                            && mapContains(part.attrs(), expectedPart.attrs()));
            if (!matched) {
                return false;
            }
        }
        return true;
    }

    private boolean containsPartCategories(
            List<RuleUnitPartCategory> actual,
            List<RuleUnitPartCategory> expected) {
        for (RuleUnitPartCategory expectedCategory : expected) {
            boolean matched = actual.stream().anyMatch(category ->
                    Objects.equals(category.category(), expectedCategory.category())
                            && nullableEquals(expectedCategory.aggregate(), category.aggregate())
                            && nullableEquals(expectedCategory.operator(), category.operator())
                            && nullableEquals(expectedCategory.value(), category.value())
                            && mapContains(category.where(), expectedCategory.where()));
            if (!matched) {
                return false;
            }
        }
        return true;
    }

    private boolean nullableEquals(Object expected, Object actual) {
        return expected == null || Objects.equals(expected, actual);
    }

    private boolean mapContains(Map<String, String> actual, Map<String, String> expected) {
        if (expected == null || expected.isEmpty()) {
            return true;
        }
        Map<String, String> safeActual = actual == null ? Map.of() : actual;
        for (Map.Entry<String, String> entry : expected.entrySet()) {
            if (!Objects.equals(safeActual.get(entry.getKey()), entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    private String awayEnhancedCameraMethodBody() {
        return """
                AlgCPBoolVar away = homeMode.option("Away").selectedVar();
                AlgCPBoolVar enhanced = securityLevel.option("Enhanced").selectedVar();
                AlgCPBoolVar awayEnhanced = model().newBoolVar("away_enhanced_security");
                model().addBoolAnd(new AlgCPLiteral[] {away, enhanced}).onlyEnforceIf(awayEnhanced);
                model().addBoolOr(new AlgCPLiteral[] {away.not(), enhanced.not()})
                        .onlyEnforceIf(awayEnhanced.not());
                model().addEquality(cameraOutdoor4k.quantityVar(), 1).onlyEnforceIf(awayEnhanced);
                model().addEquality(cameraOutdoor4k.quantityVar(), 0).onlyEnforceIf(awayEnhanced.not());
                """;
    }

    private String awayEnhancedCameraBusinessCases() {
        return """
                {
                  "ruleMethod": "assignOutdoorCameraByAwayEnhanced",
                  "cases": [
                    {
                      "id": "SMART-HOME-ASSIGN-IF-001",
                      "title": "离家且增强安防时配置室外 4K 摄像头",
                      "businessFamily": "ASSIGNMENT",
                      "scenario": "参数 if-else 推导部件数量",
                      "environment": "CONSTRAINT",
                      "serviceMethod": "testAssignment",
                      "given": {
                        "parameters": [
                          {"code": "homeMode", "value": "Away"},
                          {"code": "securityLevel", "value": "Enhanced"},
                          {"code": "houseSize", "value": "Small"},
                          {"code": "roomCount", "value": "3"},
                          {"code": "cameraResolution", "value": "P4K"}
                        ]
                      },
                      "expect": {
                        "parts": [
                          {"code": "cameraOutdoor4k", "quantity": 1}
                        ]
                      },
                      "note": "命中 if 分支：家庭模式 Away 且安防等级 Enhanced，用户期望室外 4K 摄像头数量为 1。"
                    },
                    {
                      "id": "SMART-HOME-ASSIGN-ELSE-001",
                      "title": "在家且基础安防时不配置室外 4K 摄像头",
                      "businessFamily": "ASSIGNMENT",
                      "scenario": "参数 if-else 推导部件数量",
                      "environment": "CONSTRAINT",
                      "serviceMethod": "testAssignment",
                      "given": {
                        "parameters": [
                          {"code": "homeMode", "value": "Home"},
                          {"code": "securityLevel", "value": "Basic"},
                          {"code": "houseSize", "value": "Small"},
                          {"code": "roomCount", "value": "3"},
                          {"code": "cameraResolution", "value": "P4K"}
                        ]
                      },
                      "expect": {
                        "parts": [
                          {"code": "cameraOutdoor4k", "quantity": 0}
                        ]
                      },
                      "note": "命中 else 分支：未同时满足 Away 与 Enhanced，用户期望室外 4K 摄像头数量为 0。"
                    },
                    {
                      "id": "SMART-HOME-ASSIGN-NO-SOLUTION-001",
                      "title": "命中条件时不能配置两个室外 4K 摄像头",
                      "businessFamily": "ASSIGNMENT",
                      "scenario": "参数 if-else 推导部件数量",
                      "environment": "CONSTRAINT",
                      "serviceMethod": "testAssignment",
                      "given": {
                        "parameters": [
                          {"code": "homeMode", "value": "Away"},
                          {"code": "securityLevel", "value": "Enhanced"},
                          {"code": "houseSize", "value": "Small"},
                          {"code": "roomCount", "value": "3"},
                          {"code": "cameraResolution", "value": "P4K"}
                        ],
                        "parts": [
                          {"code": "cameraOutdoor4k", "quantity": 2}
                        ]
                      },
                      "expect": {
                        "compatible": false
                      },
                      "note": "无解边界：规则已把室外 4K 摄像头数量固定为 1，请求数量为 2 时应无解。"
                    }
                  ]
                }
                """;
    }

    private record CaseShape(
            Boolean compatible,
            List<RuleUnitParameter> parameters,
            List<RuleUnitPart> parts,
            List<RuleUnitPartCategory> partCategories) {

        private CaseShape {
            parameters = parameters == null ? List.of() : List.copyOf(parameters);
            parts = parts == null ? List.of() : List.copyOf(parts);
            partCategories = partCategories == null ? List.of() : List.copyOf(partCategories);
        }
    }

    private static final class QueueInvoker implements LLMInvoker {

        private final Deque<String> responses = new ArrayDeque<>();

        private QueueInvoker(String... responses) {
            this.responses.addAll(List.of(responses));
        }

        @Override
        public String generate(String systemMessage, String userMessage) {
            if (responses.isEmpty()) {
                throw new IllegalStateException("No fake LLM response left for prompt: " + userMessage);
            }
            return responses.removeFirst();
        }

        @Override
        public String getConfigInfo() {
            return "smart-home-assignment-fixture";
        }
    }

    @ModuleAnno(id = 8017L)
    public static class SmartHomeAssignmentFacts extends ModuleAlgBase {

        @ParaAnno(description = "家庭模式", defaultValue = "Home",
                options = {"Home", "Away", "Night"})
        private ParaVar homeMode;

        @ParaAnno(description = "安防等级", defaultValue = "Basic",
                options = {"Basic", "Enhanced"})
        private ParaVar securityLevel;

        @ParaAnno(description = "户型大小", defaultValue = "Small",
                options = {"Small", "Large"})
        private ParaVar houseSize;

        @ParaAnno(description = "房间数量", type = ParaType.INTEGER, defaultValue = "1",
                minValue = "1", maxValue = "6")
        private ParaVar roomCount;

        @ParaAnno(description = "总功耗", type = ParaType.INTEGER, defaultValue = "0",
                minValue = "0", maxValue = "0")
        private ParaVar totalPower;

        @ParaAnno(description = "告警等级", type = ParaType.INTEGER, defaultValue = "0",
                minValue = "0", maxValue = "0")
        private ParaVar alarmLevel;

        @ParaAnno(description = "节能模式", defaultValue = "Normal", options = {"Normal"})
        private ParaVar energyMode;

        @ParaAnno(description = "摄像清晰度", defaultValue = "P1080",
                options = {"P1080", "P4K"})
        private ParaVar cameraResolution;

        @PartAnno(code = "hub", required = false)
        @DAttrAnno1(code = "Protocol", dynAttrType = DynamicAttributeType.E_STRING,
                options = {"Zigbee:Zigbee", "Matter:Matter"})
        @DAttrAnno2(code = "Power", options = {"P5:5", "P8:8"})
        private PartCategoryVar hub;

        @PartAnno(fatherCode = "hub", attrs = {"Zigbee", "5"}, maxQuantity = 0)
        private PartVar hubBasic;

        @PartAnno(fatherCode = "hub", attrs = {"Matter", "8"}, maxQuantity = 0)
        private PartVar hubMatter;

        @PartAnno(code = "camera", required = false)
        @DAttrAnno1(code = "Place", dynAttrType = DynamicAttributeType.E_STRING,
                options = {"Indoor:Indoor", "Outdoor:Outdoor"})
        @DAttrAnno2(code = "Resolution", dynAttrType = DynamicAttributeType.E_STRING,
                options = {"Res1080:1080P", "Res4K:4K"})
        @DAttrAnno3(code = "Power", options = {"P8:8", "P15:15"})
        @DAttrAnno4(code = "Score", options = {"S1:1", "S3:3"})
        private PartCategoryVar camera;

        @PartAnno(fatherCode = "camera", attrs = {"Indoor", "1080P", "8", "1"}, maxQuantity = 0)
        private PartVar cameraIndoor;

        @PartAnno(fatherCode = "camera", attrs = {"Outdoor", "4K", "15", "3"}, maxQuantity = 2)
        private PartVar cameraOutdoor4k;

        @PartAnno(code = "sensor", required = false)
        @DAttrAnno1(code = "Type", dynAttrType = DynamicAttributeType.E_STRING,
                options = {"Door:Door", "Motion:Motion", "Smoke:Smoke"})
        @DAttrAnno2(code = "Power", options = {"P1:1", "P2:2", "P3:3"})
        @DAttrAnno3(code = "Score", options = {"S1:1", "S2:2"})
        private PartCategoryVar sensor;

        @PartAnno(fatherCode = "sensor", attrs = {"Door", "1", "1"}, maxQuantity = 0)
        private PartVar sensorDoor;

        @PartAnno(fatherCode = "sensor", attrs = {"Motion", "2", "1"}, maxQuantity = 0)
        private PartVar sensorMotion;

        @PartAnno(fatherCode = "sensor", attrs = {"Smoke", "3", "2"}, maxQuantity = 0)
        private PartVar sensorSmoke;

        @PartAnno(code = "lock", required = false)
        @DAttrAnno1(code = "Secure", dynAttrType = DynamicAttributeType.E_STRING,
                options = {"Basic:Basic", "Pro:Pro"})
        @DAttrAnno2(code = "Power", options = {"P3:3", "P6:6"})
        @DAttrAnno3(code = "Score", options = {"S1:1", "S3:3"})
        private PartCategoryVar lock;

        @PartAnno(fatherCode = "lock", attrs = {"Basic", "3", "1"}, maxQuantity = 0)
        private PartVar lockBasic;

        @PartAnno(fatherCode = "lock", attrs = {"Pro", "6", "3"}, maxQuantity = 0)
        private PartVar lockPro;

        @PartAnno(code = "climate", required = false)
        @DAttrAnno1(code = "Power", options = {"P12:12", "P20:20"})
        @DAttrAnno2(code = "Type", dynAttrType = DynamicAttributeType.E_STRING,
                options = {"Small:Small", "High:High"})
        private PartCategoryVar climate;

        @PartAnno(fatherCode = "climate", attrs = {"12", "Small"}, maxQuantity = 0)
        private PartVar climateSmall;

        @PartAnno(fatherCode = "climate", attrs = {"20", "High"}, maxQuantity = 0)
        private PartVar climateHigh;

        @PartAnno(code = "light", required = false)
        @DAttrAnno1(code = "Room", dynAttrType = DynamicAttributeType.E_STRING,
                options = {"Hall:Hall", "Bedroom:Bedroom"})
        @DAttrAnno2(code = "Power", options = {"P4:4", "P6:6"})
        private PartCategoryVar light;

        @PartAnno(fatherCode = "light", attrs = {"Hall", "4"}, maxQuantity = 0)
        private PartVar lightHall;

        @PartAnno(fatherCode = "light", attrs = {"Bedroom", "6"}, maxQuantity = 0)
        private PartVar lightBedroom;

        @PartAnno(code = "sceneDevice", required = false)
        @DAttrAnno1(code = "Scene", dynAttrType = DynamicAttributeType.E_STRING,
                options = {"Away:Away", "Night:Night"})
        @DAttrAnno2(code = "Power", options = {"P2:2", "P3:3"})
        private PartCategoryVar sceneDevice;

        @PartAnno(fatherCode = "sceneDevice", attrs = {"Away", "2"}, maxQuantity = 0)
        private PartVar sceneAway;

        @PartAnno(fatherCode = "sceneDevice", attrs = {"Night", "3"}, maxQuantity = 0)
        private PartVar sceneNight;
    }
}
