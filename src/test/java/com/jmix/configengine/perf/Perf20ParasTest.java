package com.jmix.configengine.perf;

import com.jmix.configengine.scenario.base.CodeRuleAnno;
import com.jmix.configengine.scenario.base.ModuleAnno;
import com.jmix.configengine.scenario.base.ModuleScenarioTestBase;
import com.jmix.configengine.scenario.base.ParaAnno;
import com.jmix.configengine.scenario.base.PartAnno;
import com.jmix.executor.imodel.ConstraintConfig;
import com.jmix.executor.impl.artifact.ConstraintAlgImpl;
import com.jmix.executor.impl.artifact.ParaVar;
import com.jmix.executor.impl.artifact.PartVar;

import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.Literal;

import lombok.extern.slf4j.Slf4j;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

@Slf4j
@SuppressWarnings("checkstyle:all")
public class Perf20ParasTest extends ModuleScenarioTestBase {

    // ---------------模型的定义start----------------------------------------
    @ModuleAnno(id = 123L)
    static public class Perf20ParasConstraint extends ConstraintAlgImpl {
        @ParaAnno(options = {
                "op00101", "op00102", "op00103", "op00104", "op00105",
                "op00106", "op00107", "op00108", "op00109", "op00110"
        })
        private ParaVar P001;

        @ParaAnno(options = {
                "op00201", "op00202", "op00203", "op00204", "op00205",
                "op00206", "op00207", "op00208", "op00209", "op00210"
        })
        private ParaVar P002;

        @ParaAnno(options = {
                "op00301", "op00302", "op00303", "op00304", "op00305",
                "op00306", "op00307", "op00308", "op00309", "op00310"
        })
        private ParaVar P003;

        @ParaAnno(options = {
                "op00401", "op00402", "op00403", "op00404", "op00405",
                "op00406", "op00407", "op00408", "op00409", "op00410"
        })
        private ParaVar P004;

        @ParaAnno(options = {
                "op00501", "op00502", "op00503", "op00504", "op00505",
                "op00506", "op00507", "op00508", "op00509", "op00510"
        })
        private ParaVar P005;

        @ParaAnno(options = {
                "op00601", "op00602", "op00603", "op00604", "op00605",
                "op00606", "op00607", "op00608", "op00609", "op00610"
        })
        private ParaVar P006;

        @ParaAnno(options = {
                "op00701", "op00702", "op00703", "op00704", "op00705",
                "op00706", "op00707", "op00708", "op00709", "op00710"
        })
        private ParaVar P007;

        @ParaAnno(options = {
                "op00801", "op00802", "op00803", "op00804", "op00805",
                "op00806", "op00807", "op00808", "op00809", "op00810"
        })
        private ParaVar P008;

        @ParaAnno(options = {
                "op00901", "op00902", "op00903", "op00904", "op00905",
                "op00906", "op00907", "op00908", "op00909", "op00910"
        })
        private ParaVar P009;

        @ParaAnno(options = {
                "op01001", "op01002", "op01003", "op01004", "op01005",
                "op01006", "op01007", "op01008", "op01009", "op01010"
        })
        private ParaVar P010;

        @ParaAnno(options = {
                "op01101", "op01102", "op01103", "op01104", "op01105",
                "op01106", "op01107", "op01108", "op01109", "op01110"
        })
        private ParaVar P011;

        @ParaAnno(options = {
                "op01201", "op01202", "op01203", "op01204", "op01205",
                "op01206", "op01207", "op01208", "op01209", "op01210"
        })
        private ParaVar P012;

        @ParaAnno(options = {
                "op01301", "op01302", "op01303", "op01304", "op01305",
                "op01306", "op01307", "op01308", "op01309", "op01310"
        })
        private ParaVar P013;

        @ParaAnno(options = {
                "op01401", "op01402", "op01403", "op01404", "op01405",
                "op01406", "op01407", "op01408", "op01409", "op01410"
        })
        private ParaVar P014;

