package com.jmix.scenario.perf20;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.var.ParaVar;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.coretest.ModuleScenarioTestBase;
import com.jmix.executor.model.ConstraintConfig;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.ParaAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

import com.jmix.executor.southinf.cp.AlgCPBoolVar;
import com.jmix.executor.southinf.cp.AlgCPLiteral;

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
    private static class Perf20ParasConstraint extends ModuleAlgBase {
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

        @CodeRuleAnno(normalNaturalCode = "if p001.valueVar() == op00102 then p002.valueVar() != op00202")
        private void rule01() {
            AlgCPBoolVar condition = model().newBoolVar("rule01_condition");
            model().addEquality(p001.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p002.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p001.valueVar(), 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p002.valueVar() == op00202 then p003.valueVar() != op00302")
        private void rule02() {
            AlgCPBoolVar condition = model().newBoolVar("rule02_condition");
            model().addEquality(p002.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p003.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p002.valueVar(), 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p003.valueVar() == op00302 then p004.valueVar() != op00402")
        private void rule03() {
            AlgCPBoolVar condition = model().newBoolVar("rule03_condition");
            model().addEquality(p003.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p004.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p003.valueVar(), 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p004.valueVar() == op00402 then p005.valueVar() != op00502")
        private void rule04() {
            AlgCPBoolVar condition = model().newBoolVar("rule04_condition");
            model().addEquality(p004.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p005.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p004.valueVar(), 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p005.valueVar() == op00502 then p006.valueVar() != op00602")
        private void rule05() {
            AlgCPBoolVar condition = model().newBoolVar("rule05_condition");
            model().addEquality(p005.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p006.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p005.valueVar(), 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p006.valueVar() == op00602 then p007.valueVar() != op00702")
        private void rule06() {
            AlgCPBoolVar condition = model().newBoolVar("rule06_condition");
            model().addEquality(p006.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p007.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p006.valueVar(), 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p007.valueVar() == op00702 then p008.valueVar() != op00802")
        private void rule07() {
            AlgCPBoolVar condition = model().newBoolVar("rule07_condition");
            model().addEquality(p007.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p008.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p007.valueVar(), 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p008.valueVar() == op00802 then p009.valueVar() != op00902")
        private void rule08() {
            AlgCPBoolVar condition = model().newBoolVar("rule08_condition");
            model().addEquality(p008.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p009.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p008.valueVar(), 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p009.valueVar() == op00902 then p010.valueVar() != op01002")
        private void rule09() {
            AlgCPBoolVar condition = model().newBoolVar("rule09_condition");
            model().addEquality(p009.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p010.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p009.valueVar(), 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p010.valueVar() == op01002 then p011.valueVar() != op01102")
        private void rule10() {
            AlgCPBoolVar condition = model().newBoolVar("rule10_condition");
            model().addEquality(p010.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p011.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p010.valueVar(), 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p011.valueVar() == op01102 then p012.valueVar() != op01202")
        private void rule11() {
            AlgCPBoolVar condition = model().newBoolVar("rule11_condition");
            model().addEquality(p011.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p012.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p011.valueVar(), 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p012.valueVar() == op01202 then p013.valueVar() != op01302")
        private void rule12() {
            AlgCPBoolVar condition = model().newBoolVar("rule12_condition");
            model().addEquality(p012.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p013.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p012.valueVar(), 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p013.valueVar() == op01302 then p014.valueVar() != op01402")
        private void rule13() {
            AlgCPBoolVar condition = model().newBoolVar("rule13_condition");
            model().addEquality(p013.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p014.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p013.valueVar(), 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p014.valueVar() == op01402 then p015.valueVar() != op01502")
        private void rule14() {
            AlgCPBoolVar condition = model().newBoolVar("rule14_condition");
            model().addEquality(p014.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p015.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p014.valueVar(), 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p015.valueVar() == op01502 then p016.valueVar() != op01602")
        private void rule15() {
            AlgCPBoolVar condition = model().newBoolVar("rule15_condition");
            model().addEquality(p015.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p016.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p015.valueVar(), 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p016.valueVar() == op01602 then p017.valueVar() != op01702")
        private void rule16() {
            AlgCPBoolVar condition = model().newBoolVar("rule16_condition");
            model().addEquality(p016.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p017.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p016.valueVar(), 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p017.valueVar() == op01702 then p018.valueVar() != op01802")
        private void rule17() {
            AlgCPBoolVar condition = model().newBoolVar("rule17_condition");
            model().addEquality(p017.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p018.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p017.valueVar(), 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p018.valueVar() == op01802 then p019.valueVar() != op01902")
        private void rule18() {
            AlgCPBoolVar condition = model().newBoolVar("rule18_condition");
            model().addEquality(p018.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p019.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p018.valueVar(), 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p019.valueVar() == op01902 then p020.valueVar() != op02002")
        private void rule19() {
            AlgCPBoolVar condition = model().newBoolVar("rule19_condition");
            model().addEquality(p019.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p020.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p019.valueVar(), 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if p020.valueVar() == op02002 then p001.valueVar() != op00102")
        private void rule20() {
            AlgCPBoolVar condition = model().newBoolVar("rule20_condition");
            model().addEquality(p020.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p001.valueVar(), 2).onlyEnforceIf(condition);
            model().addDifferent(p020.valueVar(), 2).onlyEnforceIf(condition.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if(p011.valueVar()==op01101 && p012.valueVar()==op01201) then pt001.quantityVar()=1 "
                + "else if(p011.valueVar()==op01102 && p012.valueVar()==op01202) then pt001.quantityVar()=2 else pt001.quantityVar()=3")
        private void rule21() {
            AlgCPBoolVar condition1 = model().newBoolVar("rule21_condition1");
            AlgCPLiteral[] cond1Literals = new AlgCPLiteral[] {
                    p011.option("op01101").selectedVar(),
                    p012.option("op01201").selectedVar()
            };
            model().addBoolAnd(cond1Literals).onlyEnforceIf(condition1);
            model().addBoolOr(new AlgCPLiteral[] {
                    p011.option("op01101").selectedVar().not(),
                    p012.option("op01201").selectedVar().not()
            }).onlyEnforceIf(condition1.not());

            AlgCPBoolVar condition2 = model().newBoolVar("rule21_condition2");
            AlgCPLiteral[] cond2Literals = new AlgCPLiteral[] {
                    p011.option("op01102").selectedVar(),
                    p012.option("op01202").selectedVar()
            };
            model().addBoolAnd(cond2Literals).onlyEnforceIf(condition2);
            model().addBoolOr(new AlgCPLiteral[] {
                    p011.option("op01102").selectedVar().not(),
                    p012.option("op01202").selectedVar().not()
            }).onlyEnforceIf(condition2.not());

            model().addEquality(pt001.quantityVar(), 1).onlyEnforceIf(condition1);
            model().addEquality(pt001.quantityVar(), 2).onlyEnforceIf(condition2);
        }

        @CodeRuleAnno(normalNaturalCode = "if(p013.valueVar()==op01301 && p014.valueVar()==op01401) then pt002.quantityVar()=1 "
                + "else if(p013.valueVar()==op01302 && p014.valueVar()==op01402) then pt002.quantityVar()=2 else pt002.quantityVar()=3")
        private void rule22() {
            AlgCPBoolVar condition1 = model().newBoolVar("rule22_condition1");
            AlgCPLiteral[] cond1Literals = new AlgCPLiteral[] {
                    p013.option("op01301").selectedVar(),
                    p014.option("op01401").selectedVar()
            };
            model().addBoolAnd(cond1Literals).onlyEnforceIf(condition1);
            model().addBoolOr(new AlgCPLiteral[] {
                    p013.option("op01301").selectedVar().not(),
                    p014.option("op01401").selectedVar().not()
            }).onlyEnforceIf(condition1.not());

            AlgCPBoolVar condition2 = model().newBoolVar("rule22_condition2");
            AlgCPLiteral[] cond2Literals = new AlgCPLiteral[] {
                    p013.option("op01302").selectedVar(),
                    p014.option("op01402").selectedVar()
            };
            model().addBoolAnd(cond2Literals).onlyEnforceIf(condition2);
            model().addBoolOr(new AlgCPLiteral[] {
                    p013.option("op01302").selectedVar().not(),
                    p014.option("op01402").selectedVar().not()
            }).onlyEnforceIf(condition2.not());

            model().addEquality(pt002.quantityVar(), 1).onlyEnforceIf(condition1);
            model().addEquality(pt002.quantityVar(), 2).onlyEnforceIf(condition2);
        }

        @CodeRuleAnno(normalNaturalCode = "if(p015.valueVar()==op01501 && p016.valueVar()==op01601) then pt003.quantityVar()=1 "
                + "else if(p015.valueVar()==op01502 && p016.valueVar()==op01602) then pt003.quantityVar()=2 else pt003.quantityVar()=3")
        private void rule23() {
            AlgCPBoolVar condition1 = model().newBoolVar("rule23_condition1");
            AlgCPLiteral[] cond1Literals = new AlgCPLiteral[] {
                    p015.option("op01501").selectedVar(),
                    p016.option("op01601").selectedVar()
            };
            model().addBoolAnd(cond1Literals).onlyEnforceIf(condition1);
            model().addBoolOr(new AlgCPLiteral[] {
                    p015.option("op01501").selectedVar().not(),
                    p016.option("op01601").selectedVar().not()
            }).onlyEnforceIf(condition1.not());

            AlgCPBoolVar condition2 = model().newBoolVar("rule23_condition2");
            AlgCPLiteral[] cond2Literals = new AlgCPLiteral[] {
                    p015.option("op01502").selectedVar(),
                    p016.option("op01602").selectedVar()
            };
            model().addBoolAnd(cond2Literals).onlyEnforceIf(condition2);
            model().addBoolOr(new AlgCPLiteral[] {
                    p015.option("op01502").selectedVar().not(),
                    p016.option("op01602").selectedVar().not()
            }).onlyEnforceIf(condition2.not());

            model().addEquality(pt003.quantityVar(), 1).onlyEnforceIf(condition1);
            model().addEquality(pt003.quantityVar(), 2).onlyEnforceIf(condition2);
        }

        @CodeRuleAnno(normalNaturalCode = "if(p017.valueVar()==op01701 && p018.valueVar()==op01801) then pt004.quantityVar()=1 "
                + "else if(p017.valueVar()==op01702 && p018.valueVar()==op01802) then pt004.quantityVar()=2 else pt004.quantityVar()=3")
        private void rule24() {
            AlgCPBoolVar condition1 = model().newBoolVar("rule24_condition1");
            AlgCPLiteral[] cond1Literals = new AlgCPLiteral[] {
                    p017.option("op01701").selectedVar(),
                    p018.option("op01801").selectedVar()
            };
            model().addBoolAnd(cond1Literals).onlyEnforceIf(condition1);
            model().addBoolOr(new AlgCPLiteral[] {
                    p017.option("op01701").selectedVar().not(),
                    p018.option("op01801").selectedVar().not()
            }).onlyEnforceIf(condition1.not());

            AlgCPBoolVar condition2 = model().newBoolVar("rule24_condition2");
            AlgCPLiteral[] cond2Literals = new AlgCPLiteral[] {
                    p017.option("op01702").selectedVar(),
                    p018.option("op01802").selectedVar()
            };
            model().addBoolAnd(cond2Literals).onlyEnforceIf(condition2);
            model().addBoolOr(new AlgCPLiteral[] {
                    p017.option("op01702").selectedVar().not(),
                    p018.option("op01802").selectedVar().not()
            }).onlyEnforceIf(condition2.not());

            model().addEquality(pt004.quantityVar(), 1).onlyEnforceIf(condition1);
            model().addEquality(pt004.quantityVar(), 2).onlyEnforceIf(condition2);
            // model().addEquality((AlgCPIntVar)pt004.quantityVar(), 3).onlyEnforceIf(condition1.not(),
            // condition2.not());
        }

        @CodeRuleAnno(normalNaturalCode = "if(p019.valueVar()==op01901 && p020.valueVar()==op02001) then pt005.quantityVar()=1 "
                + "else if(p019.valueVar()==op01902 && p020.valueVar()==op02002) then pt005.quantityVar()=2 else pt005.quantityVar()=3")
        private void rule25() {
            AlgCPBoolVar condition1 = model().newBoolVar("rule25_condition1");
            AlgCPLiteral[] cond1Literals = new AlgCPLiteral[] {
                    p019.option("op01901").selectedVar(),
                    p020.option("op02001").selectedVar()
            };
            model().addBoolAnd(cond1Literals).onlyEnforceIf(condition1);
            model().addBoolOr(new AlgCPLiteral[] {
                    p019.option("op01901").selectedVar().not(),
                    p020.option("op02001").selectedVar().not()
            }).onlyEnforceIf(condition1.not());

            AlgCPBoolVar condition2 = model().newBoolVar("rule25_condition2");
            AlgCPLiteral[] cond2Literals = new AlgCPLiteral[] {
                    p019.option("op01902").selectedVar(),
                    p020.option("op02002").selectedVar()
            };
            model().addBoolAnd(cond2Literals).onlyEnforceIf(condition2);
            model().addBoolOr(new AlgCPLiteral[] {
                    p019.option("op01902").selectedVar().not(),
                    p020.option("op02002").selectedVar().not()
            }).onlyEnforceIf(condition2.not());

            model().addEquality(pt005.quantityVar(), 1).onlyEnforceIf(condition1);
            model().addEquality(pt005.quantityVar(), 2).onlyEnforceIf(condition2);
            // model().addEquality((AlgCPIntVar)pt005.quantityVar(), 3).onlyEnforceIf(condition1.not(),
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