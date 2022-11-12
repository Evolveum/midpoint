/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.errors;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.provisioning.impl.shadows.ProvisioningOperationState;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SecurityViolationException;

@Component
class SecurityViolationHandler extends HardErrorHandler {

    @Override
    protected void throwException(Exception cause, ProvisioningOperationState<?> opState, OperationResult result)
            throws SecurityViolationException {
        recordCompletionError(cause, opState, result);
        if (cause instanceof SecurityViolationException) {
            throw (SecurityViolationException)cause;
        } else {
            throw new SecurityViolationException(cause.getMessage(), cause);
        }
    }
}
