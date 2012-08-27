package com.evolveum.midpoint.prism.query;

import java.util.List;

public class NaryLogicalFilter extends LogicalFilter{
	
	public NaryLogicalFilter() {
		super();
	}
	
	public NaryLogicalFilter(List<? extends ObjectFilter> conditions) {
		super();
		setCondition(conditions);
	}

	@Override
	public String dump() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String debugDump() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String debugDump(int indent) {
		// TODO Auto-generated method stub
		return null;
	}

}
