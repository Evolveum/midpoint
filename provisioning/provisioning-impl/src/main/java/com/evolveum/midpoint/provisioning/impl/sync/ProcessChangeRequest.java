/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
