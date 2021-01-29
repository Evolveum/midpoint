/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class SqaleUtils {

    /**
     * Returns version from midPoint object as a number.
     * Returns 0 for any non-number version, never returns null.
     */
    public static int objectVersionAsInt(ObjectType schemaObject) {
        String version = schemaObject.getVersion();
        if (version != null) {
            try {
                return Integer.parseInt(version);
            } catch (NumberFormatException e) {
                // ignorable, version will be 0
            }
        }
        return 0;
    }

    /**
     * Returns version from prism object as a number.
     * Returns 0 for any non-number version, never returns null.
     */
    public static int objectVersionAsInt(PrismObject<?> prismObject) {
        String version = prismObject.getVersion();
        if (version != null) {
            try {
                return Integer.parseInt(version);
            } catch (NumberFormatException e) {
                // ignorable, version will be 0
            }
        }
        return 0;
    }
}
