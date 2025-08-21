package com.jmix.configengine.artifact;

import com.google.ortools.sat.CpSolverSolutionCallback;
import com.jmix.configengine.model.Programmable;

/**
 * Generic variable wrapper that binds to a programmable model object
 */
public abstract class Var<T extends Programmable> implements Programmable {
	protected T base;

	protected Var() {
	}

	protected Var(T base) {
		this.base = base;
	}

	public T getBase() {
		return base;
	}

	public void setBase(T base) {
		this.base = base;
	}

	@Override
	public String getCode() {
		return base != null ? base.getCode() : null;
	}

	public String getVarString(CpSolverSolutionCallback solutionCallback){
		return base != null ? base.getCode() : null;
	}
} 