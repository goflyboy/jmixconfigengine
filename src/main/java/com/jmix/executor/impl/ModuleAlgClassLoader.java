package com.jmix.executor.impl;

import com.jmix.executor.imodel.ModuleAlgArtifact;
import com.jmix.executor.impl.algmodel.ConstraintAlgImpl;
import com.jmix.executor.omodel.AlgLoaderException;

import java.util.HashMap;
import java.util.Map;

/**
 * 模块算法类加载器
 * 负责动态加载和实例化约束算法类
 * 
 * @since 2025-09-22
 */
public class ModuleAlgClassLoader extends ClassLoader {

    private final boolean isAttachedDebug;

    private final String rootFilePath;

    private String constraintRuleClassName;

    private final Map<String, Class<?>> classMap = new HashMap<>();

    private ModuleAlgArtifact algArtifact;

    public ModuleAlgClassLoader(boolean isAttachedDebug, String rootFilePath) {
        this.isAttachedDebug = isAttachedDebug;
        this.rootFilePath = rootFilePath;
    }

    /**
     * 初始化模块算法类加载器
     * 
     * @param moduleCode
     * @param algArtifact
     */
    public void init(String moduleCode, ModuleAlgArtifact algArtifact) {
        this.algArtifact = algArtifact;
        if (algArtifact != null) {
            // 构建完整的类名，支持内部类场景
            StringBuilder classNameBuilder = new StringBuilder();
            // 添加包名和类名
            classNameBuilder.append(algArtifact.getPackageName()).append(".");
            // 如果有父类名称，先添加父类
            if (algArtifact.getParentClassName() != null
                    && !algArtifact.getParentClassName().isEmpty()) {
                classNameBuilder.append(algArtifact.getParentClassName()).append("$");
            }
            classNameBuilder.append(moduleCode).append("Constraint");
            this.constraintRuleClassName = classNameBuilder.toString();
        }
    }

    /**
     * 创建模块算法类
     * 
     * @param moduleCode
     * @return
     * @throws Exception
     */
    public ConstraintAlgImpl newConstraintAlg(String moduleCode) throws Exception {
        String className = this.constraintRuleClassName;
        Class<?> clazz = classMap.get(className);
        if (clazz == null) {
            clazz = loadClass(className);
            classMap.put(className, clazz);
        }
        return (ConstraintAlgImpl) clazz.getDeclaredConstructor().newInstance();
    }

    /**
     * 加载类
     * 
     * @param name
     * @return
     */
    @Override
    public Class<?> loadClass(String name) {
        try {
            // : support jar loading by algArtifact and rootFilePath when not attached debug
            return super.loadClass(name);
        } catch (ClassNotFoundException e) {
            throw new AlgLoaderException("Class not found: " + name, e);
        }
    }

    /**
     * 
     * @return 模块算法类加载器信息
     */
    public String toString() {
        return "ModuleAlgClassLoader{"
                + "isAttachedDebug="
                + isAttachedDebug
                + ", rootFilePath='"
                + rootFilePath
                + '\''
                + ", constraintRuleClassName='"
                + constraintRuleClassName
                + '\''
                + ", classMap="
                + classMap
                + ", algArtifact="
                + algArtifact
                + '}';
    }
}