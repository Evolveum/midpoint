/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.query;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.query.UndefinedFilter;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public class UndefinedFilterImpl extends ObjectFilterImpl implements UndefinedFilter {

	public UndefinedFilterImpl() {
		super();
	}

	public static UndefinedFilter createUndefined() {
		return new UndefinedFilterImpl();
	}

	@Override
	public UndefinedFilterImpl clone() {
		return new UndefinedFilterImpl();
	}

	@Override
	public void checkConsistence(boolean requireDefinitions) {
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

	@Override
	public boolean equals(Object obj, boolean exact) {
		return obj instanceof UndefinedFilter;
	}

	@Override
	public int hashCode() {
		return 0;
	}


}
