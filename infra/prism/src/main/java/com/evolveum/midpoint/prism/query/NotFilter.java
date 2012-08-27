package com.evolveum.midpoint.prism.query;

import com.evolveum.midpoint.util.DebugUtil;


public class NotFilter extends UnaryLogicalFilter {

//	private ObjectFilter filter;

	public NotFilter() {

	}

	public NotFilter(ObjectFilter filter) {
		setFilter(filter);
	}

	public static NotFilter createNot(ObjectFilter filter) {
		return new NotFilter(filter);
	}

//	public ObjectFilter getFilter() {
//		return filter;
//	}
//
//	public void setFilter(ObjectFilter filter) {
//		this.filter = filter;
//	}

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
		sb.append("NOT: \n");
		DebugUtil.indentDebugDump(sb, indent);
		if (getFilter() != null) {
			sb.append("Critaria: ");
			sb.append(getFilter().debugDump(indent + 1));
			sb.append("\n");
		}

		return sb.toString();

	}
}
