package com.evolveum.midpoint.prism.query;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Dumpable;

public class AndFilter extends NaryLogicalFilter{
	
	
	public AndFilter(List<ObjectFilter> condition) {
		super(condition);

	}

	
	public static AndFilter createAnd(ObjectFilter... conditions){
		List<ObjectFilter> filters = new ArrayList<ObjectFilter>();
		for (ObjectFilter condition : conditions){
			filters.add(condition);
		}
		
		return new AndFilter(filters);
	}
	
	public static AndFilter createAnd(List<ObjectFilter> conditions){
		return new AndFilter(conditions);
	}
	
	@Override
	public AndFilter clone() {
		return new AndFilter(getClonedConditions());
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
		sb.append("AND: \n");
		DebugUtil.indentDebugDump(sb, indent);
		for (ObjectFilter filter : getCondition()){
			sb.append(filter.debugDump(indent + 1));
			sb.append("\n");	
		}

		return sb.toString();

	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("AND: ");
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
