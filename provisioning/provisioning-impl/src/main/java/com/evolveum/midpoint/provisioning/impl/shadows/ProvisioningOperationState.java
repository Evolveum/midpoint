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

import java.util.List;
import java.util.Objects;
import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectConverter;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.result.ResourceOperationStatus;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Represents a state of a provisioning add/modify/delete operation in the `shadows` package.
 *
 * No business aspects here. Just managing the execution state with the aim of handling pending operations.
 * Only the {@link #repoShadow} is used for other purposes.
 *
 * @author semancik
 */
public class ProvisioningOperationState implements ShortDumpable {

    /**
     * Status (success, failure, in progress), asynchronous operation reference, and the operation type (manual, asynchronous).
     *
     * It comes either from the lower layers ({@link ResourceObjectConverter}) or when the operation is postponed.
     */
    private ResourceOperationStatus resourceOperationStatus;

    /**
     * Status of the [pending] operation that is to be applied if nothing can be determined from {@link #resourceOperationStatus}.
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
    private RepoShadow repoShadow;

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
    private PendingOperation currentPendingOperation;

    /**
     * List of pending operations executed during a propagation operation.
     * These are updated in a special way.
     */
    private PendingOperations propagatedPendingOperations;

    public ProvisioningOperationState() {
    }

    public ProvisioningOperationState(RepoShadow repoShadow) {
        this.repoShadow = repoShadow;
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

    private OperationResultStatus getResultStatus() {
        return resourceOperationStatus != null ? resourceOperationStatus.getStatus() : null;
    }

    public OperationResultStatusType getResultStatusTypeOrDefault() {
        return createStatusType(
                getResultStatusOrDefault());
    }

    public PendingOperationTypeType getOperationType() {
        return resourceOperationStatus != null ? resourceOperationStatus.getOperationType() : null;
    }

    public void markAsPostponed(@NotNull OperationResultStatus currentStatus) {
        // Note about the RETRY operation type: manual nor asynchronous operations are never postponed. Hence RETRY.
        this.resourceOperationStatus =
                new ResourceOperationStatus(currentStatus, null, PendingOperationTypeType.RETRY);
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

    public RepoShadow getRepoShadow() {
        return repoShadow;
    }

    public @NotNull RepoShadow getRepoShadowRequired() {
        return Objects.requireNonNull(repoShadow, "No repo shadow");
    }

    String getRepoShadowOid() {
        return repoShadow != null ? repoShadow.getOid() : null;
    }

    /**
     * Sets the object as a reference, not as a (cloned) value.
     * All modifications on the original object will be reflected in stored one.
     */
    public void setRepoShadow(RepoShadow repoShadow) {
        this.repoShadow = repoShadow;
    }

    public PendingOperation getCurrentPendingOperation() {
        return currentPendingOperation;
    }

    public boolean hasCurrentPendingOperation() {
        return currentPendingOperation != null;
    }

    public void setCurrentPendingOperation(@NotNull PendingOperation pendingOperation) {
        stateCheck(currentPendingOperation == null,
                "Current pending operation is already set! %s in %s", currentPendingOperation, repoShadow);
        stateCheck(propagatedPendingOperations == null,
                "Propagated and 'regular' pending operations cannot be mixed: %s vs %s",
                propagatedPendingOperations, pendingOperation);
        currentPendingOperation = pendingOperation;
    }

    public PendingOperations getPropagatedPendingOperations() {
        return propagatedPendingOperations;
    }

    private void setPropagatedPendingOperations(PendingOperations propagatedPendingOperations) {
        this.propagatedPendingOperations = propagatedPendingOperations;
    }

    public Integer getAttemptNumber() {
        return attemptNumber;
    }

    private void setAttemptNumber(Integer attemptNumber) {
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
        return resourceOperationStatus != null ? resourceOperationStatus.getAsynchronousOperationReference() : null;
    }

    /** This method is called when we get the real result from the (attempted) operation execution. */
    void setResourceOperationStatus(@NotNull ResourceOperationStatus opStatus) {
        resourceOperationStatus = opStatus;
        if (resourceOperationStatus.isInProgress()) {
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
        if (resourceOperationStatus != null) {
            sb.append(", result: ");
            resourceOperationStatus.shortDump(sb);
        }
    }

    static ProvisioningOperationState fromPendingOperation(
            @NotNull RepoShadow repoShadow,
            @NotNull PendingOperation pendingOperation) {
        var newOpState = new ProvisioningOperationState(repoShadow);
        newOpState.setCurrentPendingOperation(pendingOperation);
        newOpState.setExecutionStatus(pendingOperation.getExecutionStatus());
        newOpState.setAttemptNumber(pendingOperation.getAttemptNumber());
        return newOpState;
    }

    static ProvisioningOperationState fromPropagatedPendingOperations(
            @NotNull RepoShadow repoShadow,
            @NotNull PendingOperations pendingOperations) {
        var newOpState = new ProvisioningOperationState(repoShadow);
        newOpState.setPropagatedPendingOperations(pendingOperations);
        return newOpState;
    }

    /**
     * Creates a {@link PendingOperationType} that represents the result/status of this operation. This is one of channels
     * of serializing that information.
     */
    public PendingOperationType toPendingOperation(
            ObjectDelta<ShadowType> delta, String asyncOperationReferenceOverride, XMLGregorianCalendar now)
            throws SchemaException {
        PendingOperationType pendingOperation = new PendingOperationType();
        pendingOperation.setType(getOperationType());
        pendingOperation.setDelta(
                DeltaConvertor.toObjectDeltaType(
                        convertForPendingOperationStorage(delta)));
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

    /**
     * TODO Should we store reference attributes or not?
     */
    private ObjectDelta<ShadowType> convertForPendingOperationStorage(ObjectDelta<ShadowType> delta) {
        if (ObjectDelta.isAdd(delta)) {
            var clone = delta.clone();
            var attributesContainer = ShadowUtil.getAttributesContainer(clone.getObjectToAdd());
            if (attributesContainer != null) {
                for (var refAttr : List.copyOf(attributesContainer.getReferenceAttributes())) {
                    attributesContainer.removeReference(refAttr.getElementName());
                }
            }
            return clone;
        } else if (ObjectDelta.isModify(delta)) {
            var clone = delta.clone();
            clone.getModifications().removeIf(
                    modification -> modification.getDefinition() instanceof ShadowReferenceAttributeDefinition);
            return clone;
        } else {
            return delta;
        }
    }

    public boolean hasRepoShadow() {
        return repoShadow != null;
    }
}
