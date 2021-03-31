/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.annotation;

import com.evolveum.midpoint.util.exception.SchemaException;

public enum DiagramElementInclusionType {
    AUTO, INCLUDE, EXCLUDE;

    public static DiagramElementInclusionType parse(String s) throws SchemaException {
        if (s == null) {
            return null;
        }
        switch (s) {
            case "auto":
                return AUTO;
            case "include":
                return INCLUDE;
            case "exclude":
                return EXCLUDE;
            default:
                throw new SchemaException("Unknown diagram inclusion "+s);
        }
    }
}
