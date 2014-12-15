/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.prism.query;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugUtil;

public class InOidFilter extends ObjectFilter {

	private Collection<String> oids;
	private ExpressionWrapper expression;
	
	InOidFilter(Collection<String> oids) {
		this.oids = oids;
	}
	
	InOidFilter(ExpressionWrapper expression){
		this.expression = expression;
	}
	
	public static InOidFilter createInOid(Collection<String> oids){
		return new InOidFilter(oids);
	}
	
	public static InOidFilter createInOid(String... oids){
		return new InOidFilter(Arrays.asList(oids));
	}
	
	public static InOidFilter createInOid(ExpressionWrapper expression){
		return new InOidFilter(expression);
	}
		
	public Collection<String> getOids() {
		return oids;
	}
	
	public void setOids(Collection<String> oids) {
		this.oids = oids;
	}
	
	public ExpressionWrapper getExpression() {
		return expression;
	}
	
	public void setExpression(ExpressionWrapper expression) {
		this.expression = expression;
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
		InOidFilter inOid = new InOidFilter(getOids());
		inOid.setExpression(getExpression());
		return inOid;
	}

	@Override
	public <T extends Objectable> boolean match(PrismObject<T> object,
			MatchingRuleRegistry matchingRuleRegistry) {
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((oids == null) ? 0 : oids.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InOidFilter other = (InOidFilter) obj;
		if (oids == null) {
			if (other.oids != null)
				return false;
		} else if (!oids.equals(other.oids))
			return false;
		return true;
	}

}
