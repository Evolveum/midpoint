/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.query;

/**
 *
 */
public interface NotFilter extends UnaryLogicalFilter {

	@Override
	NotFilter clone();

	@Override
	NotFilter cloneEmpty();

	void setFilter(ObjectFilter inner);

	//	@Override
//	protected String getDebugDumpOperationName() {
//		return "AND";
//	}

}
