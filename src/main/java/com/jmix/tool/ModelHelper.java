package com.jmix.tool;

import com.jmix.executor.bmodel.Module;
import com.jmix.executor.bmodel.logic.Rule;
import com.jmix.executor.impl.util.CommHelper;
import com.jmix.executor.model.AlgLoaderException;
import com.jmix.tool.artbuilder.ModuleAlgArtifactGenerator;
import com.jmix.tool.artbuilder.impl.ModuleVarInfo;
import com.jmix.tool.bbuilder.ModuleGenneratorByAnno;
import com.jmix.tool.impl.ModelGenneratorException;
import com.jmix.tool.impl.ModuleCompiler;
import com.jmix.tool.impl.ModuleGenerator;
import com.jmix.tool.impl.ModuleRunner;
import com.jmix.tool.impl.StructCodeInjector;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 妯″瀷鏂囦欢鐢熸垚鍣?
 * 鐢ㄤ簬鏍规嵁鐢ㄦ埛杈撳叆鐢熸垚娴嬭瘯浠ｇ爜鏂囦欢
 * 
 * @since 2025-09-22
 */
@Slf4j
public class ModelHelper {

    /**
     * 鐢熸垚ModelFile鏂囦欢锛堜娇鐢ㄩ粯璁ゅ寘鍚嶏級
     * 
     * @param modelScenarioName     妯″瀷鍦烘櫙鍚嶇О
     * @param userVariableModel     鐢ㄦ埛鍙橀噺妯″瀷
     * @param userLogicByPseudocode 鐢ㄦ埛閫昏緫浼唬鐮?
     */
    public void generatorModelFile(String modelScenarioName, String userVariableModel, String userLogicByPseudocode) {
        generatorModelFile("com.jmix.configengine.scenario.ruletest", modelScenarioName, userVariableModel,
                userLogicByPseudocode, "");
    }

    /**
     * 鐢熸垚ModelFile鏂囦欢
     * 
     * @param packageName           鍖呭悕
     * @param modelScenarioName     妯″瀷鍦烘櫙鍚嶇О
     * @param userVariableModel     鐢ㄦ埛鍙橀噺妯″瀷
     * @param userLogicByPseudocode 鐢ㄦ埛閫昏緫浼唬鐮?
     */
    public void generatorModelFile(String packageName, String modelScenarioName, String userVariableModel,
            String userLogicByPseudocode) {
        generatorModelFile(packageName, modelScenarioName, userVariableModel, userLogicByPseudocode, "");
    }

    /**
     * 鐢熸垚ModelFile鏂囦欢锛堝寘鍚敤鎴锋祴璇曠敤渚嬬壒娈婅鏍硷級
     * 
     * @param packageName           鍖呭悕
     * @param modelScenarioName     妯″瀷鍦烘櫙鍚嶇О
     * @param userVariableModel     鐢ㄦ埛鍙橀噺妯″瀷
     * @param userLogicByPseudocode 鐢ㄦ埛閫昏緫浼唬鐮?
     * @param userTestCaseSpec      鐢ㄦ埛娴嬭瘯鐢ㄤ緥鐗规畩瑙勬牸
     */
    public void generatorModelFile(String packageName, String modelScenarioName, String userVariableModel,
            String userLogicByPseudocode, String userTestCaseSpec) {
        try {
            // 璋冪敤澶фā鍨嬶紙濡?deepseek,Qwen锛夌殑娣卞害鎬濊€冩ā寮?
            ModuleGenerator moduleGenerator = new ModuleGenerator();
            String code = moduleGenerator.generatorModelCode(packageName, modelScenarioName, userVariableModel,
                    userLogicByPseudocode, userTestCaseSpec);

            // 鏍规嵁code鐢熸垚绫?{modelScenarioName}Test.java
            String className = modelScenarioName + "Test";
            String fileName = className + ".java";

            // 绫绘枃浠朵繚瀛樺湪褰撳墠宸ョ▼鐨勬祴璇曚唬鐮?+ packageName
            String targetPath = getTestSourcePath(packageName);
            Path fullPath = Paths.get(targetPath, fileName);

            // 纭繚鐩綍瀛樺湪
            Files.createDirectories(fullPath.getParent());

            // 濡傛灉fullPath瀛樺湪锛屽垯鍒犻櫎
            if (Files.exists(fullPath)) {
                Files.delete(fullPath);
            }

            // 鍐欏叆鏂囦欢锛屼娇鐢║TF-8缂栫爜
            try (OutputStreamWriter writer = new OutputStreamWriter(
                    new FileOutputStream(fullPath.toFile()), "UTF-8")) {
                writer.write(code);
            }

            log.info("Successfully generated test file: {}", fullPath.toAbsolutePath());

            // 灏濊瘯缂栬瘧鐢熸垚鐨凧ava鏂囦欢
            String projectRoot = System.getProperty("user.dir");

            ModuleCompiler compiler = new ModuleCompiler();
            compiler.compile(projectRoot, fullPath.toString());
            log.info("Successfully compiled test file: {}", fullPath.toAbsolutePath());

            // // 鏂板浠ｇ爜锛氳嚜鍔ㄦ敞鍏ョ害鏉熶唬鐮?
            // boolean isNeedInject = autoInjectConstraintCode(className, packageName);
            // if (isNeedInject) {
            // compileJavaFile(fullPath); // 鍐嶆缂栬瘧杩欎釜鏂囦欢
            // }

        } catch (Exception e) {
            throw new ModelGenneratorException("Failed to generate model file: " + e.getMessage(),
                    e);
        }
    }

