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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public interface UpgradeObjectProcessor<T extends Objectable> {

    default String getIdentifier() {
        return getClass().getSimpleName().replaceFirst("Processor$", "");
    }

    UpgradePhase getPhase();

    UpgradePriority getPriority();

    UpgradeType getType();

    boolean isApplicable(PrismObject<?> object, ItemPath path);

    boolean process(PrismObject<T> object, ItemPath path);

    default <O extends ObjectType> boolean matchesTypeAndHasPathItem(PrismObject<?> object, ItemPath path, Class<O> type) {
        if (!type.isAssignableFrom(object.getCompileTimeClass())) {
            return false;
        }

        Item item = object.findItem(path);
        if (item == null || item.isEmpty()) {
            return false;
        }

        return true;
    }
}
