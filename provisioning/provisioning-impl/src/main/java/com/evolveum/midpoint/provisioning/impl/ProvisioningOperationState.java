/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 */
public class ProvisioningOperationState<A extends AsynchronousOperationResult> implements ShortDumpable {

    private A asyncResult;
    private PendingOperationExecutionStatusType executionStatus;
    private PrismObject<ShadowType> repoShadow;
    private Integer attemptNumber;
    private List<PendingOperationType> pendingOperations;

    public A getAsyncResult() {
        return asyncResult;
    }

    public void setAsyncResult(A asyncResult) {
        this.asyncResult = asyncResult;
    }

    public PendingOperationExecutionStatusType getExecutionStatus() {
        return executionStatus;
    }

    public void setExecutionStatus(PendingOperationExecutionStatusType executionStatus) {
        this.executionStatus = executionStatus;
    }

    public PrismObject<ShadowType> getRepoShadow() {
        return repoShadow;
    }

    public void setRepoShadow(PrismObject<ShadowType> repoShadow) {
        this.repoShadow = repoShadow;
    }

    public List<PendingOperationType> getPendingOperations() {
        return pendingOperations;
    }

    public boolean hasPendingOperations() {
        return pendingOperations != null;
    }

    public void addPendingOperation(PendingOperationType pendingOperation) {
        if (pendingOperations == null) {
            pendingOperations = new ArrayList<>();
        }
        pendingOperations.add(pendingOperation);
    }

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public PendingOperationTypeType getOperationType() {
        if (asyncResult == null) {
            return null;
        } else {
            return asyncResult.getOperationType();
        }
    }

    public boolean isQuantumOperation() {
        if (asyncResult == null) {
            return false;
        } else {
            return asyncResult.isQuantumOperation();
        }
    }

    /**
     * Returns true if the operation was started. It returns true
     * if the operation is executing (in progress) or finished.
     */
    public boolean wasStarted() {
        return executionStatus == PendingOperationExecutionStatusType.EXECUTING || executionStatus == PendingOperationExecutionStatusType.COMPLETED;
    }

    public boolean isCompleted() {
        return executionStatus == PendingOperationExecutionStatusType.COMPLETED;
    }

    public boolean isExecuting() {
        return executionStatus == PendingOperationExecutionStatusType.EXECUTING;
    }

    public boolean isSuccess() {
        return OperationResultStatusType.SUCCESS.equals(getResultStatusType());
    }


    public OperationResultStatusType getResultStatusType() {
        OperationResultStatus resultStatus = getResultStatus();
        if (resultStatus == null) {
            return null;
        }
        return resultStatus.createStatusType();
    }

    public OperationResultStatus getResultStatus() {
        if (asyncResult == null || asyncResult.getOperationResult() == null) {
            return null;
        } else {
            return asyncResult.getOperationResult().getStatus();
        }
    }

    public String getAsynchronousOperationReference() {
        if (asyncResult == null || asyncResult.getOperationResult() == null) {
            return null;
        } else {
            return asyncResult.getOperationResult().getAsynchronousOperationReference();
        }
    }

    public void processAsyncResult(A asyncReturnValue) {
        setAsyncResult(asyncReturnValue);
        if (asyncReturnValue == null) {
            return;
        }
        OperationResult operationResult = asyncReturnValue.getOperationResult();
        if (operationResult == null) {
            return;
        }
        if (operationResult.isInProgress()) {
            executionStatus = PendingOperationExecutionStatusType.EXECUTING;
        } else {
            executionStatus = PendingOperationExecutionStatusType.COMPLETED;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ProvisioningOperationState(");
        shortDump(sb);
        sb.append(")");
        return sb.toString();
    }

    @Override
    public void shortDump(StringBuilder sb) {
        sb.append(executionStatus);
        if (attemptNumber != null) {
            sb.append(", attempt #").append(attemptNumber);
        }
        if (pendingOperations != null) {
            sb.append(", ").append(pendingOperations.size()).append(" pending operations");
        }
        if (asyncResult != null) {
            sb.append(", result: ");
            asyncResult.shortDump(sb);
        }
    }

    public void determineExecutionStatusFromResult() {
        if (asyncResult == null) {
            throw new IllegalStateException("Cannot determine execution status from null result");
        }
        OperationResult operationResult = asyncResult.getOperationResult();
        if (operationResult == null) {
            throw new IllegalStateException("Cannot determine execution status from null result");
        }
        OperationResultStatus status = operationResult.getStatus();
        if (status == null) {
            executionStatus = PendingOperationExecutionStatusType.REQUESTED;
        } else if (status == OperationResultStatus.IN_PROGRESS) {
            executionStatus = PendingOperationExecutionStatusType.EXECUTING;
        } else {
            executionStatus = PendingOperationExecutionStatusType.COMPLETED;
        }
    }

    // TEMPORARY: TODO: remove
    public static <A extends AsynchronousOperationResult> ProvisioningOperationState<A> fromPendingOperation(
            PrismObject<ShadowType> repoShadow, PendingOperationType pendingOperation) {
        List<PendingOperationType> pendingOperations = new ArrayList<>();
        pendingOperations.add(pendingOperation);
        return fromPendingOperations(repoShadow, pendingOperations);
    }

    public static <A extends AsynchronousOperationResult> ProvisioningOperationState<A> fromPendingOperations(
            PrismObject<ShadowType> repoShadow, List<PendingOperationType> pendingOperations) {
        ProvisioningOperationState<A> opState = new ProvisioningOperationState<>();
        if (pendingOperations == null || pendingOperations.isEmpty()) {
            throw new IllegalArgumentException("Empty list of pending operations, cannot create ProvisioningOperationState");
        }
        opState.pendingOperations = pendingOperations;
        // TODO: check that they have the same status
        opState.executionStatus = pendingOperations.get(0).getExecutionStatus();
        // TODO: better algorithm
        opState.attemptNumber = pendingOperations.get(0).getAttemptNumber();
        opState.repoShadow = repoShadow;
        return opState;
    }

}
