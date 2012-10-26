package com.evolveum.midpoint.prism.query;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Dumpable;

public class OrFilter extends NaryLogicalFilter {

	public OrFilter(List<ObjectFilter> condition) {
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
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("OR: \n");
		for (ObjectFilter filter : getCondition()){
			sb.append(filter.debugDump(indent + 1));
			sb.append("\n");	
		}
		return sb.toString();
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("OR: ");
		sb.append("(");
		for (int i = 0; i < getCondition().size(); i++){
			sb.append(getCondition().get(i));
			if (i != getCondition().size() -1){
				sb.append(", ");
			}
		}
		sb.append(")");
		return sb.toString();
	}
}
