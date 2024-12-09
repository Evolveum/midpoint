/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.impl.ResourceObjectFuturizer;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.prism.xml.XmlTypeConverter.addDuration;
import static com.evolveum.midpoint.util.MiscUtil.or0;
import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType.COMPLETED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType.EXECUTING;

/**
 * Wraps a https://docs.evolveum.com/midpoint/reference/master/synchronization/consistency/[pending operation]
 * that is waiting for completion.
 *
 * This information originates in a repository shadow.
 *
 * It is used for the following main purposes:
 *
 * - executing postponed operations (retrying, grouping); see {@link ShadowRefreshOperation#retryOperation(PendingOperation, OperationResult)}
 * - keeping information about manual/async operations which are _not_ to be executed by midPoint, but a human or external system;
 * - shadow futurization; see {@link ResourceObjectFuturizer}
 *
 * == Exact content of the pending operation
 *
 * Shadow and deltas are transferred before their real execution. For example,
 *
 * - associations are translated into reference attribute values;
 * - reference attribute values are resolved from shadow OIDs to actual resource object identifiers (DNs, etc);
 * - validity and/or references simulation is carried out;
 * - READ+REPLACE and operation grouping is carried out;
 * - ...
 *
 * At exactly which level should the pending operations be recorded?
 *
 * Probably at the highest one - as seen by the client.
 *
 * See also {@link ProvisioningOperationState#toPendingOperation(ObjectDelta, String, XMLGregorianCalendar)}
 * and `PendingOperationsHelper.recordRequestedPendingOperationDelta`.
 */
public class PendingOperation implements Serializable, Cloneable, DebugDumpable {

    @NotNull private final PendingOperationType bean;

    /** Lazily evaluated */
    private ObjectDelta<ShadowType> delta;

    PendingOperation(@NotNull PendingOperationType bean) {
        this.bean = bean;
    }

    public @NotNull PendingOperationType getBean() {
        return bean;
    }

    public boolean isExecuting() {
        PendingOperationExecutionStatusType executionStatus = bean.getExecutionStatus();
        if (executionStatus == null) {
            // LEGACY: 3.7 and earlier
            return bean.getResultStatus() == OperationResultStatusType.IN_PROGRESS;
        } else {
            return executionStatus == PendingOperationExecutionStatusType.EXECUTING;
        }
    }

    public boolean isCompleted() {
        return ProvisioningUtil.isCompleted(getResultStatus());
    }

    public boolean isCompletedAndOverPeriod(XMLGregorianCalendar now, Duration period) {
        if (!isCompleted()) {
            return false;
        }
        XMLGregorianCalendar completionTimestamp = getCompletionTimestamp();
        if (completionTimestamp == null) {
            return false;
        }
        return period == null || XmlTypeConverter.isAfterInterval(completionTimestamp, period, now);
    }

    public String getAsynchronousOperationReference() {
        return bean.getAsynchronousOperationReference();
    }

    public OperationResultStatusType getResultStatus() {
        return bean.getResultStatus();
    }

    public XMLGregorianCalendar getCompletionTimestamp() {
        return bean.getCompletionTimestamp();
    }

    public @NotNull ObjectDelta<ShadowType> getDelta() throws SchemaException {
        if (delta == null) {
            delta = DeltaConvertor.createObjectDelta(
                    getDeltaBeanRequired());
        }
        return delta;
    }

    public boolean hasDelta() {
        return getDeltaBean() != null;
    }

    private @Nullable ObjectDeltaType getDeltaBean() {
        return bean.getDelta();
    }

