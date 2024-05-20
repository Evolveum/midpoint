/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.delta;

import java.util.Collection;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class ThreeWayMerge<O extends ObjectType> {

    private PrismObject<O> left;

    private PrismObject<O> right;

    private PrismObject<O> base;

    private final ParameterizedEquivalenceStrategy strategy;

    private ObjectTreeDelta<O> leftDelta;

    private ObjectTreeDelta<O> rightDelta;

    public ThreeWayMerge(PrismObject<O> right, PrismObject<O> left, PrismObject<O> base) {
        this(right, left, base, ParameterizedEquivalenceStrategy.FOR_DELTA_ADD_APPLICATION);
    }

    public ThreeWayMerge(
            PrismObject<O> left, PrismObject<O> right, PrismObject<O> base, ParameterizedEquivalenceStrategy strategy) {

        this.base = base;
        this.left = left;
        this.right = right;
        this.strategy = strategy;

        initialize();
    }

    public ThreeWayMerge(
            ObjectDelta<O> left, ObjectDelta<O> right, PrismObject<O> base, ParameterizedEquivalenceStrategy strategy) {

        this.base = base;

        this.leftDelta = ObjectTreeDelta.fromItemDelta(left);
        this.rightDelta = ObjectTreeDelta.fromItemDelta(right);

        this.strategy = strategy;
    }

    public void initialize() {
        ObjectDelta<O> baseToLeft = base.diff(left, strategy);
        ObjectDelta<O> baseToRight = base.diff(right, strategy);

        leftDelta = ObjectTreeDelta.fromItemDelta(baseToLeft);
        rightDelta = ObjectTreeDelta.fromItemDelta(baseToRight);
    }

    public Collection<? extends ItemDelta<?, ?>> getNonConflictingModifications(
            Direction direction, ParameterizedEquivalenceStrategy strategy) {

        ObjectTreeDelta<O> delta = direction == Direction.LEFT_TO_RIGHT ? leftDelta : rightDelta;
        ObjectTreeDelta<O> other = direction == Direction.LEFT_TO_RIGHT ? rightDelta : leftDelta;

        return delta.getNonConflictingModifications(other, strategy);
    }

    public Collection<Conflict> getConflictingModifications() {
        return leftDelta.getConflictsWith(rightDelta, strategy);
    }
}
