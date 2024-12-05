/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.errors;

import com.evolveum.midpoint.provisioning.impl.shadows.ShadowProvisioningOperation;

import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;

@Component
class SchemaExceptionHandler extends HardErrorHandler {

    @Override
    protected void throwException(@Nullable ShadowProvisioningOperation operation, Exception cause, OperationResult result)
            throws SchemaException {
        recordCompletionError(operation, cause, result);
        if (cause instanceof SchemaException schemaException) {
            throw schemaException;
        } else {
            throw new SchemaException(cause.getMessage(), cause);
        }
    }
}
