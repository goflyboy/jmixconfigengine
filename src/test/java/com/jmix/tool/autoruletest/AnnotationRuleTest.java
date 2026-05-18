package com.jmix.tool.autoruletest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.jmix.executor.southinf.ModuleAlgBase;
import com.jmix.executor.southinf.var.ParaVar;
import com.jmix.executor.southinf.var.PartCategoryVar;
import com.jmix.executor.southinf.var.PartVar;
import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.logic.Rule;
import com.jmix.executor.bmodel.para.ParaType;
import com.jmix.executor.impl.util.CommHelper;
import com.jmix.tool.bbuilder.ModuleGenneratorByAnno;
import com.jmix.tool.bbuilder.anno.CodeRuleAnno;
import com.jmix.tool.bbuilder.anno.CompatiableRuleAnno;
import com.jmix.tool.bbuilder.anno.ModuleAnno;
import com.jmix.tool.bbuilder.anno.ParaAnno;
import com.jmix.tool.bbuilder.anno.PartAnno;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

/**
 * 娉ㄨВ瑙勫垯娴嬭瘯绫?
 * 鐢ㄤ簬娴嬭瘯CompatiableRuleAnno鍜孋odeRuleAnno娉ㄨВ鐨勫姛鑳?
 * 
 * @since 2025-09-23
 */
@Slf4j
/**
 * 娉ㄨВ瑙勫垯娴嬭瘯绾︽潫妯″瀷绫?
 * 
 * @since 2025-09-23
 */
@ModuleAnno(id = 123, code = "AnnotationRule", packageName = "com.jmix.configengine.scenario.ruletest", version = "1.0", description = "娉ㄨВ瑙勫垯娴嬭瘯妯″潡", sortNo = 1)
public class AnnotationRuleTest extends ModuleAlgBase {

    @ParaAnno(description = "棰滆壊鍙傛暟", type = ParaType.ENUM, options = { "Red", "Blue", "Green" })
    private ParaVar colorVar;

    @ParaAnno(description = "灏哄鍙傛暟", type = ParaType.ENUM, options = { "Small", "Medium", "Large" })
    private ParaVar sizeVar;

    @PartAnno(description = "閮ㄤ欢1", maxQuantity = 10)
    private PartVar part1Var;

    /**
     * 鍏煎鎬ц鍒欙細濡傛灉棰滆壊鏄孩鑹诧紝鍒欓儴浠?鐨勬暟閲忓繀椤绘槸1
     */
    @CompatiableRuleAnno(leftExprCode = "color.valueVar()==\"Red\"", operator = "Requires", rightExprCode = "part1.quantityVar()==1", normalNaturalCode = "濡傛灉棰滆壊鏄孩鑹诧紝鍒欓儴浠?鐨勬暟閲忓繀椤绘槸1")
    /**
     * 瑙勫垯1锛氬吋瀹规€ц鍒?
     */
    public void rule1() {
        // 鑷姩鐢熸垚鐨勭害鏉熶唬鐮佸皢鍦ㄨ繖閲屾敞鍏?
    }

    /**
     * 浠ｇ爜瑙勫垯锛氳嚜瀹氫箟閫昏緫
     */
    @CodeRuleAnno(code = "if (color.valueVar() == \"Blue\" && size.valueVar() == \"Large\") { return false; }", normalNaturalCode = "blue large is incompatible")
    public void rule2() {
        // 浠ｇ爜瑙勫垯涓嶉渶瑕佹敞鍏ヤ唬鐮?
    }

    /**
     * 鍒濆鍖栫害鏉?
     */
    @CodeRuleAnno
    private void initConstraint() {
        // 绾︽潫鍒濆鍖栭€昏緫
    }

    /**
     * 娴嬭瘯瑙勫垯鐢熸垚
     */
    @Test
    @Disabled
    public void testRuleGeneration() {
        // 鍒涘缓涓存椂璺緞
        String tempPath = CommHelper.createTempPath(AnnotationRuleTest.class);

        // 鐢熸垚Module
        Module module = ModuleGenneratorByAnno.build(AnnotationRuleTest.class, tempPath);

        // 楠岃瘉Module涓寘鍚簡鐢熸垚鐨勮鍒?
        assertNotNull(module, "Module should not be null");
        assertNotNull(module.getRules(), "Module rules should not be null");
        assertEquals(2, module.getRules().size(), "Should have 2 rules");

        // 楠岃瘉绗竴涓鍒欐槸鍏煎鎬ц鍒?
        Rule rule1 = module.getRules().get(0);
        assertEquals(rule1.getCode(), "rule1", "First rule should be rule1");
        assertEquals(rule1.getRuleSchemaTypeFullName(),
                "CDSL.V5.Struct.CompatiableRule", "First rule should be CompatiableRule");
        // 楠岃瘉绗簩涓鍒欐槸浠ｇ爜瑙勫垯
        Rule rule2 = module.getRules().get(1);
        assertEquals(rule2.getCode(), "rule2", "Second rule should be rule2");
        assertEquals(rule2.getRuleSchemaTypeFullName(), "CDSL.V5.Struct.CodeRule", "Second rule should be CodeRule");

        // 楠岃瘉鍙傛暟鍜岄儴浠?
        assertNotNull(module.getParas(), "Module paras should not be null");
        assertEquals(2, module.getParas().size(), "Should have 2 paras");

        assertNotNull(module.getAllParts(), "Module parts should not be null");
        assertEquals(1, module.getAllParts().size(), "Should have 1 part");

        log.info("鉁?Rule generation test passed");
        log.info("  Generated rule count: {}", module.getRules().size());
        log.info("  Generated parameter count: {}", module.getParas().size());
        log.info("  Generated part count: {}", module.getAllParts().size());
    }

    /**
     * 娴嬭瘯娉ㄨВ瑙ｆ瀽
     */
    @Test
    @Disabled
    public void testAnnotationParsing() {
        // 娴嬭瘯CompatiableRuleAnno娉ㄨВ瑙ｆ瀽
        try {
            Method rule1Method = AnnotationRuleTest.class.getMethod("rule1");
            CompatiableRuleAnno compatiableRuleAnno = rule1Method.getAnnotation(CompatiableRuleAnno.class);

            assertNotNull(compatiableRuleAnno, "CompatiableRuleAnno should not be null");
            assertEquals(compatiableRuleAnno.leftExprCode(), "Color.valueVar()==\"Red\"", "Left expression should match");
            assertEquals(compatiableRuleAnno.operator(), "Requires", "Operator should match");
            assertEquals(compatiableRuleAnno.rightExprCode(), "Part1.quantityVar()==1", "Right expression should match");

            log.info("鉁?CompatiableRuleAnno annotation parsing test passed");
        } catch (NoSuchMethodException e) {
            fail("Method rule1 not found: " + e.getMessage());
        }

        // 娴嬭瘯CodeRuleAnno娉ㄨВ瑙ｆ瀽
        try {
            Method rule2Method = AnnotationRuleTest.class.getMethod("rule2");
            CodeRuleAnno codeRuleAnno = rule2Method.getAnnotation(CodeRuleAnno.class);

            assertNotNull(codeRuleAnno, "CodeRuleAnno should not be null");
            assertTrue(codeRuleAnno.code().contains("Color.valueVar()"), "Code should contain Color.valueVar()");
            assertTrue(codeRuleAnno.code().contains("Size.valueVar()"), "Code should contain Size.valueVar()");

            log.info("鉁?CodeRuleAnno annotation parsing test passed");
        } catch (NoSuchMethodException e) {
            fail("Method rule2 not found: " + e.getMessage());
        }
    }
}
