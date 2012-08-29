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
		if (condition == null) {
			return null;
		}
		if (condition.isEmpty()) {
			return null;
		}
		if (condition.size() == 1) {
			return condition.get(0);
		}
		throw new IllegalStateException("Unary logical filter can contains only one value, but contains "
				+ condition.size());
	}
	
	public void setFilter(ObjectFilter filter){
		condition = new ArrayList<ObjectFilter>();
		condition.add(filter);
	}

}