    /**
     * 鑾峰彇娴嬭瘯婧愮爜璺緞
     * 
     * @param packageName 鍖呭悕
     * @return 娴嬭瘯婧愮爜璺緞
     */
    private String getTestSourcePath(String packageName) {
        // 鑾峰彇褰撳墠宸ヤ綔鐩綍
        String currentDir = ""; // System.getProperty("user.dir"); 澶歁odule鐨勬儏鍐典笅锛岃矾寰勫彇鐨勬槸璺焢roject鐨勫湴鍧€
        String classPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        currentDir = classPath.substring(1, classPath.indexOf("/target"));
        currentDir = currentDir.replace('/', File.separatorChar);
        // 鏋勫缓娴嬭瘯婧愮爜璺緞
        String packagePath = packageName.replace('.', File.separatorChar);
        return currentDir + File.separator + "src" + File.separator + "test" + File.separator + "java" + File.separator
                + packagePath;
    }

    /**
     * 鐢熸垚ModelFile鏂囦欢锛屽苟杩愯锛堜娇鐢ㄩ粯璁ゅ寘鍚嶏級
     * 
     * @param modelScenarioName     妯″瀷鍦烘櫙鍚嶇О
     * @param userVariableModel     鐢ㄦ埛鍙橀噺妯″瀷
     * @param userLogicByPseudocode 鐢ㄦ埛閫昏緫浼唬鐮?
     */
    public void generatorRunModelFile(String modelScenarioName, String userVariableModel,
            String userLogicByPseudocode) {
        generatorRunModelFile("com.jmix.configengine.scenario.ruletest", modelScenarioName, userVariableModel,
                userLogicByPseudocode, "");
    }

    /**
     * 鐢熸垚ModelFile鏂囦欢锛屽苟杩愯
     * 
     * @param packageName           鍖呭悕
     * @param modelScenarioName     妯″瀷鍦烘櫙鍚嶇О
     * @param userVariableModel     鐢ㄦ埛鍙橀噺妯″瀷
     * @param userLogicByPseudocode 鐢ㄦ埛閫昏緫浼唬鐮?
     */
    public void generatorRunModelFile(String packageName, String modelScenarioName,
            String userVariableModel, String userLogicByPseudocode) {
        generatorRunModelFile(packageName, modelScenarioName, userVariableModel, userLogicByPseudocode, "");
    }

