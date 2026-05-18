package com.jmix.scenario.tshirt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jmix.executor.bmodel.Module;
import com.jmix.executor.impl.util.ModuleUtils;
import com.jmix.executor.model.AlgLoaderException;
import com.jmix.tool.artbuilder.ModuleAlgArtifactGenerator;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * ModuleAlgArtifactGenerator.buildConstraintRule 鏂规硶娴嬭瘯绫?
 * 
 * 娴嬭瘯鐩爣锛?
 * 1. 鏍规嵁T鎭よ～妯″潡鏍蜂緥鏁版嵁鐢熸垚Module鏁版嵁
 * 2. 璋冪敤ModuleAlgArtifactGenerator.buildConstraintRule鐢熸垚绾︽潫瑙勫垯浠ｇ爜
 * 3. 楠岃瘉鐢熸垚鐨勪唬鐮佹枃浠舵槸鍚︽纭?
 * 
 * @since 2025-09-23
 */
@Slf4j
public class ModuleAlgArtifactGeneratorBaseTest {

    private ModuleAlgArtifactGenerator generator;

    private Module tshirtModule;

    private String outputPath;

    // 鏄惁鍒犻櫎鐢熸垚鐨勬枃浠?
    private boolean deleteGeneratedFile = true;

    /**
     * 娴嬭瘯璁剧疆鏂规硶
     * 
     * @throws Exception 寮傚父
     */
    @BeforeEach
    public void setUp() throws Exception {
        // 鍒濆鍖栫敓鎴愬櫒
        generator = new ModuleAlgArtifactGenerator();

        // 璁剧疆杈撳嚭璺緞涓哄綋鍓嶅寘鎵€鍦ㄧ洰褰?
        outputPath = getCurrentPackagePath() + "/TShirtConstraint.java";

        // 鍒涘缓T鎭よ～妯″潡鏁版嵁锛堢畝鍖栫増鏈紝閬垮厤JSON鍙嶅簭鍒楀寲闂锛?
        // tshirtModule = createSimpleTShirtModule();
        String jsonFilePath = "src/test/java/com/jmix/configengine/scenario/tshirt/tshirtdata.json";
        // 浣跨敤ModuleUtils.fromJsonFile鏂规硶璇诲彇JSON鏂囦欢
        tshirtModule = ModuleUtils.fromJsonFile(jsonFilePath);

        // 鍒濆鍖栨ā鍧?
        tshirtModule.init();
    }

    /**
     * 娴嬭瘯buildConstraintRule鏂规硶
     * 
     * @throws Exception 寮傚父
     */
    @Test
    @Disabled
    public void testBuildConstraintRule() throws Exception {
        deleteGeneratedFile = false;
        log.info("=== Starting test for buildConstraintRule method ===");
        log.info("Output path: {}", outputPath);

        // 楠岃瘉妯″潡鏁版嵁鏄惁姝ｇ‘鍒涘缓
        assertNotNull(tshirtModule, "T鎭よ～妯″潡搴旇鍒涘缓鎴愬姛");
        assertEquals("妯″潡浠ｇ爜搴旇鏄疶Shirt", "TShirt", tshirtModule.getCode());
        assertNotNull(tshirtModule.getParas(), "paras should not be null");
        assertNotNull(tshirtModule.getAllParts(), "parts should not be null");
        assertNotNull(tshirtModule.getRules(), "rules should not be null");

        log.info("鉁?Module data validation passed");
        log.info("  Parameter count: {}", tshirtModule.getParas().size());
        log.info("  Part count: {}", tshirtModule.getAllParts().size());
        log.info("  Rule count: {}", tshirtModule.getRules().size());

        // 璋冪敤buildConstraintRule鏂规硶
        log.info("\n=== Calling buildConstraintRule method ===");

        // 鐢变簬妯℃澘闇€瑕乺ule.left鍜宺ule.right灞炴€э紝鎴戜滑闇€瑕佸厛鎵嬪姩鏋勫缓杩欎簺淇℃伅
        // 鎴栬€呬慨鏀规ā鏉挎潵澶勭悊杩欑鎯呭喌
        try {
            generator.buildConstraintRule(tshirtModule, outputPath);
        } catch (Exception e) {
            log.error("buildConstraintRule call failed, error message: {}", e.getMessage());
            // 杩欐槸涓€涓凡鐭ラ棶棰橈紝妯℃澘闇€瑕乺ule.left鍜宺ule.right灞炴€?
            // 浣嗗綋鍓嶇殑瀹炵幇娌℃湁姝ｇ‘璁剧疆杩欎簺灞炴€?
            throw new AlgLoaderException("buildConstraintRule call failed, error message: " + e.getMessage());
        }

        // 楠岃瘉鐢熸垚鐨勪唬鐮佹枃浠舵槸鍚﹀瓨鍦?
        File generatedFile = new File(outputPath);
        assertTrue(generatedFile.exists(), "generated file should exist");
        assertTrue(generatedFile.canRead(), "generated file should be readable");

        log.info("鉁?Constraint code file generated successfully");
        log.info("  File path: {}", generatedFile.getAbsolutePath());
        log.info("  File size: {} bytes", generatedFile.length());

        // 楠岃瘉鐢熸垚鐨勪唬鐮佸唴瀹?
        validateGeneratedCode();

        log.info("\n=== Test completed ===");
    }

