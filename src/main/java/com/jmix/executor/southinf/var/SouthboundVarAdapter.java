package com.jmix.executor.southinf.var;

import com.jmix.executor.bmodel.base.Programmable;
import com.jmix.executor.impl.algmodel.VarImpl;

/**
 * Base adapter for southbound variable facades.
 */
abstract class SouthboundVarAdapter<T extends Programmable> extends VarImpl<T> {
}
