/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.shadows;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.createStatusType;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType.COMPLETED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType.EXECUTING;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * Represents a state of a provisioning add/modify/delete operation in the `shadows` package. (See its subclasses.)
 *
 * @author semancik
 */
public abstract class ProvisioningOperationState<A extends AsynchronousOperationResult> implements ShortDumpable {

    /**
     * Result of the operation. It contains the following:
     *
     * - {@link OperationResult} corresponding to the operation.
     * - {@link PendingOperationTypeType}: null, manual, retry, asynchronous: information that ultimately will be put into
     * the pending operation bean; it is set e.g. by the built-in asynchronous/manual connector or by "operation postponing code".
     * - `quantum operation` flag: currently seems to be not used.
     * - `return value`
     * ** e.g. {@link ShadowType} that is going or was added (for "add" operation)
     * ** or a collection of deltas (for "modify" operation - TODO what is that?)
     *
     * It comes either from the lower layers ({@link ResourceObjectConverter} or when the operation is postponed.
     */
    private A asyncResult;

    /**
     * Current status of this operation: requested, pending, executing, completed.
     * Corresponds to pending operation bean connected to this operation.
     */
    private PendingOperationExecutionStatusType executionStatus;

    /**
     * Repository shadow connected to the operation. Starts non-null for modify and delete operations.
     * (Although may be zeroed during the operation execution.)
     */
    private ShadowType repoShadow;

    /**
     * What is the number of the current attempt? Retrieved from pending operation, stored into pending operation.
     */
    private Integer attemptNumber;

    /**
     * TODO - this is a bit incomprehensible... analyze & document it!
     */
    private List<PendingOperationType> pendingOperations;

    public ProvisioningOperationState() {
    }

    public ProvisioningOperationState(ShadowType repoShadow) {
        this.repoShadow = repoShadow;
    }

    A getAsyncResult() {
        return asyncResult;
    }

    private OperationResult getOperationResult() {
        return asyncResult != null ? asyncResult.getOperationResult() : null;
    }

    public OperationResultStatus getResultStatus() {
        OperationResult operationResult = getOperationResult();
        return operationResult != null ? operationResult.getStatus() : null;
    }

    public boolean isSuccess() {
        return getResultStatus() == OperationResultStatus.SUCCESS;
    }

    public OperationResultStatusType getResultStatusType() {
        return createStatusType(
                getResultStatus());
    }

    public PendingOperationTypeType getOperationType() {
        return asyncResult != null ? asyncResult.getOperationType() : null;
    }

    void markToRetry(A asyncResult, OperationResult failedOperationResult) {
        asyncResult.setOperationResult(failedOperationResult);
        asyncResult.setOperationType(PendingOperationTypeType.RETRY);
        this.asyncResult = asyncResult;
        executionStatus = EXECUTING;
        if (attemptNumber == null) {
            attemptNumber = 1;
        }
    }

    public PendingOperationExecutionStatusType getExecutionStatus() {
        return executionStatus;
    }

    public void setExecutionStatus(PendingOperationExecutionStatusType executionStatus) {
        this.executionStatus = executionStatus;
    }

    public ShadowType getRepoShadow() {
        return repoShadow;
    }

