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

public class ThreeWayMergeOperation<O extends ObjectType> {

    private PrismObject<O> left;

    private PrismObject<O> right;

    private PrismObject<O> base;

    private final ParameterizedEquivalenceStrategy strategy;

    private ObjectTreeDelta<O> leftDelta;

    private ObjectTreeDelta<O> rightDelta;

    public ThreeWayMergeOperation(PrismObject<O> left, PrismObject<O> right, PrismObject<O> base) {
        this(left, right, base, ParameterizedEquivalenceStrategy.FOR_DELTA_ADD_APPLICATION);
    }

    public ThreeWayMergeOperation(
            PrismObject<O> left, PrismObject<O> right, PrismObject<O> base, ParameterizedEquivalenceStrategy strategy) {

        this.base = base;
        this.left = left;
        this.right = right;
        this.strategy = strategy;

        initialize();
    }

    public ThreeWayMergeOperation(
            ObjectDelta<O> left, ObjectDelta<O> right, PrismObject<O> base, ParameterizedEquivalenceStrategy strategy) {

        this.base = base;

        this.leftDelta = ObjectTreeDelta.fromItemDelta(left);
        this.rightDelta = ObjectTreeDelta.fromItemDelta(right);

        this.strategy = strategy;
    }

    private void initialize() {
        ObjectDelta<O> baseToLeft = base.diff(left, strategy);
        ObjectDelta<O> baseToRight = base.diff(right, strategy);

        leftDelta = ObjectTreeDelta.fromItemDelta(baseToLeft);
        rightDelta = ObjectTreeDelta.fromItemDelta(baseToRight);
    }

    public PrismObject<O> getBase() {
        return base;
    }

    public PrismObject<O> getLeft() {
        return left;
    }

    public ObjectTreeDelta<O> getLeftDelta() {
        return leftDelta;
    }

    public PrismObject<O> getRight() {
        return right;
    }

    public ObjectTreeDelta<O> getRightDelta() {
        return rightDelta;
    }

    public ParameterizedEquivalenceStrategy getStrategy() {
        return strategy;
    }

    public Collection<? extends ItemDelta<?, ?>> getNonConflictingModifications(Direction direction) {
        ObjectTreeDelta<O> delta = direction == Direction.FROM_LEFT ? leftDelta : rightDelta;
        ObjectTreeDelta<O> other = direction == Direction.FROM_LEFT ? rightDelta : leftDelta;

        return delta.getNonConflictingModifications(other, strategy);
    }

    public ObjectDelta<O> getNonConflictingDelta(Direction direction) {
        Collection modifications = getNonConflictingModifications(direction);

        ObjectDelta<O> delta = base.createModifyDelta();
        delta.addModifications(modifications);

        return delta;
    }

    public Collection<Conflict> getConflictingModifications() {
        return leftDelta.getConflictsWith(rightDelta, strategy);
    }

    public boolean hasConflicts() {
        return !getConflictingModifications().isEmpty();
    }
}
