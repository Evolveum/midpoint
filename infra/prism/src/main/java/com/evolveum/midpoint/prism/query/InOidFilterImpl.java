/*
 * Copyright (c) 2010-2017 Evolveum
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

import java.util.*;

import com.evolveum.midpoint.prism.ExpressionWrapper;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public class InOidFilter extends ObjectFilter {

	private List<String> oids;
	private ExpressionWrapper expression;
	private boolean considerOwner;				// temporary hack (checks owner OID)

	private InOidFilter(boolean considerOwner, Collection<String> oids) {
		this.considerOwner = considerOwner;
		setOids(oids);
	}

	private InOidFilter(boolean considerOwner, ExpressionWrapper expression){
		this.considerOwner = considerOwner;
		this.expression = expression;
	}

	public static InOidFilter createInOid(boolean considerOwner, Collection<String> oids){
		return new InOidFilter(considerOwner, oids);
	}

	public static InOidFilter createInOid(Collection<String> oids){
		return new InOidFilter(false, oids);
	}

	public static InOidFilter createInOid(String... oids){
		return new InOidFilter(false, Arrays.asList(oids));
	}

	public static InOidFilter createOwnerHasOidIn(Collection<String> oids){
		return new InOidFilter(true, oids);
	}

	public static InOidFilter createOwnerHasOidIn(String... oids){
		return new InOidFilter(true, Arrays.asList(oids));
	}

	public static InOidFilter createInOid(boolean considerOwner, ExpressionWrapper expression){
		return new InOidFilter(considerOwner, expression);
	}

	public Collection<String> getOids() {
		return oids;
	}

	public void setOids(Collection<String> oids) {
		this.oids = oids != null ? new ArrayList<>(oids) : null;
	}

	public boolean isConsiderOwner() {
		return considerOwner;
	}

	public ExpressionWrapper getExpression() {
		return expression;
	}

	public void setExpression(ExpressionWrapper expression) {
		this.expression = expression;
	}

	@Override
	public void checkConsistence(boolean requireDefinitions) {
		if (oids == null) {
			throw new IllegalArgumentException("Null oids in "+this);
		}
		for (String oid: oids) {
			if (StringUtils.isBlank(oid)) {
				throw new IllegalArgumentException("Empty oid in "+this);
			}
		}
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("IN OID: ");
		if (considerOwner) {
			sb.append("(for owner)");
		}
		sb.append("VALUE:");
		if (getOids() != null) {
			for (String oid : getOids()) {
				sb.append("\n");
				DebugUtil.indentDebugDump(sb, indent+1);
				sb.append(oid);
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
			for (String value : getOids()) {
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
		InOidFilter inOid = new InOidFilter(considerOwner, getOids());
		inOid.setExpression(getExpression());
		return inOid;
	}

	@Override
	public boolean match(PrismContainerValue value, MatchingRuleRegistry matchingRuleRegistry) throws SchemaException {
		if (value == null) {
			return false;			// just for sure
		}

		// are we a prism object?
		if (value.getParent() instanceof PrismObject) {
			if (considerOwner) {
				return false;
			}
			String oid = ((PrismObject) (value.getParent())).getOid();
			return StringUtils.isNotBlank(oid) && oids != null && oids.contains(oid);
		}

		final PrismContainerValue pcvToConsider;
		if (considerOwner) {
			if (!(value.getParent() instanceof PrismContainer)) {
				return false;
			}
			PrismContainer container = (PrismContainer) value.getParent();
			if (!(container.getParent() instanceof PrismContainerValue)) {
				return false;
			}
			pcvToConsider = (PrismContainerValue) container.getParent();
		} else {
			pcvToConsider = value;
		}
		return pcvToConsider.getId() != null && oids.contains(pcvToConsider.getId());
	}

	@Override
	public boolean equals(Object o, boolean exact) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		InOidFilter that = (InOidFilter) o;

		if (considerOwner != that.considerOwner)
			return false;
		if (oids != null ? !oids.equals(that.oids) : that.oids != null)
			return false;
		return expression != null ? expression.equals(that.expression) : that.expression == null;

	}

	@Override
	public int hashCode() {
		int result = oids != null ? oids.hashCode() : 0;
		result = 31 * result + (expression != null ? expression.hashCode() : 0);
		result = 31 * result + (considerOwner ? 1 : 0);
		return result;
	}
}
