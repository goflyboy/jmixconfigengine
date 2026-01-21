package com.jmix.scenario.perf20;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.coretest.ConstraintAlgImplTestBase;
import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.bmodel.ConstraintConfig;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.ParaAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.Literal;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

/**
 * 20参数性能测试类
 * 测试包含20个参数的模块性能表现
 * 
 * @since 2025-09-22
 */
@Slf4j
public class Perf20ParasTest extends ModuleScenarioTestBase {
    public Perf20ParasTest() {
        super(Perf20ParasConstraint.class);
    }

    // ---------------模型的定义start----------------------------------------
    /**
     * 20参数性能测试约束模型类
     * 
     * @since 2025-09-23
     */
    @ModuleAnno(id = 123L)
    private static class Perf20ParasConstraint extends ConstraintAlgImplTestBase {
        @ParaAnno(options = {
                "op00101", "op00102", "op00103", "op00104", "op00105",
                "op00106", "op00107", "op00108", "op00109", "op00110"
        })
        private ParaVar p001;

        @ParaAnno(options = {
                "op00201", "op00202", "op00203", "op00204", "op00205",
                "op00206", "op00207", "op00208", "op00209", "op00210"
        })
        private ParaVar p002;

        @ParaAnno(options = {
                "op00301", "op00302", "op00303", "op00304", "op00305",
                "op00306", "op00307", "op00308", "op00309", "op00310"
        })
        private ParaVar p003;

        @ParaAnno(options = {
                "op00401", "op00402", "op00403", "op00404", "op00405",
                "op00406", "op00407", "op00408", "op00409", "op00410"
        })
        private ParaVar p004;

        @ParaAnno(options = {
                "op00501", "op00502", "op00503", "op00504", "op00505",
                "op00506", "op00507", "op00508", "op00509", "op00510"
        })
        private ParaVar p005;

        @ParaAnno(options = {
                "op00601", "op00602", "op00603", "op00604", "op00605",
                "op00606", "op00607", "op00608", "op00609", "op00610"
        })
        private ParaVar p006;

        @ParaAnno(options = {
                "op00701", "op00702", "op00703", "op00704", "op00705",
                "op00706", "op00707", "op00708", "op00709", "op00710"
        })
        private ParaVar p007;

        @ParaAnno(options = {
                "op00801", "op00802", "op00803", "op00804", "op00805",
                "op00806", "op00807", "op00808", "op00809", "op00810"
        })
        private ParaVar p008;

        @ParaAnno(options = {
                "op00901", "op00902", "op00903", "op00904", "op00905",
                "op00906", "op00907", "op00908", "op00909", "op00910"
        })
        private ParaVar p009;

        @ParaAnno(options = {
                "op01001", "op01002", "op01003", "op01004", "op01005",
                "op01006", "op01007", "op01008", "op01009", "op01010"
        })
        private ParaVar p010;

        @ParaAnno(options = {
                "op01101", "op01102", "op01103", "op01104", "op01105",
                "op01106", "op01107", "op01108", "op01109", "op01110"
        })
        private ParaVar p011;

        @ParaAnno(options = {
                "op01201", "op01202", "op01203", "op01204", "op01205",
                "op01206", "op01207", "op01208", "op01209", "op01210"
        })
        private ParaVar p012;

        @ParaAnno(options = {
                "op01301", "op01302", "op01303", "op01304", "op01305",
                "op01306", "op01307", "op01308", "op01309", "op01310"
        })
        private ParaVar p013;

        @ParaAnno(options = {
                "op01401", "op01402", "op01403", "op01404", "op01405",
                "op01406", "op01407", "op01408", "op01409", "op01410"
        })
        private ParaVar p014;

        @ParaAnno(options = {
                "op01501", "op01502", "op01503", "op01504", "op01505",
                "op01506", "op01507", "op01508", "op01509", "op01510"
        })
        private ParaVar p015;

        @ParaAnno(options = {
                "op01601", "op01602", "op01603", "op01604", "op01605",
                "op01606", "op01607", "op01608", "op01609", "op01610"
        })
        private ParaVar p016;

        @ParaAnno(options = {
                "op01701", "op01702", "op01703", "op01704", "op01705",
                "op01706", "op01707", "op01708", "op01709", "op01710"
        })
        private ParaVar p017;

        @ParaAnno(options = {
                "op01801", "op01802", "op01803", "op01804", "op01805",
                "op01806", "op01807", "op01808", "op01809", "op01810"
        })
        private ParaVar p018;

        @ParaAnno(options = {
                "op01901", "op01902", "op01903", "op01904", "op01905",
                "op01906", "op01907", "op01908", "op01909", "op01910"
        })
        private ParaVar p019;

