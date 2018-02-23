/*
 * Copyright (c) 2017 Evolveum
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

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.context.EvaluationOrder;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.MultiSet;
import org.jetbrains.annotations.NotNull;

/**
 * @author semancik
 *
 */
public class EvaluationOrderImpl implements EvaluationOrder {

	public static EvaluationOrder UNDEFINED = new UndefinedEvaluationOrderImpl();
	public static EvaluationOrder ZERO = createZero();
	public static EvaluationOrder ONE = ZERO.advance(SchemaConstants.ORG_DEFAULT);

	@NotNull private final HashMap<QName, Integer> orderMap;		// see checkConsistence

	private void checkConsistence() {
		if (CHECK_CONSISTENCE) {
			orderMap.forEach((r, v) -> {
				if (r == null || QNameUtil.noNamespace(r)) {
					throw new IllegalStateException("Null or unqualified relation " + r + " in " + this);
				}
				if (v == null) {
					throw new IllegalStateException("Null value in for relation " + r + " in " + this);
				}
			});
		}
	}

	private static final boolean CHECK_CONSISTENCE = true;

	private EvaluationOrderImpl() {
		orderMap = new HashMap<>();
	}

	private EvaluationOrderImpl(EvaluationOrderImpl that) {
		this.orderMap = new HashMap<>(that.orderMap);
	}

	private static EvaluationOrderImpl createZero() {
		EvaluationOrderImpl eo = new EvaluationOrderImpl();
		eo.orderMap.put(SchemaConstants.ORG_DEFAULT, 0);
		return eo;
	}

	@Override
	public int getSummaryOrder() {
		int rv = 0;
		for (Entry<QName, Integer> entry : orderMap.entrySet()) {
			if (!ObjectTypeUtil.isDelegationRelation(entry.getKey())) {
				rv += entry.getValue();
			}
		}
		return rv;
	}

	@Override
	public EvaluationOrder advance(QName relation) {
		checkConsistence();
		return advance(relation, 1);
	}

	private EvaluationOrder advance(QName relation, int amount) {
		EvaluationOrderImpl clone = clone();
		clone.advanceThis(relation, amount);
		clone.checkConsistence();
		return clone;
	}

	@Override
	public EvaluationOrder decrease(MultiSet<QName> relations) {
		EvaluationOrderImpl clone = clone();
		for (QName relation : relations) {
			clone.advanceThis(relation, -1);
		}
		clone.checkConsistence();
		return clone;
	}

	// must always be private: public interface will not allow to modify object state!
	private void advanceThis(QName relation, int amount) {
		@NotNull QName normalizedRelation = ObjectTypeUtil.normalizeRelation(relation);
		orderMap.put(normalizedRelation, getMatchingRelationOrder(normalizedRelation) + amount);
	}

	@Override
	public int getMatchingRelationOrder(QName relation) {
		checkConsistence();
		if (relation == null) {
			return getSummaryOrder();
		}
		return orderMap.getOrDefault(ObjectTypeUtil.normalizeRelation(relation), 0);
	}

	@Override
	public EvaluationOrder resetOrder(QName relation, int newOrder) {
		EvaluationOrderImpl clone = clone();
		clone.orderMap.put(ObjectTypeUtil.normalizeRelation(relation), newOrder);
		clone.checkConsistence();
		return clone;
	}

	@Override
	public Map<QName, Integer> diff(EvaluationOrder newState) {
		if (!newState.isDefined()) {
			throw new IllegalArgumentException("Cannot compute diff to undefined evaluation order");
		}
		@SuppressWarnings({"unchecked", "raw"})
		Collection<QName> relations = CollectionUtils.union(getRelations(), newState.getRelations());
		Map<QName, Integer> rv = new HashMap<>();
		// relation is not null below
		relations.forEach(relation -> rv.put(relation, newState.getMatchingRelationOrder(relation) - getMatchingRelationOrder(relation)));
		return rv;
	}

	@Override
	public EvaluationOrder applyDifference(Map<QName,Integer> difference) {
		EvaluationOrderImpl clone = clone();
		difference.forEach((relation, count) -> clone.advanceThis(relation, count));
		clone.checkConsistence();
		return clone;
	}

	@Override
	public boolean isDefined() {
		return true;
	}

	@Override
	public Set<QName> getRelations() {
		return orderMap.keySet();
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.debugDumpLabelLn(sb, "EvaluationOrder", indent);
		DebugUtil.debugDumpWithLabelLn(sb, "summaryOrder", getSummaryOrder(), indent + 1);
		DebugUtil.debugDumpWithLabel(sb, "orderMap", orderMap, indent + 1);
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof EvaluationOrderImpl))
			return false;
		EvaluationOrderImpl that = (EvaluationOrderImpl) o;
		return Objects.equals(orderMap, that.orderMap);
	}

	@Override
	public int hashCode() {
		return Objects.hash(orderMap);
	}

	@Override
	public String toString() {
		return "EvaluationOrder(" + shortDump() + ")";
	}

	@Override
	public String shortDump() {
		StringBuilder sb = new StringBuilder();
		shortDump(sb);
		return sb.toString();
	}

	@Override
	public void shortDump(StringBuilder sb) {
		for (Entry<QName,Integer> entry: orderMap.entrySet()) {
			if (entry.getKey() != null) {
				sb.append(entry.getKey().getLocalPart());
			} else {
				sb.append("null");		// actually shouldn't occur (relations are normalized)
			}
			sb.append(":");
			sb.append(entry.getValue());
			sb.append(",");
		}
		sb.setLength(sb.length() - 1);
		sb.append("=").append(getSummaryOrder());
	}

	@Override
	public Collection<QName> getExtraRelations() {
		return orderMap.entrySet().stream()
				.filter(e -> !ObjectTypeUtil.isMembershipRelation(e.getKey()) && !ObjectTypeUtil.isDelegationRelation(e.getKey()) && e.getValue() > 0)
				.map(e -> e.getKey())
				.collect(Collectors.toSet());
	}

	@Override
	public EvaluationOrderImpl clone() {
		return new EvaluationOrderImpl(this);
	}

	@Override
	public boolean isValid() {
		return orderMap.values().stream().allMatch(c -> c >= 0);
	}

	@Override
	public boolean isOrderOne() {
		return getSummaryOrder() == 1;
	}
}
