/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
