package com.jmix.configengine.scenario.base;

import com.jmix.configengine.ModuleConstraintExecutor;
import com.jmix.configengine.artifact.ConstraintAlgImpl;
import com.jmix.configengine.executor.ModuleConstraintExecutorImpl;
import com.jmix.configengine.model.Module;
import lombok.extern.slf4j.Slf4j;


import java.io.File;
import java.util.List;

import org.junit.Before;

/**
 * 模块场景测试基类
 */
@Slf4j
public abstract class ModuleSecnarioTestBase {
    
    // 核心属性
    protected Class<? extends ConstraintAlgImpl> constraintAlgClazz;
    protected String tempResourcePath = "";
    protected Module module;
    protected ModuleConstraintExecutor.ConstraintConfig cfg;
    protected ModuleConstraintExecutorImpl exec;
    protected List<ModuleConstraintExecutor.ModuleInst> solutions;
    
    /**
     * 构造函数
     */
    public ModuleSecnarioTestBase(Class<? extends ConstraintAlgImpl> constraintAlgClazz) {
        this.constraintAlgClazz = constraintAlgClazz;
    }
    
    /**
     * 每个用例执行前调用
     */
    @Before
    public void setUp() {
        init();
    }
    
    /**
     * 构建Module数据
     */
    protected Module buildModule(Class<? extends ConstraintAlgImpl> moduleAlgClazz) {
        // 生成临时资源路径
        tempResourcePath = getResourcePath(moduleAlgClazz) + "/tempResource";
        createDirectory(tempResourcePath);
        
        // 通过注解生成Module
        module = ModuleGenneratorByAnno.build(moduleAlgClazz, tempResourcePath);
        return module;
    }
    
    /**
     * 初始化测试环境
     */
    protected void init() {
        exec = new ModuleConstraintExecutorImpl();
        cfg = new ModuleConstraintExecutor.ConstraintConfig();
        cfg.isAttachedDebug = true; // 测试环境直接使用当前classpath加载
        cfg.rootFilePath = tempResourcePath;
        exec.init(cfg);
        
        module = buildModule(this.constraintAlgClazz);
        exec.addModule(module.getId(), module);
    }
    
    /**
     * 执行参数推理
     */
    protected List<ModuleConstraintExecutor.ModuleInst> inferParas(String partCode, Integer qty) {
        ModuleConstraintExecutor.InferParasReq req = new ModuleConstraintExecutor.InferParasReq();
        req.moduleId = module.getId();
        req.enumerateAllSolution = true;
        
        ModuleConstraintExecutor.Result<List<ModuleConstraintExecutor.ModuleInst>> r = exec.inferParas(req);
        log.info("推理结果: {}", r);
        
        solutions = r.data;
        return solutions;
    }
    
    /**
     * 获取指定索引的解决方案
     */
    protected ProgammableInstAssert solution(int index) {
        if (solutions == null || index >= solutions.size()) {
            throw new IndexOutOfBoundsException("解决方案索引超出范围: " + index);
        }
        ModuleConstraintExecutor.ModuleInst solution = solutions.get(index);
        return new ProgammableInstAssert(solution);
    }
    
    /**
     * 打印所有解决方案
     */
    protected void printSolutions() {
        if (solutions == null || solutions.isEmpty()) {
            log.info("没有找到解决方案");
            return;
        }
        
        log.info("找到 {} 个解决方案:", solutions.size());
        for (int i = 0; i < solutions.size(); i++) {
            log.info("解决方案 {}: {}", i, ModuleConstraintExecutorImpl.toJson(solutions.get(i)));
        }
    }
    
    /**
     * 获取资源路径
     */
    private String getResourcePath(Class<?> clazz) {
        String packagePath = clazz.getPackage().getName().replace('.', '/');
        return "src/test/resources/" + packagePath;
    }
    
    /**
     * 创建目录
     */
    private void createDirectory(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                log.info("创建目录: {}", path);
            } else {
                log.warn("创建目录失败: {}", path);
            }
        }
    }
} 