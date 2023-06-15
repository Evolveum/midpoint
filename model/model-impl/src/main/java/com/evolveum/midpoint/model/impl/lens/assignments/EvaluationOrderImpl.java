/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.assignments;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.context.EvaluationOrder;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrderConstraintsType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MultiSet;
import org.jetbrains.annotations.NotNull;

/**
 * @author semancik
 *
 */
public class EvaluationOrderImpl implements EvaluationOrder {

    private static final Trace LOGGER = TraceManager.getTrace(EvaluationOrderImpl.class);

    static final EvaluationOrder UNDEFINED = new UndefinedEvaluationOrderImpl();

    @NotNull private final HashMap<QName, Integer> orderMap; // see checkConsistence

    public static EvaluationOrder zero(RelationRegistry relationRegistry) {
        EvaluationOrderImpl eo = new EvaluationOrderImpl();
        eo.orderMap.put(relationRegistry.getDefaultRelation(), 0);
        return eo;
    }

    private void checkConsistence() {
        if (CHECK_CONSISTENCE) {
            orderMap.forEach((r, v) -> {
                if (r == null) {
                    throw new IllegalStateException("Null relation in " + this);
                }
                if (isNotNormalized(r)) {
                    throw new IllegalStateException("Un-normalized relation " + r + " in " + this);
                }
                if (v == null) {
                    throw new IllegalStateException("Null value in for relation " + r + " in " + this);
                }
            });
        }
    }

    private boolean isNotNormalized(QName relation) {
        return relation == null || !relation.equals(getRelationRegistry().normalizeRelation(relation));
    }

    private RelationRegistry getRelationRegistry() {
        return SchemaService.get().relationRegistry();
    }

    private static final boolean CHECK_CONSISTENCE = true;

    private EvaluationOrderImpl() {
        orderMap = new HashMap<>();
    }

    private EvaluationOrderImpl(EvaluationOrderImpl that) {
        this.orderMap = new HashMap<>(that.orderMap);
    }

    @Override
    public int getSummaryOrder() {
        int rv = 0;
        for (Entry<QName, Integer> entry : orderMap.entrySet()) {
            if (!getRelationRegistry().isDelegation(entry.getKey())) {
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
        @NotNull QName normalizedRelation = getRelationRegistry().normalizeRelation(relation);
        orderMap.put(normalizedRelation, getMatchingRelationOrder(normalizedRelation) + amount);
    }

    @Override
    public int getMatchingRelationOrder(QName relation) {
        checkConsistence();
        if (relation == null) {
            return getSummaryOrder();
        }
        return orderMap.getOrDefault(getRelationRegistry().normalizeRelation(relation), 0);
    }

    @Override
    public EvaluationOrder resetOrder(QName relation, int newOrder) {
        EvaluationOrderImpl clone = clone();
        clone.orderMap.put(getRelationRegistry().normalizeRelation(relation), newOrder);
        clone.checkConsistence();
        return clone;
    }

    @Override
    public Map<QName, Integer> diff(EvaluationOrder newState) {
        if (!newState.isDefined()) {
            throw new IllegalArgumentException("Cannot compute diff to undefined evaluation order");
        }
        Collection<QName> relations = CollectionUtils.union(getRelations(), newState.getRelations());
        Map<QName, Integer> rv = new HashMap<>();
        // relation is not null below
        relations.forEach(relation ->
                rv.put(
                        relation,
                        newState.getMatchingRelationOrder(relation) - getMatchingRelationOrder(relation)));
        return rv;
    }

    @Override
    public EvaluationOrder applyDifference(Map<QName, Integer> difference) {
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
                sb.append("null");        // actually shouldn't occur (relations are normalized)
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
                .filter(e -> !getRelationRegistry().isAutomaticallyMatched(e.getKey()) && e.getValue() > 0)
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

    @Override
    public boolean matches(Integer assignmentOrder, List<OrderConstraintsType> assignmentOrderConstraint) {
        boolean rv;
        List<QName> extraRelations = new ArrayList<>(getExtraRelations());
        if (assignmentOrder == null && assignmentOrderConstraint.isEmpty()) {
            // compatibility
            rv = getSummaryOrder() == 1;
        } else {
            rv = true;
            if (assignmentOrder != null) {
                if (getSummaryOrder() != assignmentOrder) {
                    rv = false;
                }
            }
            for (OrderConstraintsType orderConstraint : assignmentOrderConstraint) {
                if (!isMatchingConstraint(orderConstraint)) {
                    rv = false;
                    break;
                }
                extraRelations.removeIf(r -> QNameUtil.match(r, orderConstraint.getRelation()));
            }
        }
        // TODO this is to be reconsidered -- why do we consider assignment of relation e.g. approver non-matching?
        if (!extraRelations.isEmpty()) {
            rv = false;
        }
        LOGGER.trace("computeMatchingOrder => {}, for assignment.order={}, assignment.orderConstraint={}, evaluationOrder={}, remainingExtraRelations={}",
                rv, assignmentOrder, assignmentOrderConstraint, this, extraRelations);
        return rv;
    }

    private boolean isMatchingConstraint(OrderConstraintsType orderConstraint) {
        int evaluationOrderInt = getMatchingRelationOrder(orderConstraint.getRelation());
        if (orderConstraint.getOrder() != null) {
            return orderConstraint.getOrder() == evaluationOrderInt;
        } else {
            int orderMin = 1;
            int orderMax = 1;
            if (orderConstraint.getOrderMin() != null) {
                orderMin = XsdTypeMapper.multiplicityToInteger(orderConstraint.getOrderMin());
            }
            if (orderConstraint.getOrderMax() != null) {
                orderMax = XsdTypeMapper.multiplicityToInteger(orderConstraint.getOrderMax());
            }
            return XsdTypeMapper.isMatchingMultiplicity(evaluationOrderInt, orderMin, orderMax);
        }
    }

}