    public void setRepoShadow(ShadowType repoShadow) {
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

    public int getRealAttemptNumber() {
        return Objects.requireNonNullElse(attemptNumber, 1);
    }

    /**
     * Returns true if the operation was started, i.e. if it is executing (in progress) or completed.
     */
    public boolean wasStarted() {
        return executionStatus == EXECUTING || executionStatus == COMPLETED;
    }

    public boolean isCompleted() {
        return executionStatus == COMPLETED;
    }

    public boolean isExecuting() {
        return executionStatus == EXECUTING;
    }

    public String getAsynchronousOperationReference() {
        OperationResult operationResult = getOperationResult();
        return operationResult != null ? operationResult.getAsynchronousOperationReference() : null;
    }

    /** This method is called when we get the real asynchronous result from the (attempted) operation execution. */
    void recordRealAsynchronousResult(A asyncResult) {
        this.asyncResult = asyncResult;
        OperationResult operationResult = getOperationResult();
        if (operationResult == null) {
            // No effect
        } else if (operationResult.isInProgress()) {
            executionStatus = EXECUTING;
        } else {
            executionStatus = COMPLETED;
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

    void determineExecutionStatusFromResult() {
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
            executionStatus = EXECUTING;
        } else {
            executionStatus = COMPLETED;
        }
    }

    private static <X extends ProvisioningOperationState<?>> X fromPendingOperationsInternal(
            ShadowType repoShadow, List<PendingOperationType> pendingOperations, Function<ShadowType, X> supplier) {
        if (pendingOperations == null || pendingOperations.isEmpty()) {
            throw new IllegalArgumentException("Empty list of pending operations, cannot create ProvisioningOperationState");
        }
        X typedOpState = supplier.apply(repoShadow);
        ProvisioningOperationState<?> opState = typedOpState;
        opState.pendingOperations = new ArrayList<>(pendingOperations);
        // TODO: check that they have the same status
        opState.executionStatus = pendingOperations.get(0).getExecutionStatus();
        // TODO: better algorithm
        opState.attemptNumber = pendingOperations.get(0).getAttemptNumber();
        return typedOpState;
    }

    boolean objectExists() {
        if (repoShadow != null) {
            return ShadowUtil.isExists(repoShadow);
        } else {
            return false; // shouldn't occur
        }
    }

    /** Creates a {@link PendingOperationType} that represents the result/status of this operation. */
    public PendingOperationType toPendingOperation(
            ObjectDelta<ShadowType> requestDelta, String asyncOperationReferenceOverride, XMLGregorianCalendar now) throws SchemaException {
        ObjectDeltaType deltaType = DeltaConvertor.toObjectDeltaType(requestDelta);
        PendingOperationType pendingOperation = new PendingOperationType();
        pendingOperation.setType(getOperationType());
        pendingOperation.setDelta(deltaType);
        pendingOperation.setRequestTimestamp(now);
        if (executionStatus == EXECUTING) {
            pendingOperation.setOperationStartTimestamp(now);
        }
        pendingOperation.setExecutionStatus(executionStatus);
        pendingOperation.setResultStatus(getResultStatusType());
        if (attemptNumber != null) {
            pendingOperation.setAttemptNumber(attemptNumber);
            pendingOperation.setLastAttemptTimestamp(now);
        }
        if (asyncOperationReferenceOverride != null) {
            pendingOperation.setAsynchronousOperationReference(asyncOperationReferenceOverride);
        } else {
            pendingOperation.setAsynchronousOperationReference(getAsynchronousOperationReference());
        }
        return pendingOperation;
    }

    public static class AddOperationState
            extends ProvisioningOperationState<AsynchronousOperationReturnValue<ShadowType>> {

        public AddOperationState() {
        }

        public AddOperationState(@NotNull ShadowType repoShadow) {
            super(repoShadow);
        }

        static @NotNull AddOperationState fromPendingOperations(
                @NotNull ShadowType repoShadow, @NotNull List<PendingOperationType> pendingOperations) {
            return fromPendingOperationsInternal(repoShadow, pendingOperations, AddOperationState::new);
        }

        static @NotNull AddOperationState fromPendingOperation(
                @NotNull ShadowType repoShadow, @NotNull PendingOperationType pendingOperation) {
            return fromPendingOperations(repoShadow, List.of(pendingOperation));
        }

        public OperationResultStatus markToRetry(OperationResult failedOperationResult) {
            this.markToRetry(new AsynchronousOperationReturnValue<>(), failedOperationResult);
            return OperationResultStatus.IN_PROGRESS;
        }

        public ShadowType getReturnedShadow() {
            AsynchronousOperationReturnValue<ShadowType> aResult = getAsyncResult();
            return aResult != null ? aResult.getReturnValue() : null;
        }
    }

    public static class ModifyOperationState
            extends ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue<?>>>>> {

        ModifyOperationState(@NotNull ShadowType repoShadow) {
            super(repoShadow);
        }

        static @NotNull ModifyOperationState fromPendingOperation(
                @NotNull ShadowType repoShadow, @NotNull PendingOperationType pendingOperation) {
            return fromPendingOperationsInternal(repoShadow, List.of(pendingOperation), ModifyOperationState::new);
        }

        public OperationResultStatus markToRetry(OperationResult failedOperationResult) {
            this.markToRetry(
                    new AsynchronousOperationReturnValue<>(),
                    failedOperationResult);
            return OperationResultStatus.IN_PROGRESS;
        }
    }

    public static class DeleteOperationState
            extends ProvisioningOperationState<AsynchronousOperationResult> {

        DeleteOperationState(@NotNull ShadowType repoShadow) {
            super(repoShadow);
        }

        static @NotNull DeleteOperationState fromPendingOperation(
                @NotNull ShadowType repoShadow, @NotNull PendingOperationType pendingOperation) {
            return fromPendingOperationsInternal(repoShadow, List.of(pendingOperation), DeleteOperationState::new);
        }

        public OperationResultStatus markToRetry(OperationResult failedOperationResult) {
            this.markToRetry(new AsynchronousOperationResult(), failedOperationResult);
            return OperationResultStatus.IN_PROGRESS;
        }
    }

}
