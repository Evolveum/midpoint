/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.marshaller;

import com.evolveum.midpoint.prism.path.ItemPath;

/**
 *  TEMPORARY. WILL BE RESOLVED SOMEHOW.
 *
 *  See MID-6320.
 */
public class ItemPathSerializerTemp {
    public static String serializeWithDeclarations(ItemPath path) {
        return ItemPathHolder.serializeWithDeclarations(path);
    }

}
