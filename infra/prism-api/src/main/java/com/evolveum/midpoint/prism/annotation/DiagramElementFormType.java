/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.annotation;

import com.evolveum.midpoint.util.exception.SchemaException;

public enum DiagramElementFormType {
    EXPANDED, COLLAPSED;

    public static DiagramElementFormType parse(String s) throws SchemaException {
        if (s == null) {
            return null;
        }
        switch (s) {
            case "expanded":
                return EXPANDED;
            case "collapsed":
                return COLLAPSED;
            default:
                throw new SchemaException("Unknown diagram form "+s);
        }
    }
}
