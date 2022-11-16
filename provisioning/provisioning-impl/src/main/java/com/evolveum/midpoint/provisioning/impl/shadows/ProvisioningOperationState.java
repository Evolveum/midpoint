/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.shadows;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.createStatusType;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType.COMPLETED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType.EXECUTING;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationTypeType.RETRY;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.util.MiscUtil;

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
     * Status of the [pending] operation that is to be applied if nothing can be determined from {@link #asyncResult}.
     * Used for error handling.
     */
    private OperationResultStatus defaultResultStatus;

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
     * The timestamp of the last executed attempt - set only if it has to be updated in the repository.
     */
    private XMLGregorianCalendar lastAttemptTimestamp;

    /**
     * The pending operation in the shadow that this operation is an execution of (first or repeated).
     * It will be updated with the actual result of the execution.
     *
     * NOTE: This _excludes_ the propagation operation. In that case, we aggregate all waiting operations and execute them
     * as a single delta. The code for updating the shadow is (currently) distinct from the one that uses this field.
     * See {@link #propagatedPendingOperations}.
     */
    private PendingOperationType currentPendingOperation;

    /**
     * List of pending operations executed during a propagation operation.
     * These are updated in a special way.
     */
    private List<PendingOperationType> propagatedPendingOperations;

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

    public void setDefaultResultStatus(OperationResultStatus defaultResultStatus) {
        this.defaultResultStatus = defaultResultStatus;
    }

    private OperationResultStatus getResultStatusOrDefault() {
        return MiscUtil.getFirstNonNull(getResultStatus(), defaultResultStatus);
    }

    public boolean isSuccess() {
        return getResultStatus() == OperationResultStatus.SUCCESS;
    }

    public OperationResultStatusType getResultStatusTypeOrDefault() {
        return createStatusType(
                getResultStatusOrDefault());
    }

    public PendingOperationTypeType getOperationType() {
        return asyncResult != null ? asyncResult.getOperationType() : null;
    }

    public abstract OperationResultStatus markAsPostponed(OperationResult failedOperationResult);

    void markAsPostponed(A asyncResult, OperationResult failedOperationResult) {
        asyncResult.setOperationResult(failedOperationResult);
        asyncResult.setOperationType(RETRY);
        this.asyncResult = asyncResult;
        executionStatus = EXECUTING;
        if (attemptNumber == null) {
            attemptNumber = 1;
        }
        if (lastAttemptTimestamp == null) {
            lastAttemptTimestamp = ShadowsLocalBeans.get().clock.currentTimeXMLGregorianCalendar();
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

    @NotNull
    public ShadowType getRepoShadowRequired() {
        return Objects.requireNonNull(repoShadow, "No repo shadow");
    }

    String getRepoShadowOid() {
        return repoShadow != null ? repoShadow.getOid() : null;
    }

    /**
     * Sets the object as a reference, not as a (cloned) value.
     * All modifications on the original object will be reflected in stored one.
     */
    public void setRepoShadow(ShadowType repoShadow) {
        this.repoShadow = repoShadow;
    }

    public PendingOperationType getCurrentPendingOperation() {
        return currentPendingOperation;
    }

    public boolean hasCurrentPendingOperation() {
        return currentPendingOperation != null;
    }

    public void setCurrentPendingOperation(@NotNull PendingOperationType pendingOperation) {
        stateCheck(currentPendingOperation == null,
                "Current pending operation is already set! %s in %s", currentPendingOperation, repoShadow);
        stateCheck(propagatedPendingOperations == null,
                "Propagated and 'regular' pending operations cannot be mixed: %s vs %s",
                propagatedPendingOperations, pendingOperation);
        currentPendingOperation = pendingOperation;
    }

    public List<PendingOperationType> getPropagatedPendingOperations() {
        return propagatedPendingOperations;
    }

    void setPropagatedPendingOperations(List<PendingOperationType> propagatedPendingOperations) {
        this.propagatedPendingOperations = propagatedPendingOperations;
    }

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public int getRealAttemptNumber() {
        return Objects.requireNonNullElse(attemptNumber, 1);
    }

    public XMLGregorianCalendar getLastAttemptTimestamp() {
        return lastAttemptTimestamp;
    }

    /**
     * Returns true if the operation was started, i.e. if it is executing (in progress) or completed.
     */
    boolean wasStarted() {
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
        if (currentPendingOperation != null) {
            sb.append(", ").append(" has current pending operation");
        }
        if (asyncResult != null) {
            sb.append(", result: ");
            asyncResult.shortDump(sb);
        }
    }

    private static <X extends ProvisioningOperationState<?>> X fromPendingOperationInternal(
            @NotNull ShadowType repoShadow,
            @NotNull PendingOperationType pendingOperation,
            @NotNull Function<ShadowType, X> newOpStateSupplier) {
        X newOpState = newOpStateSupplier.apply(repoShadow);
        newOpState.setCurrentPendingOperation(pendingOperation);
        newOpState.setExecutionStatus(pendingOperation.getExecutionStatus());
        newOpState.setAttemptNumber(pendingOperation.getAttemptNumber());
        return newOpState;
    }

    /**
     * Creates a {@link PendingOperationType} that represents the result/status of this operation. This is one of channels
     * of serializing that information.
     */
    public PendingOperationType toPendingOperation(
            ObjectDelta<ShadowType> requestDelta, String asyncOperationReferenceOverride, XMLGregorianCalendar now)
            throws SchemaException {
        ObjectDeltaType deltaType = DeltaConvertor.toObjectDeltaType(requestDelta);
        PendingOperationType pendingOperation = new PendingOperationType();
        pendingOperation.setType(getOperationType());
        pendingOperation.setDelta(deltaType);
        pendingOperation.setRequestTimestamp(now);
        if (executionStatus == EXECUTING) {
            pendingOperation.setOperationStartTimestamp(now);
        }
        pendingOperation.setExecutionStatus(executionStatus);
        pendingOperation.setResultStatus(getResultStatusTypeOrDefault());
        pendingOperation.setAttemptNumber(attemptNumber);
        pendingOperation.setLastAttemptTimestamp(lastAttemptTimestamp);
        pendingOperation.setAsynchronousOperationReference(
                asyncOperationReferenceOverride != null ? asyncOperationReferenceOverride : getAsynchronousOperationReference());
        return pendingOperation;
    }

    public boolean hasRepoShadow() {
        return repoShadow != null;
    }

    // TODO should we move these XOperationState classes into ShadowXOperation ones?
    public static class AddOperationState
            extends ProvisioningOperationState<AsynchronousOperationReturnValue<ShadowType>> {

        AddOperationState() {
        }

        AddOperationState(@NotNull ShadowType repoShadow) {
            super(repoShadow);
        }

        static @NotNull AddOperationState fromPendingOperation(
                @NotNull ShadowType repoShadow, @NotNull PendingOperationType pendingOperation) {
            return fromPendingOperationInternal(repoShadow, pendingOperation, AddOperationState::new);
        }

        public OperationResultStatus markAsPostponed(OperationResult failedOperationResult) {
            this.markAsPostponed(new AsynchronousOperationReturnValue<>(), failedOperationResult);
            return OperationResultStatus.IN_PROGRESS;
        }

        /** This is a shadow that was created on the resource by the operation. */
        ShadowType getCreatedShadow() {
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
            return fromPendingOperationInternal(repoShadow, pendingOperation, ModifyOperationState::new);
        }

        public OperationResultStatus markAsPostponed(OperationResult failedOperationResult) {
            this.markAsPostponed(
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
            return fromPendingOperationInternal(repoShadow, pendingOperation, DeleteOperationState::new);
        }

        public OperationResultStatus markAsPostponed(OperationResult failedOperationResult) {
            this.markAsPostponed(new AsynchronousOperationResult(), failedOperationResult);
            return OperationResultStatus.IN_PROGRESS;
        }
    }
}
