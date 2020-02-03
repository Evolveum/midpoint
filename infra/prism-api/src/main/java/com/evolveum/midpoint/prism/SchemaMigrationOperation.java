/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 */
public enum SchemaMigrationOperation {

    REMOVED, MOVED, RENAMED_PARENT;

    public static SchemaMigrationOperation parse(String s) throws SchemaException {
        if (s == null) {
            return null;
        }
        switch (s) {
            case "removed":
                return REMOVED;
            case "moved":
                return MOVED;
            case "renamedParent":
                return RENAMED_PARENT;
            default:
                throw new SchemaException("Unknown schema migration operation "+s);
        }
    }

}
