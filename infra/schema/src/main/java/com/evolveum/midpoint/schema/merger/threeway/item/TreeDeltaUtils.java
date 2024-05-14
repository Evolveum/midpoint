/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.threeway.item;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ModificationType;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class TreeDeltaUtils {

    public static <O extends ObjectType> ThreeWayMerge<O> createThreeWayMerge(
            PrismObject<O> base, PrismObject<O> left, PrismObject<O> right) {

        ObjectDelta<O> baseToLeft = base.diff(left);
        ObjectDelta<O> baseToRight = base.diff(right);

        ObjectTreeDelta<O> leftDelta = ObjectTreeDelta.fromItemDelta(baseToLeft);
        ObjectTreeDelta<O> rightDelta = ObjectTreeDelta.fromItemDelta(baseToRight);

        return new ThreeWayMerge<>(leftDelta, rightDelta, base);

    }

    public static void addItemTreeDeltaValue(@NotNull ItemDelta delta, @NotNull ItemTreeDeltaValue value) {
        ModificationType modificationType = value.getModificationType();
        PrismValue prismValue = value.getValue();
        if (modificationType == null || prismValue == null) {
            return;
        }

        switch (modificationType) {
            case ADD -> delta.addValueToAdd(prismValue);
            case DELETE -> delta.addValueToDelete(prismValue);
            case REPLACE -> delta.addValueToReplace(prismValue);
        }
    }
}
