/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.context;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.namespace.QName;

import org.apache.commons.collections4.MultiSet;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrderConstraintsType;

/**
 * @author semancik
 * @author mederly
 */
public interface EvaluationOrder extends DebugDumpable, ShortDumpable, Cloneable, Serializable {

    int getSummaryOrder();

    EvaluationOrder advance(QName relation);

    EvaluationOrder decrease(MultiSet<QName> relations);

    int getMatchingRelationOrder(QName relation);

    Collection<QName> getExtraRelations();

    EvaluationOrder clone();

    EvaluationOrder resetOrder(QName relation, int newOrder);

    // returns the delta that would transform current object state to the newState
    // both current and new states must be defined
    Map<QName, Integer> diff(EvaluationOrder newState);

    EvaluationOrder applyDifference(Map<QName, Integer> difference);

    boolean isDefined();

    Set<QName> getRelations();

    boolean isValid();

    boolean isOrderOne();

    boolean matches(Integer assignmentOrder, List<OrderConstraintsType> assignmentOrderConstraint);
}
