package com.evolveum.midpoint.prism.query;

import java.util.ArrayList;
import java.util.List;

public abstract class UnaryLogicalFilter extends LogicalFilter {

	public UnaryLogicalFilter() {
		super();
	}

	public UnaryLogicalFilter(ObjectFilter condition) {
		super();
		setFilter(condition);
	}

	public ObjectFilter getFilter() {
		if (getCondition().isEmpty()) {
			return null;
		}
		if (getCondition().size() == 1) {
			return getCondition().get(0);
		}
		throw new IllegalStateException("Unary logical filter can contains only one value, but contains "
				+ getCondition().size());
	}
	
	public void setFilter(ObjectFilter filter){
		List<ObjectFilter> conditions = new ArrayList<ObjectFilter>();
		conditions.add(filter);
		setCondition(conditions);
	}

}
