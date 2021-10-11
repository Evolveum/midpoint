/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.ShortDumpable;

import org.apache.commons.collections4.MultiSet;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author semancik
 * @author mederly
 */
public interface EvaluationOrder extends DebugDumpable, ShortDumpable, Cloneable {

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
}
