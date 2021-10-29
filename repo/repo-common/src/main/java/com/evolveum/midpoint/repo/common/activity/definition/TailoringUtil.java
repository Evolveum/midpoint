/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import com.evolveum.midpoint.prism.Containerable;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityControlFlowDefinitionTailoringType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TailoringModeType;

import com.google.common.base.MoreObjects;
import org.jetbrains.annotations.NotNull;

class TailoringUtil {

    /**
     * We assume the same property name is used in all "tailoring" objects.
     */
    private static final ItemName F_TAILORING_MODE = ActivityControlFlowDefinitionTailoringType.F_TAILORING_MODE;

    static <T extends Containerable> @NotNull T getTailoredBean(@NotNull T original, @NotNull T tailoring) {
        TailoringModeType mode = getTailoringMode(tailoring.asPrismContainerValue());
        switch (mode) {
            case OVERWRITE:
                return tailoring.cloneWithoutId();
            case OVERWRITE_SPECIFIED:
                return overwriteSpecified(original, tailoring);
            default:
                throw new AssertionError(mode);
        }
    }

    private static @NotNull TailoringModeType getTailoringMode(PrismContainerValue<?> tailoring) {
        return MoreObjects.firstNonNull(
                tailoring.getPropertyRealValue(F_TAILORING_MODE, TailoringModeType.class),
                TailoringModeType.OVERWRITE);
    }

    private static <T extends Containerable> T overwriteSpecified(T original, T tailoring) {
        //noinspection unchecked
        PrismContainerValue<T> originalPcv = original.asPrismContainerValue();
        //noinspection unchecked
        PrismContainerValue<T> tailoringPcv = tailoring.asPrismContainerValue();

        for (Item<?, ?> itemToOverwrite : tailoringPcv.getItems()) {
            if (QNameUtil.match(itemToOverwrite.getElementName(), F_TAILORING_MODE)) {
                continue;
            }
            try {
                //noinspection unchecked
                originalPcv.addReplaceExisting(itemToOverwrite.clone());
            } catch (SchemaException e) {
                throw new IllegalStateException("Unexpected SchemaException during activity tailoring: " + e.getMessage(), e);
            }
        }
        return originalPcv.asContainerable();
    }
}
