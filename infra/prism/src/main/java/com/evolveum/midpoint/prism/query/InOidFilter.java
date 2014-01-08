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

	private Collection<String> oids;
	
	InOidFilter(Collection<String> oids) {
		this.oids = oids;
	}
	
	public static InOidFilter createInOid(Collection<String> oids){
		return new InOidFilter(oids);
	}
	
		
	public Collection<String> getOids() {
		return oids;
	}
	
	public void setOids(Collection<String> oids) {
		this.oids = oids;
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
		
		
		return sb.toString();
		
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("IN OID: ");
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
		return new InOidFilter(getOids());
	}

	@Override
	public <T extends Objectable> boolean match(PrismObject<T> object,
			MatchingRuleRegistry matchingRuleRegistry) {
		return false;
	}

}
