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

    /**
     * @return Unique identifier of the processor. By default it is class name without "Processor" suffix.
     */
    default String getIdentifier() {
        return getIdentifier(getClass());
    }

    UpgradePhase getPhase();

    UpgradePriority getPriority();

    UpgradeType getType();

    /**
     * Checks if the processor is applicable for the object and path.
     * Most often whether object is instance of proper ObjectType and item at the path exists.
     */
    boolean isApplicable(PrismObject<?> object, ItemPath path);

    /**
     * Executes upgrade of item defined by path argument by modifying the object to correct state.
     */
    boolean process(PrismObject<T> object, ItemPath path);

    /**
     * Matches object type and path template (without container ids in case of multivalue containers.
     *
     * @param object tested object
     * @param path validation item path
     * @param type expected type (ObjectType)
     * @param expected exptected path template
     * @param <O>
     * @return true if matches
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
