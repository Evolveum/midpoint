/**
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.model.impl.lens;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.context.EvaluationOrder;
import com.evolveum.midpoint.model.api.util.DeputyUtils;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;

/**
 * @author semancik
 *
 */
public class EvaluationOrderImpl implements EvaluationOrder {

	public static EvaluationOrder UNDEFINED = new UndefinedEvaluationOrderImpl();
	public static EvaluationOrder ZERO = createZero();
	public static EvaluationOrder ONE = ZERO.advance();

	private int summaryOrder = 0;
	private final HashMap<QName,Integer> orderMap = new HashMap<>();

	private EvaluationOrderImpl() {
	}

	private static EvaluationOrderImpl createZero() {
		EvaluationOrderImpl eo = new EvaluationOrderImpl();
		eo.orderMap.put(null, 0);
		return eo;
	}

	@Override
	public int getSummaryOrder() {
		return summaryOrder;
	}
	
	@Override
	public EvaluationOrder advance() {
		return advance(null);
	}
	
	@Override
	public EvaluationOrder advance(QName relation) {
		return advance(1, relation);
	}

	private EvaluationOrder advance(int amount, QName relation) {
		EvaluationOrderImpl advanced = new EvaluationOrderImpl();
		boolean found = false;
		for (Entry<QName,Integer> entry: orderMap.entrySet()) {
			if (QNameUtil.match(entry.getKey(), relation)) {
				advanced.orderMap.put(entry.getKey(), entry.getValue() + amount);
				found = true;
			} else {
				advanced.orderMap.put(entry.getKey(), entry.getValue());
			}
		}
		if (!found) {
			advanced.orderMap.put(relation, amount);
		}
		if (DeputyUtils.isDelegationRelation(relation)) {
			advanced.summaryOrder = this.summaryOrder;
		} else {
			advanced.summaryOrder = this.summaryOrder + amount;
		}
		return advanced;
	}

	@Override
	public EvaluationOrder decrease(int amount) {
		return decrease(amount, null);
	}

	@Override
	public EvaluationOrder decrease(int amount, QName relation) {
		return advance(-amount, relation);
	}

	@Override
	public int getMatchingRelationOrder(QName relation) {
		for (Entry<QName,Integer> entry: orderMap.entrySet()) {
			if (QNameUtil.match(entry.getKey(), relation)) {
				return entry.getValue();
			}
		}
		return 0;
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.debugDumpLabelLn(sb, "EvaluationOrder", indent);
		DebugUtil.debugDumpWithLabelLn(sb, "summaryOrder", summaryOrder, indent + 1);
		DebugUtil.debugDumpWithLabel(sb, "orderMap", orderMap, indent + 1);
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((orderMap == null) ? 0 : orderMap.hashCode());
		result = prime * result + summaryOrder;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		EvaluationOrderImpl other = (EvaluationOrderImpl) obj;
		if (orderMap == null) {
			if (other.orderMap != null) {
				return false;
			}
		} else if (!orderMap.equals(other.orderMap)) {
			return false;
		}
		if (summaryOrder != other.summaryOrder) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "EvaluationOrder(" + shortDump() + ")";
	}
	
	@Override
	public String shortDump() {
		StringBuilder sb = new StringBuilder();
		for (Entry<QName,Integer> entry: orderMap.entrySet()) {
			if (entry.getKey() != null) {
				sb.append(entry.getKey().getLocalPart());
			} else {
				sb.append("null");
			}
			sb.append(":");
			sb.append(entry.getValue());
			sb.append(",");
		}
		sb.setLength(sb.length() - 1);
		sb.append("=").append(summaryOrder);
		return sb.toString();
	}

	@Override
	public Collection<QName> getExtraRelations() {
		return orderMap.keySet().stream()
				.filter(r -> !DeputyUtils.isMembershipRelation(r) && !DeputyUtils.isDelegationRelation(r))
				.collect(Collectors.toSet());
	}
}