        @ParaAnno(options = {
                "op01501", "op01502", "op01503", "op01504", "op01505",
                "op01506", "op01507", "op01508", "op01509", "op01510"
        })
        private ParaVar P015;

        @ParaAnno(options = {
                "op01601", "op01602", "op01603", "op01604", "op01605",
                "op01606", "op01607", "op01608", "op01609", "op01610"
        })
        private ParaVar P016;

        @ParaAnno(options = {
                "op01701", "op01702", "op01703", "op01704", "op01705",
                "op01706", "op01707", "op01708", "op01709", "op01710"
        })
        private ParaVar P017;

        @ParaAnno(options = {
                "op01801", "op01802", "op01803", "op01804", "op01805",
                "op01806", "op01807", "op01808", "op01809", "op01810"
        })
        private ParaVar P018;

        @ParaAnno(options = {
                "op01901", "op01902", "op01903", "op01904", "op01905",
                "op01906", "op01907", "op01908", "op01909", "op01910"
        })
        private ParaVar P019;

        @ParaAnno(options = {
                "op02001", "op02002", "op02003", "op02004", "op02005",
                "op02006", "op02007", "op02008", "op02009", "op02010"
        })
        private ParaVar P020;

        @PartAnno(maxQuantity = 10)
        private PartVar PT001;

        @PartAnno(maxQuantity = 10)
        private PartVar PT002;

        @PartAnno(maxQuantity = 10)
        private PartVar PT003;

        @PartAnno(maxQuantity = 10)
        private PartVar PT004;

        @PartAnno(maxQuantity = 10)
        private PartVar PT005;

