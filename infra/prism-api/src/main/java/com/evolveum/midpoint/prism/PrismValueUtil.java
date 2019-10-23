/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.xnode.*;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 *
 */
public class PrismValueUtil {

    public static PrismContainerValue<?> getParentContainerValue(PrismValue value) {
        Itemable parent = value.getParent();
        if (parent instanceof Item) {
            PrismValue parentParent = ((Item) parent).getParent();
            return parentParent instanceof PrismContainerValue ? (PrismContainerValue) parentParent : null;
        } else {
            return null;
        }
    }

    public static <T> PrismProperty<T> createRaw(@NotNull XNode node, @NotNull QName itemName, PrismContext prismContext)
            throws SchemaException {
        Validate.isTrue(!(node instanceof RootXNode));
        PrismProperty<T> property = prismContext.itemFactory().createProperty(itemName);
        if (node instanceof ListXNode) {
            for (XNode subnode : ((ListXNode) node).asList()) {
                property.add(createRaw(subnode, prismContext));
            }
        } else {
            property.add(createRaw(node, prismContext));
        }
        return property;
    }

    private static <T> PrismPropertyValue<T> createRaw(XNode rawElement, PrismContext prismContext) {
        return prismContext.itemFactory().createPropertyValue(rawElement);
    }

    public static boolean differentIds(PrismValue v1, PrismValue v2) {
        Long id1 = v1 instanceof PrismContainerValue ? ((PrismContainerValue) v1).getId() : null;
        Long id2 = v2 instanceof PrismContainerValue ? ((PrismContainerValue) v2).getId() : null;
        return id1 != null && id2 != null && id1.longValue() != id2.longValue();
    }
}
