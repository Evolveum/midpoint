/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * This exception exists only for the purpose of passing checked {@link SchemaException}s where they are not allowed.
 * This comes handy e.g. in callbacks. This exception must never be shown to "outside".
 *
 * @author Radovan Semancik
 */
class IntermediateSchemaException extends RuntimeException {

    IntermediateSchemaException(SchemaException cause) {
        super(cause);
    }

    SchemaException getSchemaException() {
        return (SchemaException) getCause();
    }
}
