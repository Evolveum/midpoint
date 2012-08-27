package com.evolveum.midpoint.prism.query;

import java.util.ArrayList;
import java.util.List;

public abstract class LogicalFilter extends ObjectFilter{
	
	private List<? extends ObjectFilter> condition;
	
	public LogicalFilter(){

	}
	
	
	public List<? extends ObjectFilter> getCondition() {
		if (condition == null){
			condition = new ArrayList<ObjectFilter>();
		}
		return condition;
	}
	
	public void setCondition(List<? extends ObjectFilter> condition) {
		this.condition = condition;
	}


//	@Override
//	public String dump() {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//
//	@Override
//	public String debugDump() {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//
//	@Override
//	public String debugDump(int indent) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//	
	
	
}
