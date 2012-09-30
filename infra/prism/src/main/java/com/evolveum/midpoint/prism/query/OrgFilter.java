package com.evolveum.midpoint.prism.query;

import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.util.DebugUtil;

public class OrgFilter extends ObjectFilter {

	private PrismReferenceValue orgRef;
	private String minDepth;
	private String maxDepth;

	public OrgFilter(PrismReferenceValue orgRef, String minDepth, String maxDepth) {
		this.orgRef = orgRef;
		this.minDepth = minDepth;
		this.maxDepth = maxDepth;
	}

	public static OrgFilter createOrg(PrismReferenceValue orgRef, String minDepth, String maxDepth) {
		return new OrgFilter(orgRef, minDepth, maxDepth);
	}

	public static OrgFilter createOrg(String orgRef, String minDepth, String maxDepth) {
		return new OrgFilter(new PrismReferenceValue(orgRef), minDepth, maxDepth);
	}

	public static OrgFilter createOrg(String orgRef) {
		return new OrgFilter(new PrismReferenceValue(orgRef), null, null);
	}
	
	public PrismReferenceValue getOrgRef() {
		return orgRef;
	}

	public void setOrgRef(PrismReferenceValue orgRef) {
		this.orgRef = orgRef;
	}

	public String getMinDepth() {
		return minDepth;
	}

	public void setMinDepth(String minDepth) {
		this.minDepth = minDepth;
	}

	public String getMaxDepth() {
		return maxDepth;
	}

	public void setMaxDepth(String maxDepth) {
		this.maxDepth = maxDepth;
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
		sb.append("ORG: \n");
		if (getOrgRef() != null) {
			sb.append(getOrgRef().debugDump(indent + 1));
			sb.append("\n");
		} else {
			DebugUtil.indentDebugDump(sb, indent + 1);
			sb.append("null\n");
		}

		if (getMinDepth() != null) {
			DebugUtil.indentDebugDump(sb, indent + 1);
			sb.append(getMaxDepth());
		}
		if (getMaxDepth() != null) {
			DebugUtil.indentDebugDump(sb, indent + 1);
			sb.append(getMaxDepth());
		}
		return sb.toString();
	}

}
