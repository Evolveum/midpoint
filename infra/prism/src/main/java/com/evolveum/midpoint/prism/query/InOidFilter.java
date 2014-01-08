package com.evolveum.midpoint.prism.query;

import java.util.Collection;
import java.util.Iterator;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugUtil;

public class InOidFilter extends ObjectFilter{

	private ItemPath fullPath;
	private Collection<String> oids;
	private QName matchingRule;
	
	InOidFilter(ItemPath path, QName matchingRule, Collection<String> oids) {
		this.fullPath = path;
		this.matchingRule = matchingRule;
		this.oids = oids;
	}
	
	public static InOidFilter createInOid(ItemPath path, QName matchingRule, Collection<String> oids){
		return new InOidFilter(path, matchingRule, oids);
	}
	
	public static InOidFilter createInOid(QName path, QName matchingRule, Collection<String> oids){
		return new InOidFilter(new ItemPath(path), matchingRule, oids);
	}
	
	
	public QName getMatchingRule() {
		return matchingRule;
	}
	
	public Collection<String> getOids() {
		return oids;
	}
	
	public ItemPath getFullPath() {
		return fullPath;
	}
	
	public void setPath(ItemPath path) {
		this.fullPath = path;
	}
	
	public void setOids(Collection<String> oids) {
		this.oids = oids;
	}
	
	public void setMatchingRule(QName matchingRule) {
		this.matchingRule = matchingRule;
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
		sb.append("IN OID: ");
		
		if (getFullPath() != null){
			sb.append("\n");
			DebugUtil.indentDebugDump(sb, indent+1);
			sb.append("PATH: ");
			sb.append(getFullPath().toString());
		} 
			
		sb.append("\n");
		DebugUtil.indentDebugDump(sb, indent+1);
		sb.append("VALUE:");
		if (getOids() != null) {
			sb.append("\n");
			for (String oid : getOids()) {
				sb.append(oid);
				sb.append("\n");
			}
		} else {
			sb.append(" null");
		}
		
		sb.append("\n");
		DebugUtil.indentDebugDump(sb, indent+1);
		sb.append("MATCHING: ");
		if (getMatchingRule() != null) {
			sb.append(getMatchingRule());
		} else {
			sb.append("default");
		}
		return sb.toString();
		
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("IN OID: ");
		if (getFullPath() != null){
			sb.append(getFullPath().toString());
			sb.append(", ");
		}
		if (getOids() != null){
			Iterator<String> itertor = getOids().iterator();
			while (itertor.hasNext()){
				String value = itertor.next();
				if (value == null) {
					sb.append("null");
				} else {
					sb.append(value);
				}
				sb.append("; ");
				
			}
		}
		return sb.toString();
	}

	@Override
	public InOidFilter clone() {
		return new InOidFilter(getFullPath(), getMatchingRule(), getOids());
	}

	@Override
	public <T extends Objectable> boolean match(PrismObject<T> object,
			MatchingRuleRegistry matchingRuleRegistry) {
		return false;
	}

}
