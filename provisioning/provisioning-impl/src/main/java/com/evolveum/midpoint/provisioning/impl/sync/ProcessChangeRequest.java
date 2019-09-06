/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.sync;

import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SystemException;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

/**
 * Wrapper for "process change" request and its execution.
 */
public class ProcessChangeRequest {

    @NotNull private final Change change;
    private final ProvisioningContext globalContext;
    private final boolean simulate;
    private boolean success;

    public ProcessChangeRequest(@NotNull Change change, ProvisioningContext globalContext, boolean simulate) {
        Validate.notNull(change, "change");
        this.change = change;
        this.globalContext = globalContext;
        this.simulate = simulate;
    }

    public Change getChange() {
        return change;
    }

    public ProvisioningContext getGlobalContext() {
        return globalContext;
    }

    public boolean isSimulate() {
        return simulate;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void onSuccess() {
    }

    public void onError(OperationResult result) {
        // Probably nothing to do here. The error is already recorded in operation result.
    }

    public void onError(Throwable t, OperationResult result) {
        // TODO consider better exception reporting
        throw new SystemException(t.getMessage(), t);
    }

    public Object getPrimaryIdentifierRealValue() {
        return change.getPrimaryIdentifierRealValue();
    }

    @Override
    public String toString() {
        return "ProcessChangeRequest{" +
                "change=" + change +
                ", success=" + success +
                '}';
    }
}
