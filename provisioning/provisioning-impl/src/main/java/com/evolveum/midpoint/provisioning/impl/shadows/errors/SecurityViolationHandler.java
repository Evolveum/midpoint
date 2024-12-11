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
import com.evolveum.midpoint.util.exception.SecurityViolationException;

@Component
class SecurityViolationHandler extends HardErrorHandler {

    @Override
    protected void throwException(@Nullable ShadowProvisioningOperation operation, Exception cause, OperationResult result)
            throws SecurityViolationException {
        recordCompletionError(operation, cause, result);
        if (cause instanceof SecurityViolationException securityViolationException) {
            throw securityViolationException;
        } else {
            throw new SecurityViolationException(cause.getMessage(), cause);
        }
    }
}
