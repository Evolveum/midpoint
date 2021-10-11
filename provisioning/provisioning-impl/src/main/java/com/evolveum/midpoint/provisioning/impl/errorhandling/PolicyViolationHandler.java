/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.errorhandling;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.provisioning.impl.ProvisioningOperationState;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;

@Component
public class PolicyViolationHandler extends HardErrorHandler {

    @Override
    protected void throwException(Exception cause, ProvisioningOperationState<? extends AsynchronousOperationResult> opState, OperationResult result)
            throws PolicyViolationException {
        recordCompletionError(cause, opState, result);
        if (cause instanceof PolicyViolationException) {
            throw (PolicyViolationException)cause;
        } else {
            throw new PolicyViolationException(cause.getMessage(), cause);
        }
    }

}
