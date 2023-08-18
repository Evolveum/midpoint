/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.task.work;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSetType;

public class ObjectSetUtil {

    /**
     * Fills-in the expected type or checks that provided one is not contradicting it.
     */
    public static void assumeObjectType(@NotNull ObjectSetType set, @NotNull QName superType) {
        if (superType.equals(set.getType())) {
            return;
        }
        if (set.getType() == null || QNameUtil.match(set.getType(), superType)) {
            set.setType(superType);
            return;
        }
        argCheck(PrismContext.get().getSchemaRegistry().isAssignableFrom(superType, set.getType()),
                "Activity requires object type of %s, but %s was provided in the work definition",
                superType, set.getType());
    }

    public static void applyDefaultObjectType(@NotNull ObjectSetType set, @NotNull QName type) {
        if (set.getType() == null) {
            set.setType(type);
        }
    }

    public static @NotNull ObjectSetType emptyIfNull(ObjectSetType configured) {
        return configured != null ? configured : new ObjectSetType();
    }
}
