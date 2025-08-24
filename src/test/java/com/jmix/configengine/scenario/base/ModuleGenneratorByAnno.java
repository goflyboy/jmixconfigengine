package com.jmix.configengine.scenario.base;

import com.jmix.configengine.artifact.ConstraintAlg;
import com.jmix.configengine.model.Para;
import com.jmix.configengine.model.Part;
import com.jmix.configengine.model.ParaOption;
import com.jmix.configengine.model.ParaType;
import com.jmix.configengine.model.PartType;
import com.jmix.configengine.model.Module;
import com.jmix.configengine.model.ModuleAlgArtifact;
import com.jmix.configengine.util.ModuleUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

/**
 * 通过注解生成Module的工具类
 */
@Slf4j
public class ModuleGenneratorByAnno {
    
    public static Module build(Class<? extends ConstraintAlg> moduleAlgClazz, String resourcePath) {
        Module module = new Module();
        
        // 1. 根据ModuleAnno信息创建Module
        ModuleAnno moduleAnno = moduleAlgClazz.getAnnotation(ModuleAnno.class);
        if (moduleAnno != null) {
            // 1.1 生成Module.code
            String className = moduleAlgClazz.getSimpleName();
            String code = className.replace("Constraint", "");
            module.setCode(code);
            
            // 1.2 设置其他属性
            module.setId(moduleAnno.id());
            module.setPackageName(moduleAnno.packageName().isEmpty() ? 
                moduleAlgClazz.getPackage().getName() : moduleAnno.packageName());
            module.setVersion(moduleAnno.version());
            module.setDescription(moduleAnno.description());
            module.setSortNo(moduleAnno.sortNo());
            module.setExtSchema(moduleAnno.extSchema());

            //生成module.alg
            ModuleAlgArtifact alg = new ModuleAlgArtifact();
            alg.setId(module.getId());
            alg.setFileName(moduleAlgClazz.getName());
            alg.setPackageName(moduleAlgClazz.getPackage().getName());
            
            // 检查是否为内部类，设置parentClassName
            Class<?> enclosingClass = moduleAlgClazz.getEnclosingClass();
            if (enclosingClass != null) {
                // 是内部类，设置父类名称
                alg.setParentClassName(enclosingClass.getName());
                log.info("检测到内部类: {}，父类: {}", moduleAlgClazz.getName(), enclosingClass.getName());
            } else {
                // 不是内部类，保持默认空字符串
                alg.setParentClassName("");
            }
            
            module.setAlg(alg);
            
            // 处理扩展属性
            if (moduleAnno.extAttrs().length > 0) {
                Map<String, String> extAttrs = new HashMap<>();
                for (String attr : moduleAnno.extAttrs()) {
                    String[] parts = attr.split(":");
                    if (parts.length == 2) {
                        extAttrs.put(parts[0], parts[1]);
                    }
                }
                module.setExtAttrs(extAttrs);
            }
        }
        
        // 2. 遍历成员变量，创建Para和Part
        List<Para> paras = new ArrayList<>();
        List<Part> parts = new ArrayList<>();
        
        for (Field field : moduleAlgClazz.getDeclaredFields()) {
            if (field.getType().getSimpleName().equals("ParaVar")) {
                Para para = createParaFromField(field);
                if (para != null) {
                    paras.add(para);
                }
            } else if (field.getType().getSimpleName().equals("PartVar")) {
                Part part = createPartFromField(field);
                if (part != null) {
                    parts.add(part);
                }
            }
        }
        
        module.setParas(paras);
        module.setParts(parts);
        
        // 3. 保存Module到文件
        String fileName = module.getCode() + ".base.json";
        String filePath = resourcePath + "/" + fileName;
        
        try {
            ModuleUtils.toJsonFile(module, filePath);
            log.info("Module已保存到: {}", filePath);
        } catch (Exception e) {
            log.error("保存Module失败", e);
        }
        
        return module;
    }
    
    private static Para createParaFromField(Field field) {
        ParaAnno paraAnno = field.getAnnotation(ParaAnno.class);
        if (paraAnno == null) return null;
        
        Para para = new Para();
        
        // 生成Para.code
        String fieldName = field.getName();
        String code = fieldName.replace("Var", "");
        para.setCode(code);
        
        // 设置其他属性
        para.setFatherCode(paraAnno.fatherCode());
        para.setDefaultValue(paraAnno.defaultValue());
        para.setDescription(paraAnno.description());
        para.setSortNo(paraAnno.sortNo());
        para.setType(paraAnno.type());
        para.setExtSchema(paraAnno.extSchema());
        
        // 处理扩展属性
        if (paraAnno.extAttrs().length > 0) {
            Map<String, String> extAttrs = new HashMap<>();
            for (String attr : paraAnno.extAttrs()) {
                String[] parts = attr.split(":");
                if (parts.length == 2) {
                    extAttrs.put(parts[0], parts[1]);
                }
            }
            para.setExtAttrs(extAttrs);
        }
        
        // 处理枚举选项
        if (paraAnno.options().length > 0) {
            List<ParaOption> options = new ArrayList<>();
            for (int i = 0; i < paraAnno.options().length; i++) {
                ParaOption option = new ParaOption();
                option.setCode(paraAnno.options()[i]);
                option.setCodeId((i + 1) * 10); // 10, 20, 30...
                option.setDefaultValue(paraAnno.options()[i]);
                option.setDescription("");
                option.setSortNo(i + 1);
                options.add(option);
            }
            para.setOptions(options);
        }
        
        return para;
    }
    
    private static Part createPartFromField(Field field) {
        PartAnno partAnno = field.getAnnotation(PartAnno.class);
        if (partAnno == null) return null;
        
        Part part = new Part();
        
        // 生成Part.code
        String fieldName = field.getName();
        String code = fieldName.replace("Var", "");
        part.setCode(code);
        
        // 设置其他属性
        part.setFatherCode(partAnno.fatherCode());
        part.setDefaultValue(0); // Part的默认值类型是Integer
        part.setDescription(partAnno.description());
        part.setSortNo(partAnno.sortNo());
        part.setType(partAnno.type());
        part.setPrice(partAnno.price());
        part.setExtSchema(partAnno.extSchema());
        
        // 处理规格属性
        if (partAnno.attrs().length > 0) {
            Map<String, String> attrs = new HashMap<>();
            for (String attr : partAnno.attrs()) {
                String[] parts = attr.split(":");
                if (parts.length == 2) {
                    attrs.put(parts[0], parts[1]);
                }
            }
            part.setAttrs(attrs);
        }
        
        // 处理扩展属性
        if (partAnno.extAttrs().length > 0) {
            Map<String, String> extAttrs = new HashMap<>();
            for (String attr : partAnno.extAttrs()) {
                String[] parts = attr.split(":");
                if (parts.length == 2) {
                    extAttrs.put(parts[0], parts[1]);
                }
            }
            part.setExtAttrs(extAttrs);
        }
        
        return part;
    }
} 