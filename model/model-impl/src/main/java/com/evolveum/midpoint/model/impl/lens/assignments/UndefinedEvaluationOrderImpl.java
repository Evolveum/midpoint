/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.assignments;

import com.evolveum.midpoint.model.api.context.EvaluationOrder;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrderConstraintsType;

import org.apache.commons.collections4.MultiSet;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * @author semancik
 *
 */
public class UndefinedEvaluationOrderImpl implements EvaluationOrder {

    UndefinedEvaluationOrderImpl() {
    }

    @Override
    public int getSummaryOrder() {
        return -1; // TODO
    }

    @Override
    public EvaluationOrder advance(QName relation) {
        return this;
    }

    @Override
    public EvaluationOrder decrease(MultiSet<QName> relations) {
        return this;
    }

    @Override
    public EvaluationOrder clone() {
        return this;
    }

    @Override
    public EvaluationOrder resetOrder(QName relation, int newOrder) {
        return this;
    }

    @Override
    public Map<QName, Integer> diff(EvaluationOrder newState) {
        throw new IllegalStateException("Cannot compute diff on undefined evaluation order");
    }

    @Override
    public EvaluationOrder applyDifference(Map<QName, Integer> difference) {
        return this;
    }

    @Override
    public boolean isDefined() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;                    // undefined order is always valid
    }

    @Override
    public Set<QName> getRelations() {
        return Collections.emptySet();
    }

    @Override
    public int getMatchingRelationOrder(QName relation) {
        return -1;        // TODO
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, "EvaluationOrder UNDEFINED", indent);
        return sb.toString();
    }


    @Override
    public String toString() {
        return "EvaluationOrder(" + shortDump() + ")";
    }

    @Override
    public void shortDump(StringBuilder sb) {
        sb.append("UNDEFINED");
    }

    @Override
    public Collection<QName> getExtraRelations() {
        return Collections.emptyList();
    }

    @Override
    public boolean isOrderOne() {
        return false; // TODO
    }

    @Override
    public boolean matches(Integer assignmentOrder, List<OrderConstraintsType> assignmentOrderConstraint) {
        return false;
    }
}
