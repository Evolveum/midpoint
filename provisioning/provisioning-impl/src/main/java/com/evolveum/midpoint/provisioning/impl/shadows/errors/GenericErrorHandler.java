/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.errors;

import com.evolveum.midpoint.provisioning.impl.shadows.ShadowProvisioningOperation;

import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.schema.result.OperationResult;

@Component
class GenericErrorHandler extends HardErrorHandler {

    @Override
    protected void throwException(@Nullable ShadowProvisioningOperation operation, Exception cause, OperationResult result)
            throws GenericConnectorException {
        recordCompletionError(operation, cause, result);
        if (cause instanceof GenericConnectorException genericConnectorException) {
            throw genericConnectorException;
        } else {
            throw new GenericConnectorException(cause.getMessage(), cause);
        }
    }

}
