/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.marshaller;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 *  TEMPORARY. WILL BE RESOLVED SOMEHOW.
 *
 *  See MID-6320.
 */
public class ItemPathSerializerTemp {
    public static String serializeWithDeclarations(ItemPath path) {
        return ItemPathHolder.serializeWithDeclarations(path);
    }

    public static String serializeWithForcedDeclarations(ItemPath path) {
        return ItemPathHolder.serializeWithForcedDeclarations(path);
    }

    public static String serializeWithForcedDeclarations(ItemPathType value) {
        return ItemPathHolder.serializeWithForcedDeclarations(value.getItemPath());
    }
}