        @ParaAnno(options = {
                "op02001", "op02002", "op02003", "op02004", "op02005",
                "op02006", "op02007", "op02008", "op02009", "op02010"
        })
        private ParaVar p020;

        @PartAnno(maxQuantity = 10)
        private PartVar pt001;

        @PartAnno(maxQuantity = 10)
        private PartVar pt002;

        @PartAnno(maxQuantity = 10)
        private PartVar pt003;

        @PartAnno(maxQuantity = 10)
        private PartVar pt004;

        @PartAnno(maxQuantity = 10)
        private PartVar pt005;

        @CodeRuleAnno(normalNaturalCode = "if p001.value == op00102 then p002.value != op00202")
        private void rule01() {
            BoolVar condition = model.newBoolVar("rule01_condition");
            model.addEquality(p001.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p002.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p001.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p002.value == op00202 then p003.value != op00302")
        private void rule02() {
            BoolVar condition = model.newBoolVar("rule02_condition");
            model.addEquality(p002.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p003.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p002.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p003.value == op00302 then p004.value != op00402")
        private void rule03() {
            BoolVar condition = model.newBoolVar("rule03_condition");
            model.addEquality(p003.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p004.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p003.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p004.value == op00402 then p005.value != op00502")
        private void rule04() {
            BoolVar condition = model.newBoolVar("rule04_condition");
            model.addEquality(p004.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p005.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p004.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p005.value == op00502 then p006.value != op00602")
        private void rule05() {
            BoolVar condition = model.newBoolVar("rule05_condition");
            model.addEquality(p005.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p006.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p005.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p006.value == op00602 then p007.value != op00702")
        private void rule06() {
            BoolVar condition = model.newBoolVar("rule06_condition");
            model.addEquality(p006.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p007.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p006.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p007.value == op00702 then p008.value != op00802")
        private void rule07() {
            BoolVar condition = model.newBoolVar("rule07_condition");
            model.addEquality(p007.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p008.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p007.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p008.value == op00802 then p009.value != op00902")
        private void rule08() {
            BoolVar condition = model.newBoolVar("rule08_condition");
            model.addEquality(p008.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p009.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p008.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p009.value == op00902 then p010.value != op01002")
        private void rule09() {
            BoolVar condition = model.newBoolVar("rule09_condition");
            model.addEquality(p009.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p010.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p009.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p010.value == op01002 then p011.value != op01102")
        private void rule10() {
            BoolVar condition = model.newBoolVar("rule10_condition");
            model.addEquality(p010.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p011.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p010.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p011.value == op01102 then p012.value != op01202")
        private void rule11() {
            BoolVar condition = model.newBoolVar("rule11_condition");
            model.addEquality(p011.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p012.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p011.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p012.value == op01202 then p013.value != op01302")
        private void rule12() {
            BoolVar condition = model.newBoolVar("rule12_condition");
            model.addEquality(p012.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p013.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p012.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p013.value == op01302 then p014.value != op01402")
        private void rule13() {
            BoolVar condition = model.newBoolVar("rule13_condition");
            model.addEquality(p013.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p014.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p013.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p014.value == op01402 then p015.value != op01502")
        private void rule14() {
            BoolVar condition = model.newBoolVar("rule14_condition");
            model.addEquality(p014.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p015.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p014.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p015.value == op01502 then p016.value != op01602")
        private void rule15() {
            BoolVar condition = model.newBoolVar("rule15_condition");
            model.addEquality(p015.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p016.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p015.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p016.value == op01602 then p017.value != op01702")
        private void rule16() {
            BoolVar condition = model.newBoolVar("rule16_condition");
            model.addEquality(p016.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p017.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p016.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p017.value == op01702 then p018.value != op01802")
        private void rule17() {
            BoolVar condition = model.newBoolVar("rule17_condition");
            model.addEquality(p017.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p018.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p017.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p018.value == op01802 then p019.value != op01902")
        private void rule18() {
            BoolVar condition = model.newBoolVar("rule18_condition");
            model.addEquality(p018.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p019.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p018.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p019.value == op01902 then p020.value != op02002")
        private void rule19() {
            BoolVar condition = model.newBoolVar("rule19_condition");
            model.addEquality(p019.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p020.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p019.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p020.value == op02002 then p001.value != op00102")
        private void rule20() {
            BoolVar condition = model.newBoolVar("rule20_condition");
            model.addEquality(p020.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p001.value, 2).onlyEnforceIf(condition);
            model.addDifferent(p020.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if(p011.value==op01101 && p012.value==op01201) then pt001.qty=1 "
                + "else if(p011.value==op01102 && p012.value==op01202) then pt001.qty=2 else pt001.qty=3")
        private void rule21() {
            BoolVar condition1 = model.newBoolVar("rule21_condition1");
            Literal[] cond1Literals = new Literal[] {
                    p011.getParaOptionByCode("op01101").getIsSelectedVar(),
                    p012.getParaOptionByCode("op01201").getIsSelectedVar()
            };
            model.addBoolAnd(cond1Literals).onlyEnforceIf(condition1);
            model.addBoolOr(new Literal[] {
                    p011.getParaOptionByCode("op01101").getIsSelectedVar().not(),
                    p012.getParaOptionByCode("op01201").getIsSelectedVar().not()
            }).onlyEnforceIf(condition1.not());

            BoolVar condition2 = model.newBoolVar("rule21_condition2");
            Literal[] cond2Literals = new Literal[] {
                    p011.getParaOptionByCode("op01102").getIsSelectedVar(),
                    p012.getParaOptionByCode("op01202").getIsSelectedVar()
            };
            model.addBoolAnd(cond2Literals).onlyEnforceIf(condition2);
            model.addBoolOr(new Literal[] {
                    p011.getParaOptionByCode("op01102").getIsSelectedVar().not(),
                    p012.getParaOptionByCode("op01202").getIsSelectedVar().not()
            }).onlyEnforceIf(condition2.not());

            model.addEquality(pt001.qty, 1).onlyEnforceIf(condition1);
            model.addEquality(pt001.qty, 2).onlyEnforceIf(condition2);
        }

        @CodeRuleAnno(normalNaturalCode = "if(p013.value==op01301 && p014.value==op01401) then pt002.qty=1 "
                + "else if(p013.value==op01302 && p014.value==op01402) then pt002.qty=2 else pt002.qty=3")
        private void rule22() {
            BoolVar condition1 = model.newBoolVar("rule22_condition1");
            Literal[] cond1Literals = new Literal[] {
                    p013.getParaOptionByCode("op01301").getIsSelectedVar(),
                    p014.getParaOptionByCode("op01401").getIsSelectedVar()
            };
            model.addBoolAnd(cond1Literals).onlyEnforceIf(condition1);
            model.addBoolOr(new Literal[] {
                    p013.getParaOptionByCode("op01301").getIsSelectedVar().not(),
                    p014.getParaOptionByCode("op01401").getIsSelectedVar().not()
            }).onlyEnforceIf(condition1.not());

            BoolVar condition2 = model.newBoolVar("rule22_condition2");
            Literal[] cond2Literals = new Literal[] {
                    p013.getParaOptionByCode("op01302").getIsSelectedVar(),
                    p014.getParaOptionByCode("op01402").getIsSelectedVar()
            };
            model.addBoolAnd(cond2Literals).onlyEnforceIf(condition2);
            model.addBoolOr(new Literal[] {
                    p013.getParaOptionByCode("op01302").getIsSelectedVar().not(),
                    p014.getParaOptionByCode("op01402").getIsSelectedVar().not()
            }).onlyEnforceIf(condition2.not());

            model.addEquality(pt002.qty, 1).onlyEnforceIf(condition1);
            model.addEquality(pt002.qty, 2).onlyEnforceIf(condition2);
        }

        @CodeRuleAnno(normalNaturalCode = "if(p015.value==op01501 && p016.value==op01601) then pt003.qty=1 "
                + "else if(p015.value==op01502 && p016.value==op01602) then pt003.qty=2 else pt003.qty=3")
        private void rule23() {
            BoolVar condition1 = model.newBoolVar("rule23_condition1");
            Literal[] cond1Literals = new Literal[] {
                    p015.getParaOptionByCode("op01501").getIsSelectedVar(),
                    p016.getParaOptionByCode("op01601").getIsSelectedVar()
            };
            model.addBoolAnd(cond1Literals).onlyEnforceIf(condition1);
            model.addBoolOr(new Literal[] {
                    p015.getParaOptionByCode("op01501").getIsSelectedVar().not(),
                    p016.getParaOptionByCode("op01601").getIsSelectedVar().not()
            }).onlyEnforceIf(condition1.not());

            BoolVar condition2 = model.newBoolVar("rule23_condition2");
            Literal[] cond2Literals = new Literal[] {
                    p015.getParaOptionByCode("op01502").getIsSelectedVar(),
                    p016.getParaOptionByCode("op01602").getIsSelectedVar()
            };
            model.addBoolAnd(cond2Literals).onlyEnforceIf(condition2);
            model.addBoolOr(new Literal[] {
                    p015.getParaOptionByCode("op01502").getIsSelectedVar().not(),
                    p016.getParaOptionByCode("op01602").getIsSelectedVar().not()
            }).onlyEnforceIf(condition2.not());

            model.addEquality(pt003.qty, 1).onlyEnforceIf(condition1);
            model.addEquality(pt003.qty, 2).onlyEnforceIf(condition2);
        }

        @CodeRuleAnno(normalNaturalCode = "if(p017.value==op01701 && p018.value==op01801) then pt004.qty=1 "
                + "else if(p017.value==op01702 && p018.value==op01802) then pt004.qty=2 else pt004.qty=3")
        private void rule24() {
            BoolVar condition1 = model.newBoolVar("rule24_condition1");
            Literal[] cond1Literals = new Literal[] {
                    p017.getParaOptionByCode("op01701").getIsSelectedVar(),
                    p018.getParaOptionByCode("op01801").getIsSelectedVar()
            };
            model.addBoolAnd(cond1Literals).onlyEnforceIf(condition1);
            model.addBoolOr(new Literal[] {
                    p017.getParaOptionByCode("op01701").getIsSelectedVar().not(),
                    p018.getParaOptionByCode("op01801").getIsSelectedVar().not()
            }).onlyEnforceIf(condition1.not());

            BoolVar condition2 = model.newBoolVar("rule24_condition2");
            Literal[] cond2Literals = new Literal[] {
                    p017.getParaOptionByCode("op01702").getIsSelectedVar(),
                    p018.getParaOptionByCode("op01802").getIsSelectedVar()
            };
            model.addBoolAnd(cond2Literals).onlyEnforceIf(condition2);
            model.addBoolOr(new Literal[] {
                    p017.getParaOptionByCode("op01702").getIsSelectedVar().not(),
                    p018.getParaOptionByCode("op01802").getIsSelectedVar().not()
            }).onlyEnforceIf(condition2.not());

            model.addEquality(pt004.qty, 1).onlyEnforceIf(condition1);
            model.addEquality(pt004.qty, 2).onlyEnforceIf(condition2);
            // model.addEquality((IntVar)pt004.qty, 3).onlyEnforceIf(condition1.not(),
            // condition2.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if(p019.value==op01901 && p020.value==op02001) then pt005.qty=1 "
                + "else if(p019.value==op01902 && p020.value==op02002) then pt005.qty=2 else pt005.qty=3")
        private void rule25() {
            BoolVar condition1 = model.newBoolVar("rule25_condition1");
            Literal[] cond1Literals = new Literal[] {
                    p019.getParaOptionByCode("op01901").getIsSelectedVar(),
                    p020.getParaOptionByCode("op02001").getIsSelectedVar()
            };
            model.addBoolAnd(cond1Literals).onlyEnforceIf(condition1);
            model.addBoolOr(new Literal[] {
                    p019.getParaOptionByCode("op01901").getIsSelectedVar().not(),
                    p020.getParaOptionByCode("op02001").getIsSelectedVar().not()
            }).onlyEnforceIf(condition1.not());

            BoolVar condition2 = model.newBoolVar("rule25_condition2");
            Literal[] cond2Literals = new Literal[] {
                    p019.getParaOptionByCode("op01902").getIsSelectedVar(),
                    p020.getParaOptionByCode("op02002").getIsSelectedVar()
            };
            model.addBoolAnd(cond2Literals).onlyEnforceIf(condition2);
            model.addBoolOr(new Literal[] {
                    p019.getParaOptionByCode("op01902").getIsSelectedVar().not(),
                    p020.getParaOptionByCode("op02002").getIsSelectedVar().not()
            }).onlyEnforceIf(condition2.not());

            model.addEquality(pt005.qty, 1).onlyEnforceIf(condition1);
            model.addEquality(pt005.qty, 2).onlyEnforceIf(condition2);
            // model.addEquality((IntVar)pt005.qty, 3).onlyEnforceIf(condition1.not(),
            // condition2.not());
        }
    }

    // ---------------模型的定义end----------------------------------------
    @Test
    public void testPerformancePt003OnlyOneSolution() {
        executeTest("testPerformancePt003OnlyOneSolution", false);
    }

    @Test
    @Disabled
    public void testPerformancePt003AllSolutions() {
        executeTest("testPerformancePt003AllSolutions", true);
    }

    private void executeTest(String testCaseName, boolean tmpEnumerateAllSolution) {
        long totalTime = 0L;
        int iterations = 10;
        this.setEnumerateAllSolution(tmpEnumerateAllSolution);
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            inferParas("pt003", 2);
        }
        long endTime = System.nanoTime();
        totalTime += TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        long averageTime = totalTime / iterations;
        log.info("{} Average time for inferParas(pt003, 4): {} ms", testCaseName, averageTime);
        assertTrue(averageTime <= 300,
                testCaseName + " Performance test failed: average time " + averageTime + " ms exceeds 300 ms");
    }

    @Override
    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(ConstraintConfig.LOAD_TYPE_FULL); // 全量加载
    }
}