/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.validator;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.validator.processor.ProcessorMixin;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public interface UpgradeObjectProcessor<T extends Objectable> extends ProcessorMixin {

    default String getIdentifier() {
        return getIdentifier(getClass());
    }

    UpgradePhase getPhase();

    UpgradePriority getPriority();

    UpgradeType getType();

    boolean isApplicable(PrismObject<?> object, ItemPath path);

    boolean process(PrismObject<T> object, ItemPath path);

    /**
     * Matches object type and path template (without container ids in case of multivalue containers.
     *
     * @param object tested object
     * @param path validation item path
     * @param type expected type (ObjectType)
     * @param expected exptected path template
     * @return true if matches
     *
     * @param <O>
     */
    default <O extends ObjectType> boolean matchObjectTypeAndPathTemplate(PrismObject<?> object, ItemPath path, Class<O> type, ItemPath expected) {
        if (!type.isAssignableFrom(object.getCompileTimeClass())) {
            return false;
        }

        if (!path.namedSegmentsOnly().equivalent(expected)) {
            return false;
        }

        Item item = object.findItem(path);
        if (item == null || item.isEmpty()) {
            return false;
        }

        return true;
    }
}
