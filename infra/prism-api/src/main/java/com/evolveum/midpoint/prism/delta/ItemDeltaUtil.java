/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.delta;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ModificationTypeType;

/**
 *
 */
public class ItemDeltaUtil {

    public static boolean isEmpty(ItemDeltaType itemDeltaType) {
        if (itemDeltaType == null) {
            return true;
        }
        if (itemDeltaType.getModificationType() == ModificationTypeType.REPLACE) {
            return false;
        }
        return !itemDeltaType.getValue().isEmpty();
    }

    public static <IV extends PrismValue,ID extends ItemDefinition> PrismValueDeltaSetTriple<IV> toDeltaSetTriple(
            Item<IV, ID> item,
            ItemDelta<IV, ID> delta, PrismContext prismContext) throws SchemaException {
        if (item == null && delta == null) {
            return null;
        }
        if (delta == null) {
            PrismValueDeltaSetTriple<IV> triple = prismContext.deltaFactory().createPrismValueDeltaSetTriple();
            triple.addAllToZeroSet(PrismValueCollectionsUtil.cloneCollection(item.getValues()));
            return triple;
        }
        return delta.toDeltaSetTriple(item);
    }

    // TODO move to Item
    public static <V extends PrismValue, D extends ItemDefinition> ItemDelta<V, D> createAddDeltaFor(Item<V, D> item) {
        ItemDelta<V, D> rv = item.createDelta(item.getPath());
        rv.addValuesToAdd(item.getClonedValues());
        return rv;
    }

    // TODO move to Item
    @SuppressWarnings("unchecked")
    public static <V extends PrismValue, D extends ItemDefinition> ItemDelta<V, D> createAddDeltaFor(Item<V, D> item,
            PrismValue value) {
        ItemDelta<V, D> rv = item.createDelta(item.getPath());
        rv.addValueToAdd((V) CloneUtil.clone(value));
        return rv;
    }

}
