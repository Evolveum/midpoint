/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.query;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.UnaryLogicalFilter;

import java.util.ArrayList;

public abstract class UnaryLogicalFilterImpl extends LogicalFilterImpl implements UnaryLogicalFilter {

	public ObjectFilter getFilter() {
		if (conditions == null) {
			return null;
		}
		if (conditions.isEmpty()) {
			return null;
		}
		if (conditions.size() == 1) {
			return conditions.get(0);
		}
		throw new IllegalStateException("Unary logical filter can contains only one value, but contains " + conditions.size());
	}
	
	public void setFilter(ObjectFilter filter){
		conditions = new ArrayList<>();
		conditions.add(filter);
	}

	@Override
	public abstract UnaryLogicalFilterImpl clone();

}
