/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.context;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPathSegmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrderConstraintsType;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;

/**
 * Single assignment in an assignment path. In addition to the AssignmentType, it contains resolved target:
 * full object, resolved from targetRef. If targetRef resolves to multiple objects, in the path segment
 * one of them is stored: the one that participates in the particular assignment path.
 *
 * @author semancik
 */
public interface AssignmentPathSegment extends DebugDumpable, ShortDumpable, Serializable {

    // Returns version of the assignment (old/new) that was evaluated
    AssignmentType getAssignment();

    AssignmentType getAssignment(boolean evaluateOld);

    // Returns 'assignment new' - i.e. the analogous to getAssignment(false)
    // Until 2017-06-13 the name of this method was 'getAssignment()'
    // TODO its use is a bit questionable; it might return null, when evaluating negative-mode assignments
    AssignmentType getAssignmentNew();

    AssignmentType getAssignmentAny();

    /**
     * True if the segment corresponds to assignment. False if it's an inducement.
     */
    boolean isAssignment();

    ObjectType getSource();

    String getSourceOid();

    ObjectType getTarget();

    QName getRelation();

    /**
     *  Whether this assignment/inducement matches the focus level, i.e. if we should collect constructions,
     *  focus mappings, focus policy rules and similar items from it.
     */
    boolean isMatchingOrder();

    /**
     *  Whether this assignment/inducement matches the target level, i.e. if we should collect target
     *  policy rules from it.
     */
    boolean isMatchingOrderForTarget();

    /**
     * True if the relation is a delegation one.
     */
    boolean isDelegation();

    @NotNull AssignmentPathSegmentType toAssignmentPathSegmentBean(boolean includeAssignmentsContent);

    /**
     * Returns true if the path segment matches specified order constraints. All of them must match.
     * Although there are some defaults, it is recommended to specify constraints explicitly.
     */
    boolean matches(@NotNull List<OrderConstraintsType> orderConstraints);

    // Preliminary limited implementation. Use with care.
    boolean equivalent(AssignmentPathSegment otherSegment);

    @NotNull ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> getAssignmentIdi();
}
