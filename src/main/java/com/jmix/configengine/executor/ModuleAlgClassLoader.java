package com.jmix.configengine.executor;

import com.jmix.configengine.artifact.ConstraintAlgImpl;
import com.jmix.configengine.model.ModuleAlgArtifact;

import java.util.HashMap;
import java.util.Map;

/**
 * Module algorithm class loader (extracted from executor impl)
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
	
	public void init(String moduleCode, ModuleAlgArtifact algArtifact) {
		this.algArtifact = algArtifact;
		if (algArtifact != null) {
			this.constraintRuleClassName = algArtifact.getPackageName() + "." + moduleCode + "Constraint";
		}
	}
	
	public ConstraintAlgImpl newConstraintAlg(String moduleCode) throws Exception {
		String className = this.constraintRuleClassName;
		Class<?> clazz = classMap.get(className);
		if (clazz == null) {
			clazz = loadClass(className);
			classMap.put(className, clazz);
		}
		return (ConstraintAlgImpl) clazz.getDeclaredConstructor().newInstance();
	}
	
	@Override
	public Class<?> loadClass(String name) {
		try {
			// TODO: support jar loading by algArtifact and rootFilePath when not attached debug
			return super.loadClass(name);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Class not found: " + name, e);
		}
	}
} 