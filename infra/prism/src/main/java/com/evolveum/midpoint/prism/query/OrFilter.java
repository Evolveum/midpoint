package com.evolveum.midpoint.prism.query;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Dumpable;

public class OrFilter extends NaryLogicalFilter {

	public OrFilter(List<? extends ObjectFilter> condition) {
		super(condition);
	}

	
	public static OrFilter createOr(ObjectFilter... conditions){
		List<ObjectFilter> filters = new ArrayList<ObjectFilter>();
		for (ObjectFilter condition : conditions){
			filters.add(condition);
		}
		
		return new OrFilter(filters);
	}
	
	public static OrFilter createOr(List<ObjectFilter> conditions){	
		return new OrFilter(conditions);
	}
	
	@Override
	public String dump() {
		return debugDump(0);
	}
	
	@Override
	public String debugDump() {
		return debugDump(0);
	}
	
	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		sb.append("OR: \n");
		DebugUtil.indentDebugDump(sb, indent);
		for (ObjectFilter filter : getCondition()){
			sb.append("Critaria: ");
			sb.append(filter.debugDump(indent + 1));
			sb.append("\n");	
		}
		return sb.toString();
	}
}