    /**
     * 楠岃瘉鐢熸垚鐨勪唬鐮佸唴瀹?
     */
    private void validateGeneratedCode() throws Exception {
        Path path = Paths.get(outputPath);
        String content = new String(Files.readAllBytes(path));

        // 楠岃瘉鍩烘湰缁撴瀯
        assertTrue(content.contains("public class TShirtConstraint"), "鐢熸垚鐨勪唬鐮佸簲璇ュ寘鍚被瀹氫箟");
        assertTrue(content.contains("extends ModuleAlgBase"), "Generated code should extend ModuleAlgBase");

        // 楠岃瘉鍙傛暟鍙橀噺
        assertTrue(content.contains("private ParaVar colorVar"), "搴旇鍖呭惈Color鍙傛暟鍙橀噺");
        assertTrue(content.contains("private ParaVar sizeVar"), "搴旇鍖呭惈Size鍙傛暟鍙橀噺");

        // 楠岃瘉閮ㄤ欢鍙橀噺
        assertTrue(content.contains("private PartVar tShirt11Var"), "搴旇鍖呭惈TShirt11閮ㄤ欢鍙橀噺");
        assertTrue(content.contains("private PartVar tShirt12Var"), "搴旇鍖呭惈TShirt12閮ㄤ欢鍙橀噺");

        // 楠岃瘉鏂规硶
        assertTrue(content.contains("import com.jmix.executor.southinf.ModuleAlgBase"),
                "Should import southbound base class");

        // 楠岃瘉绾︽潫瑙勫垯鏂规硶
        assertTrue(content.contains("addConstraint_rule1"), "should contain rule1 constraint method");
        assertTrue(content.contains("addConstraint_rule2"), "should contain rule2 constraint method");
        assertTrue(content.contains("addConstraint_rule3"), "should contain rule3 constraint method");

        log.info("鉁?Generated code content validation passed");
        log.info("  Contains correct class definition");
        log.info("  Contains all parameter variables");
        log.info("  Contains all part variables");
        log.info("  Contains all constraint rule methods");
    }

    private String getCurrentPackagePath() {
        // 鑾峰彇褰撳墠绫荤殑鍖呰矾寰?
        String packagePath = this.getClass().getPackage().getName().replace('.', File.separatorChar);

        // 鏋勫缓瀹屾暣鐨勭洰褰曡矾寰?
        String baseDir = "src/test/java";
        String fullPath = baseDir + File.separator + packagePath;

        // 纭繚鐩綍瀛樺湪
        File dir = new File(fullPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        return fullPath;
    }

    /**
     * 娴嬭瘯娓呯悊鏂规硶
     */
    @AfterEach
    public void tearDown() {
        // 娓呯悊鐢熸垚鐨勬祴璇曟枃浠?
        if (!deleteGeneratedFile) {
            return;
        }

        try {
            File generatedFile = new File(outputPath);
            if (generatedFile.exists()) {
                generatedFile.delete();
                log.info("Cleaning up test file: {}", outputPath);
            }
        } catch (Exception e) {
            log.error("Failed to clean up test file: {}", e.getMessage());
        }
    }
}
