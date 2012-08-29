package com.evolveum.midpoint.prism.query;

import java.util.ArrayList;
import java.util.List;

public class NaryLogicalFilter extends LogicalFilter{
	
	public NaryLogicalFilter() {
		super();
	}
	
	public NaryLogicalFilter(List<ObjectFilter> conditions) {
		super();
		setCondition(conditions);
	}
	
	public List<ObjectFilter> getCondition() {
		if (condition == null){
			condition = new ArrayList<ObjectFilter>();
		}
		return condition;
	}
	
	public void setCondition(List<ObjectFilter> condition) {
		this.condition = condition;
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