    /**
     * 鐢熸垚ModelFile鏂囦欢锛屽苟杩愯锛堝寘鍚敤鎴锋祴璇曠敤渚嬬壒娈婅鏍硷級
     * 
     * @param packageName           鍖呭悕
     * @param modelScenarioName     妯″瀷鍦烘櫙鍚嶇О
     * @param userVariableModel     鐢ㄦ埛鍙橀噺妯″瀷
     * @param userLogicByPseudocode 鐢ㄦ埛閫昏緫浼唬鐮?
     * @param userTestCaseSpec      鐢ㄦ埛娴嬭瘯鐢ㄤ緥鐗规畩瑙勬牸
     */
    public void generatorRunModelFile(String packageName, String modelScenarioName,
            String userVariableModel, String userLogicByPseudocode, String userTestCaseSpec) {
        try {
            // 璋冪敤generatorModelFile鐢熸垚鏂囦欢锛堟槸涓€涓祴璇曠敤渚嬶級锛屽锛欳alculateRuleSimpleTest.java
            generatorModelFile(packageName, modelScenarioName, userVariableModel, userLogicByPseudocode,
                    userTestCaseSpec);

            // 浣跨敤 ModuleRunner 杩愯杩欎釜鏂囦欢锛堣繍琛岃繖涓祴璇曠被锛?
            ModuleRunner moduleRunner = new ModuleRunner();
            moduleRunner.runTestFile(packageName, modelScenarioName);
            log.info("Successfully run test file: {}", packageName + "." + modelScenarioName);
        } catch (Exception e) {
            throw new ModelGenneratorException(
                    "Failed to generate and run model file: " + e.getMessage(), e);
        }
    }

    /**
     * 鑷姩娉ㄥ叆绾︽潫浠ｇ爜
     * 
     * @param className   绫诲悕
     * @param packageName 鍖呭悕
     * @return 鏄惁闇€瑕佹敞鍏?
     */
    public boolean autoInjectConstraintCode(String className, String packageName) {
        // 鑾峰彇瀹屾暣鐨勭被鍚?
        String fullClassName = packageName + "." + className;
        Class<?> injectedClazz;
        try {
            injectedClazz = Class.forName(fullClassName);
        } catch (ClassNotFoundException e) {
            log.error("Class not found: {}", fullClassName, e);
            throw new AlgLoaderException("Class not found: " + fullClassName, e);
        }
        return autoInjectConstraintCode(injectedClazz);
    }

    /**
     * 鑷姩娉ㄥ叆绾︽潫浠ｇ爜
     * 
     * @param injectedClazz 绫?
     * @return 鏄惁闇€瑕佹敞鍏?
     */
    public boolean autoInjectConstraintCode(Class<?> injectedClazz) {
        try {
            // 鍒涘缓涓存椂璺緞
            String tempPath = CommHelper.createTempPath(injectedClazz);

            // 鐢熸垚Module
            Module module = ModuleGenneratorByAnno.build(injectedClazz, tempPath);

            // 妫€鏌ユ槸鍚﹂渶瑕佹敞鍏?
            boolean isNeedInject = checkNeedInject(module);

            if (isNeedInject) {
                // 鎵ц娉ㄥ叆鎿嶄綔
                performInjection(injectedClazz, module);
                log.info("鉁?Automatic constraint code injection completed");
            }

            return isNeedInject;

        } catch (Exception e) {
            log.error("Failed to inject constraint code automatically: {}", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 妫€鏌ユ槸鍚﹂渶瑕佹敞鍏ョ害鏉熶唬鐮?
     * 
     * @param module 妯″潡瀵硅薄
     * @return 鏄惁闇€瑕佹敞鍏?
     */
    private boolean checkNeedInject(Module module) {
        if (module.getRules() == null) {
            return false;
        }

        for (Rule rule : module.getRules()) {
            if ("CDSL.V5.Struct.CompatibleRule".equals(rule.getRuleSchemaTypeFullName())) {
                return true;
            }
        }

        return false;
    }

    /**
     * 鎵ц绾︽潫浠ｇ爜娉ㄥ叆
     * 
     * @param injectedClazz 娉ㄥ叆鐨勭被
     * @param module        妯″潡瀵硅薄
     * @throws Exception 娉ㄥ叆杩囩▼涓殑寮傚父
     */
    private void performInjection(Class<?> injectedClazz, Module module) throws Exception {
        // 鐢熸垚ModuleInfo
        ModuleAlgArtifactGenerator generator = new ModuleAlgArtifactGenerator();
        ModuleVarInfo moduleInfo = generator.buildModuleInfo(module);

        // 娉ㄥ叆瑙勫垯
        StructCodeInjector injector = new StructCodeInjector();
        injector.injectRule(injectedClazz, moduleInfo.getRules());
    }
}
