package com.jmix.configengine.scenario.ruletest;
import com.google.ortools.sat.*;
import com.jmix.configengine.artifact.*;
import com.jmix.configengine.model.*;
import com.jmix.configengine.scenario.base.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.*;
@Slf4j
public class ParaIntegerTest extends ModuleSecnarioTestBase {
    
    //---------------ģ�͵Ķ���start----------------------------------------
    @ModuleAnno(id = 123L)
    static public class ParaIntegerConstraint extends ConstraintAlgImpl {
        @ParaAnno(  
            type = ParaType.INTEGER,
            defaultValue = "0",
            minValue = "0",
            maxValue = "50"
        )
        private ParaVar P1;
        @ParaAnno(  
            type = ParaType.INTEGER,
            defaultValue = "0",
            minValue = "0",
            maxValue = "50"
        )
        private ParaVar P2;
		

        @PartAnno(
			maxQuantity=3
		)
        private PartVar Part1;
        
        @Override
        protected void initConstraint() {
            // Part1.quantity = P1.value + P2.value
            model.addEquality((IntVar)Part1.var, LinearExpr.sum(new IntVar[]{(IntVar)P1.var, (IntVar)P2.var}));
        }
    }
    //---------------ģ�͵Ķ���end----------------------------------------

    public ParaIntegerTest() {
        super(ParaIntegerConstraint.class);
    }

    @Test
    public void testMultipleSolutions() {
        inferParas("Part1", 3);
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(4);
        assertSolutionNum("P1:0,P2:3", 1);
        assertSolutionNum("P1:1,P2:2", 1);
        assertSolutionNum("P1:2,P2:1", 1);
        assertSolutionNum("P1:3,P2:0", 1);
    }

    @Test
    public void testNoSolution() {
        inferParas("Part1", 4);
        resultAssert().assertNoSolution();
    }

    @Test
    public void testParaDrivenInference() {
        inferParasByPara("P1", "2","P2", "1"); 
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);
        solutions(0)
                .assertPart("Part1").quantityEqual(3);
    }

    @Test
    public void testZeroQuantity() {
        inferParas("Part1", 0);
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);
        solutions(0)
                .assertPara("P1").valueEqual("0")
                .assertPara("P2").valueEqual("0");
    }

    @Test
    public void testMultipleParaInference() {
        // 使用可变参数版本：inferParasByPara(String paraCode1, String value1, String paraCode2, String value2, ...)
        inferParasByPara("P1", "2", "P2", "1");
        resultAssert()
                .assertSuccess()
                .assertSolutionSizeEqual(1);
        solutions(0)
                .assertPart("Part1").quantityEqual(3);
    }
}