/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.delta;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

import java.util.Collection;

/**
 *
 */
public class ObjectDeltaUtil {
    public static boolean isEmpty(ObjectDeltaType deltaType) {
        if (deltaType == null) {
            return true;
        }
        if (deltaType.getChangeType() == ChangeTypeType.DELETE) {
            return false;
        } else if (deltaType.getChangeType() == ChangeTypeType.ADD) {
            return deltaType.getObjectToAdd() == null || deltaType.getObjectToAdd().asPrismObject().isEmpty();
        } else {
            for (ItemDeltaType itemDeltaType : deltaType.getItemDelta()) {
                if (!ItemDeltaUtil.isEmpty(itemDeltaType)) {
                    return false;
                }
            }
            return true;
        }
    }

    public static <O extends Objectable> void applyTo(
            PrismObject<O> targetObject, Collection<? extends ItemDelta<?,?>> modifications) throws SchemaException {
        for (ItemDelta itemDelta : modifications) {
            itemDelta.applyTo(targetObject);
        }
    }
}
