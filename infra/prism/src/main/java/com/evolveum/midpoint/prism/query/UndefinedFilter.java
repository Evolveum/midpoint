package com.evolveum.midpoint.prism.query;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public class UndefinedFilter extends ObjectFilter {

	public UndefinedFilter() {
		super();
	}

	public static UndefinedFilter createUndefined() {
		return new UndefinedFilter();
	}
	
	@Override
	public UndefinedFilter clone() {
		return new UndefinedFilter();
	}

	@Override
	public void checkConsistence() {
		// nothing to do
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("UNDEFINED");
		return sb.toString();

	}
	
	@Override
	public String toString() {
		return "UNDEFINED";
	}

	@Override
	public boolean match(PrismContainerValue value, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
		return true;
	}

}