        @CodeRuleAnno(normalNaturalCode = "if P001.value == op00102 then P002.value != op00202")
        protected void Rule01() {
            BoolVar condition = model.newBoolVar("rule01_condition");
            model.addEquality(P001.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P002.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P001.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if P002.value == op00202 then P003.value != op00302")
        protected void Rule02() {
            BoolVar condition = model.newBoolVar("rule02_condition");
            model.addEquality(P002.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P003.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P002.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if P003.value == op00302 then P004.value != op00402")
        protected void Rule03() {
            BoolVar condition = model.newBoolVar("rule03_condition");
            model.addEquality(P003.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P004.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P003.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if P004.value == op00402 then P005.value != op00502")
        protected void Rule04() {
            BoolVar condition = model.newBoolVar("rule04_condition");
            model.addEquality(P004.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P005.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P004.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if P005.value == op00502 then P006.value != op00602")
        protected void Rule05() {
            BoolVar condition = model.newBoolVar("rule05_condition");
            model.addEquality(P005.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P006.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P005.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if P006.value == op00602 then P007.value != op00702")
        protected void Rule06() {
            BoolVar condition = model.newBoolVar("rule06_condition");
            model.addEquality(P006.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P007.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P006.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if P007.value == op00702 then P008.value != op00802")
        protected void Rule07() {
            BoolVar condition = model.newBoolVar("rule07_condition");
            model.addEquality(P007.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P008.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P007.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if P008.value == op00802 then P009.value != op00902")
        protected void Rule08() {
            BoolVar condition = model.newBoolVar("rule08_condition");
            model.addEquality(P008.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P009.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P008.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if P009.value == op00902 then P010.value != op01002")
        protected void Rule09() {
            BoolVar condition = model.newBoolVar("rule09_condition");
            model.addEquality(P009.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P010.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P009.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if P010.value == op01002 then P011.value != op01102")
        protected void Rule10() {
            BoolVar condition = model.newBoolVar("rule10_condition");
            model.addEquality(P010.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P011.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P010.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if P011.value == op01102 then P012.value != op01202")
        protected void Rule11() {
            BoolVar condition = model.newBoolVar("rule11_condition");
            model.addEquality(P011.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P012.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P011.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if P012.value == op01202 then P013.value != op01302")
        protected void Rule12() {
            BoolVar condition = model.newBoolVar("rule12_condition");
            model.addEquality(P012.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P013.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P012.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if P013.value == op01302 then P014.value != op01402")
        protected void Rule13() {
            BoolVar condition = model.newBoolVar("rule13_condition");
            model.addEquality(P013.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P014.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P013.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if P014.value == op01402 then P015.value != op01502")
        protected void Rule14() {
            BoolVar condition = model.newBoolVar("rule14_condition");
            model.addEquality(P014.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P015.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P014.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if P015.value == op01502 then P016.value != op01602")
        protected void Rule15() {
            BoolVar condition = model.newBoolVar("rule15_condition");
            model.addEquality(P015.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P016.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P015.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if P016.value == op01602 then P017.value != op01702")
        protected void Rule16() {
            BoolVar condition = model.newBoolVar("rule16_condition");
            model.addEquality(P016.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P017.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P016.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if P017.value == op01702 then P018.value != op01802")
        protected void Rule17() {
            BoolVar condition = model.newBoolVar("rule17_condition");
            model.addEquality(P017.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P018.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P017.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if P018.value == op01802 then P019.value != op01902")
        protected void Rule18() {
            BoolVar condition = model.newBoolVar("rule18_condition");
            model.addEquality(P018.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P019.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P018.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if P019.value == op01902 then P020.value != op02002")
        protected void Rule19() {
            BoolVar condition = model.newBoolVar("rule19_condition");
            model.addEquality(P019.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P020.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P019.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if P020.value == op02002 then P001.value != op00102")
        protected void Rule20() {
            BoolVar condition = model.newBoolVar("rule20_condition");
            model.addEquality(P020.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P001.value, 2).onlyEnforceIf(condition);
            model.addDifferent(P020.value, 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if(P011.value==op01101 && P012.value==op01201) then PT001.qty=1 " +
                "else if(P011.value==op01102 && P012.value==op01202) then PT001.qty=2 else PT001.qty=3")
        protected void Rule21() {
            BoolVar condition1 = model.newBoolVar("rule21_condition1");
            Literal[] cond1Literals = new Literal[] {
                    P011.getParaOptionByCode("op01101").getIsSelectedVar(),
                    P012.getParaOptionByCode("op01201").getIsSelectedVar()
            };
            model.addBoolAnd(cond1Literals).onlyEnforceIf(condition1);
            model.addBoolOr(new Literal[] {
                    P011.getParaOptionByCode("op01101").getIsSelectedVar().not(),
                    P012.getParaOptionByCode("op01201").getIsSelectedVar().not()
            }).onlyEnforceIf(condition1.not());

            BoolVar condition2 = model.newBoolVar("rule21_condition2");
            Literal[] cond2Literals = new Literal[] {
                    P011.getParaOptionByCode("op01102").getIsSelectedVar(),
                    P012.getParaOptionByCode("op01202").getIsSelectedVar()
            };
            model.addBoolAnd(cond2Literals).onlyEnforceIf(condition2);
            model.addBoolOr(new Literal[] {
                    P011.getParaOptionByCode("op01102").getIsSelectedVar().not(),
                    P012.getParaOptionByCode("op01202").getIsSelectedVar().not()
            }).onlyEnforceIf(condition2.not());

            model.addEquality((IntVar) PT001.qty, 1).onlyEnforceIf(condition1);
            model.addEquality((IntVar) PT001.qty, 2).onlyEnforceIf(condition2);
            // model.addEquality((IntVar)PT001.qty, 3).onlyEnforceIf(condition1.not(),
            // condition2.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if(P013.value==op01301 && P014.value==op01401) then PT002.qty=1 " +
                "else if(P013.value==op01302 && P014.value==op01402) then PT002.qty=2 else PT002.qty=3")
        protected void Rule22() {
            BoolVar condition1 = model.newBoolVar("rule22_condition1");
            Literal[] cond1Literals = new Literal[] {
                    P013.getParaOptionByCode("op01301").getIsSelectedVar(),
                    P014.getParaOptionByCode("op01401").getIsSelectedVar()
            };
            model.addBoolAnd(cond1Literals).onlyEnforceIf(condition1);
            model.addBoolOr(new Literal[] {
                    P013.getParaOptionByCode("op01301").getIsSelectedVar().not(),
                    P014.getParaOptionByCode("op01401").getIsSelectedVar().not()
            }).onlyEnforceIf(condition1.not());

            BoolVar condition2 = model.newBoolVar("rule22_condition2");
            Literal[] cond2Literals = new Literal[] {
                    P013.getParaOptionByCode("op01302").getIsSelectedVar(),
                    P014.getParaOptionByCode("op01402").getIsSelectedVar()
            };
            model.addBoolAnd(cond2Literals).onlyEnforceIf(condition2);
            model.addBoolOr(new Literal[] {
                    P013.getParaOptionByCode("op01302").getIsSelectedVar().not(),
                    P014.getParaOptionByCode("op01402").getIsSelectedVar().not()
            }).onlyEnforceIf(condition2.not());

            model.addEquality((IntVar) PT002.qty, 1).onlyEnforceIf(condition1);
            model.addEquality((IntVar) PT002.qty, 2).onlyEnforceIf(condition2);
            // model.addEquality((IntVar)PT002.qty, 3).onlyEnforceIf(condition1.not(),
            // condition2.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if(P015.value==op01501 && P016.value==op01601) then PT003.qty=1 " +
                "else if(P015.value==op01502 && P016.value==op01602) then PT003.qty=2 else PT003.qty=3")
        protected void Rule23() {
            BoolVar condition1 = model.newBoolVar("rule23_condition1");
            Literal[] cond1Literals = new Literal[] {
                    P015.getParaOptionByCode("op01501").getIsSelectedVar(),
                    P016.getParaOptionByCode("op01601").getIsSelectedVar()
            };
            model.addBoolAnd(cond1Literals).onlyEnforceIf(condition1);
            model.addBoolOr(new Literal[] {
                    P015.getParaOptionByCode("op01501").getIsSelectedVar().not(),
                    P016.getParaOptionByCode("op01601").getIsSelectedVar().not()
            }).onlyEnforceIf(condition1.not());

            BoolVar condition2 = model.newBoolVar("rule23_condition2");
            Literal[] cond2Literals = new Literal[] {
                    P015.getParaOptionByCode("op01502").getIsSelectedVar(),
                    P016.getParaOptionByCode("op01602").getIsSelectedVar()
            };
            model.addBoolAnd(cond2Literals).onlyEnforceIf(condition2);
            model.addBoolOr(new Literal[] {
                    P015.getParaOptionByCode("op01502").getIsSelectedVar().not(),
                    P016.getParaOptionByCode("op01602").getIsSelectedVar().not()
            }).onlyEnforceIf(condition2.not());

            model.addEquality((IntVar) PT003.qty, 1).onlyEnforceIf(condition1);
            model.addEquality((IntVar) PT003.qty, 2).onlyEnforceIf(condition2);
            // model.addEquality((IntVar)PT003.qty, 3).onlyEnforceIf(condition1.not(),
            // condition2.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if(P017.value==op01701 && P018.value==op01801) then PT004.qty=1 " +
                "else if(P017.value==op01702 && P018.value==op01802) then PT004.qty=2 else PT004.qty=3")
        protected void Rule24() {
            BoolVar condition1 = model.newBoolVar("rule24_condition1");
            Literal[] cond1Literals = new Literal[] {
                    P017.getParaOptionByCode("op01701").getIsSelectedVar(),
                    P018.getParaOptionByCode("op01801").getIsSelectedVar()
            };
            model.addBoolAnd(cond1Literals).onlyEnforceIf(condition1);
            model.addBoolOr(new Literal[] {
                    P017.getParaOptionByCode("op01701").getIsSelectedVar().not(),
                    P018.getParaOptionByCode("op01801").getIsSelectedVar().not()
            }).onlyEnforceIf(condition1.not());

            BoolVar condition2 = model.newBoolVar("rule24_condition2");
            Literal[] cond2Literals = new Literal[] {
                    P017.getParaOptionByCode("op01702").getIsSelectedVar(),
                    P018.getParaOptionByCode("op01802").getIsSelectedVar()
            };
            model.addBoolAnd(cond2Literals).onlyEnforceIf(condition2);
            model.addBoolOr(new Literal[] {
                    P017.getParaOptionByCode("op01702").getIsSelectedVar().not(),
                    P018.getParaOptionByCode("op01802").getIsSelectedVar().not()
            }).onlyEnforceIf(condition2.not());

            model.addEquality((IntVar) PT004.qty, 1).onlyEnforceIf(condition1);
            model.addEquality((IntVar) PT004.qty, 2).onlyEnforceIf(condition2);
            // model.addEquality((IntVar)PT004.qty, 3).onlyEnforceIf(condition1.not(),
            // condition2.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if(P019.value==op01901 && P020.value==op02001) then PT005.qty=1 " +
                "else if(P019.value==op01902 && P020.value==op02002) then PT005.qty=2 else PT005.qty=3")
        protected void Rule25() {
            BoolVar condition1 = model.newBoolVar("rule25_condition1");
            Literal[] cond1Literals = new Literal[] {
                    P019.getParaOptionByCode("op01901").getIsSelectedVar(),
                    P020.getParaOptionByCode("op02001").getIsSelectedVar()
            };
            model.addBoolAnd(cond1Literals).onlyEnforceIf(condition1);
            model.addBoolOr(new Literal[] {
                    P019.getParaOptionByCode("op01901").getIsSelectedVar().not(),
                    P020.getParaOptionByCode("op02001").getIsSelectedVar().not()
            }).onlyEnforceIf(condition1.not());

            BoolVar condition2 = model.newBoolVar("rule25_condition2");
            Literal[] cond2Literals = new Literal[] {
                    P019.getParaOptionByCode("op01902").getIsSelectedVar(),
                    P020.getParaOptionByCode("op02002").getIsSelectedVar()
            };
            model.addBoolAnd(cond2Literals).onlyEnforceIf(condition2);
            model.addBoolOr(new Literal[] {
                    P019.getParaOptionByCode("op01902").getIsSelectedVar().not(),
                    P020.getParaOptionByCode("op02002").getIsSelectedVar().not()
            }).onlyEnforceIf(condition2.not());

            model.addEquality((IntVar) PT005.qty, 1).onlyEnforceIf(condition1);
            model.addEquality((IntVar) PT005.qty, 2).onlyEnforceIf(condition2);
            // model.addEquality((IntVar)PT005.qty, 3).onlyEnforceIf(condition1.not(),
            // condition2.not());
        }
    }
    // ---------------模型的定义end----------------------------------------

    public Perf20ParasTest() {
        super(Perf20ParasConstraint.class);
    }

    @Test
    public void testPerformancePT003_onlyOneSolution() {
        executeTest("testPerformancePT003_onlyOneSolution", false);
    }

    @Test
    public void testPerformancePT003_allSolutions() {
        executeTest("testPerformancePT003_allSolutions", true);
    }

    public void executeTest(String testCaseName, boolean tmpEnumerateAllSolution) {
        long totalTime = 0;
        int iterations = 10;
        this.setEnumerateAllSolution(tmpEnumerateAllSolution);
        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            inferParas("PT003", 2);
        }
        long endTime = System.nanoTime();
        totalTime += TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        long averageTime = totalTime / iterations;
        log.info("{} Average time for inferParas(PT003, 4): {} ms", testCaseName, averageTime);
        Assert.assertTrue(testCaseName + " Performance test failed: average time " + averageTime + " ms exceeds 300 ms",
                averageTime <= 300);
    }

    protected void beforeInitConfig(ConstraintConfig cfg) {
        cfg.setLoadType(1); // 全量加载
    }
}