    public @NotNull ObjectDeltaType getDeltaBeanRequired() {
        return stateNonNull(bean.getDelta(), "No delta in %s", this);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public PendingOperation clone() {
        return new PendingOperation(bean.clone());
    }

    @Override
    public String toString() {
        return "PendingOperation{" +
                "bean=" + bean +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        return bean.debugDump(indent);
    }

    public ChangeTypeType getChangeType() {
        // Actually, this should always return non-null value
        var deltaBean = bean.getDelta();
        return deltaBean != null ? deltaBean.getChangeType() : null;
    }

    public boolean isInProgress() {
        var resultStatus = getResultStatus();
        return resultStatus == null || resultStatus == OperationResultStatusType.IN_PROGRESS;
    }

    public boolean isAdd() {
        return getChangeType() == ChangeTypeType.ADD;
    }

    public boolean isModify() {
        return getChangeType() == ChangeTypeType.MODIFY;
    }

    public boolean isDelete() {
        return getChangeType() == ChangeTypeType.DELETE;
    }

    public ItemDelta<?, ?> createPropertyDelta(ItemPath itemPath, Object valueToReplace) {
        try {
            return PrismContext.get().deltaFor(ShadowType.class)
                    .item(getStandardPath(), itemPath)
                    .replace(valueToReplace)
                    .asItemDelta();
        } catch (SchemaException e) {
            throw SystemException.unexpected(e);
        }
    }

    ItemDelta<?, ?> createResultStatusDelta(OperationResultStatusType value) {
        return createPropertyDelta(PendingOperationType.F_RESULT_STATUS, value);
    }

    List<ItemDelta<?, ?>> createCompletionDeltas(XMLGregorianCalendar timestamp) {
        return List.of(
                createPropertyDelta(PendingOperationType.F_EXECUTION_STATUS, PendingOperationExecutionStatusType.COMPLETED),
                createPropertyDelta(PendingOperationType.F_COMPLETION_TIMESTAMP, timestamp));
    }

    public Collection<ItemDelta<?, ?>> createCancellationDeltas(XMLGregorianCalendar timestamp) {
        return List.of(
                createPropertyDelta(PendingOperationType.F_EXECUTION_STATUS, PendingOperationExecutionStatusType.COMPLETED),
                createPropertyDelta(PendingOperationType.F_RESULT_STATUS, OperationResultStatusType.NOT_APPLICABLE),
                createPropertyDelta(PendingOperationType.F_COMPLETION_TIMESTAMP, timestamp));
    }

    List<ItemDelta<?, ?>> createNextAttemptDeltas(int attemptNumber, XMLGregorianCalendar now) {
        return List.of(
                createPropertyDelta(PendingOperationType.F_ATTEMPT_NUMBER, attemptNumber),
                createPropertyDelta(PendingOperationType.F_LAST_ATTEMPT_TIMESTAMP, now),
                createPropertyDelta(PendingOperationType.F_RESULT_STATUS, OperationResultStatusType.IN_PROGRESS));
    }

    public ItemDelta<?, ?> createDeleteDelta() {
        try {
            return PrismContext.get().deltaFor(ShadowType.class)
                    .item(ShadowType.F_PENDING_OPERATION)
                    .delete(new PendingOperationType()
                            .id(getIdRequired()))
                    .asItemDelta();
        } catch (SchemaException e) {
            throw SystemException.unexpected(e);
        }
    }

    private long getIdRequired() {
        return stateNonNull(bean.getId(), "No ID in %s", this);
    }

    private @NotNull ItemPath getStandardPath() {
        return ShadowType.F_PENDING_OPERATION.append(getIdRequired());
    }

    /**
     * The `true` return value currently implies the operation type is {@link PendingOperationTypeType#RETRY}.
     * (Manual nor asynchronous operation have no attempt number set.)
     */
    boolean canBeRetried() {
        return bean.getType() == PendingOperationTypeType.RETRY
                && bean.getExecutionStatus() == EXECUTING
                && bean.getAttemptNumber() != null;
    }

    boolean isAfterRetryPeriod(Duration retryPeriod, XMLGregorianCalendar now) {
        XMLGregorianCalendar scheduledRetryTime = addDuration(bean.getLastAttemptTimestamp(), retryPeriod);
        return XmlTypeConverter.compare(now, scheduledRetryTime) == DatatypeConstants.GREATER;
    }

    public int getAttemptNumber() {
        return or0(bean.getAttemptNumber());
    }

    public PendingOperationExecutionStatusType getExecutionStatus() {
        return bean.getExecutionStatus();
    }

    public XMLGregorianCalendar getOperationStartTimestamp() {
        return bean.getOperationStartTimestamp();
    }

    public XMLGregorianCalendar getRequestTimestamp() {
        return bean.getRequestTimestamp();
    }

    public PendingOperationTypeType getType() {
        return bean.getType();
    }

    public XMLGregorianCalendar getLastAttemptTimestamp() {
        return bean.getLastAttemptTimestamp();
    }

    public boolean isPendingAddOrDelete() {
        return getExecutionStatus() != COMPLETED
                && (isAdd() || isDelete());
    }
}
