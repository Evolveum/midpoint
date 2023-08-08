/*
 * Copyright (C) 2020-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl.events;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.notifications.api.OperationStatus;
import com.evolveum.midpoint.notifications.api.events.CaseManagementEvent;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.cases.ApprovalUtils;
import com.evolveum.midpoint.schema.util.cases.ManualCaseUtils;
import com.evolveum.midpoint.schema.util.cases.OwnerOptionIdentifier;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

abstract public class CaseManagementEventImpl extends BaseEventImpl implements CaseManagementEvent {

    private static final Trace LOGGER = TraceManager.getTrace(CaseManagementEventImpl.class);

    @Nullable protected final ApprovalContextType approvalContext;
    @NotNull private final ChangeType changeType;
    @NotNull protected final CaseType aCase;

    CaseManagementEventImpl(
            @NotNull LightweightIdentifierGenerator lightweightIdentifierGenerator, @NotNull ChangeType changeType,
            @Nullable ApprovalContextType approvalContext, @NotNull CaseType aCase) {
        super(lightweightIdentifierGenerator);
        this.changeType = changeType;
        this.approvalContext = approvalContext;
        this.aCase = aCase;
    }

    @Override
    @NotNull
    public CaseType getCase() {
        return aCase;
    }

    @Override
    public OperationStatus getOperationStatus() {
        return outcomeToStatus(changeType, getCaseOrItemOutcome());
    }

    protected abstract String getCaseOrItemOutcome();

    @Override
    public String getStatusAsText() {
        if (isApproval()) {
            if (isResultKnown()) {
                return isApproved() ? "Approved" : "Rejected";
            } else {
                return "";
            }
        } else if (isCorrelation()) {
            String outcome = getCaseOrItemOutcome();
            if (outcome == null) {
                return "";
            } else {
                OwnerOptionIdentifier identifier = OwnerOptionIdentifier.fromStringValueForgiving(outcome);
                if (identifier.isNewOwner()) {
                    return "No existing owner";
                } else {
                    return identifier.getExistingOwnerId();
                }
            }
        } else if (isManualProvisioning()) {
            return String.valueOf(
                    ManualCaseUtils.translateOutcomeToStatus(
                            getCaseOrItemOutcome()));
        } else {
            return "";
        }
    }

    @Override
    public boolean isStatusType(EventStatusType eventStatus) {
        return getOperationStatus().matchesEventStatusType(eventStatus);
    }

    @Override
    public @NotNull ChangeType getChangeType() {
        return changeType;
    }

    @Override
    public boolean isOperationType(EventOperationType eventOperation) {
        return changeTypeMatchesOperationType(changeType, eventOperation);
    }

    @Override
    public boolean isResultKnown() {
        return !isInProgress(); // for now
    }

    @Override
    public boolean isApproved() {
        return isSuccess(); // for now
    }

    @Override
    public boolean isRejected() {
        return isFailure(); // for now
    }

    private OperationStatus outcomeToStatus(ChangeType changeType, String outcome) {
        if (changeType != ChangeType.DELETE) {
            return OperationStatus.SUCCESS; // TODO really?
        } else {
            if (outcome == null) {
                return OperationStatus.IN_PROGRESS;
            }

            if (isApproval()) {
                try {
                    Boolean approved = ApprovalUtils.approvalBooleanValueFromUri(outcome);
                    if (approved == null) {
                        return OperationStatus.OTHER;
                    } else {
                        return approved ? OperationStatus.SUCCESS : OperationStatus.FAILURE;
                    }
                } catch (Exception e) {
                    // TODO Should we really catch and process this exception?
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't parse approval URI {}", e, outcome);
                    return OperationStatus.OTHER;
                }
            } else if (isManualProvisioning()) {
                return OperationStatus.fromOperationResultStatus(
                        ManualCaseUtils.translateOutcomeToStatus(outcome));
            } else {
                // Correlation & others
                return OperationStatus.SUCCESS;
            }
        }
    }

    @Override
    @Nullable
    public ApprovalContextType getApprovalContext() {
        return approvalContext;
    }

    @Override
    public @Nullable ObjectType getObject() {
        return resolveReferenceIfExists(aCase.getObjectRef());
    }

    @Override
    public @Nullable PolyStringType getObjectName() {
        return getNameFromReference(aCase.getObjectRef());
    }

    @Override
    public @Nullable ObjectType getTarget() {
        return resolveReferenceIfExists(aCase.getTargetRef());
    }

    @Override
    public @Nullable PolyStringType getTargetName() {
        return getNameFromReference(aCase.getTargetRef());
    }

    @Override
    public @Nullable Object getFocusValue(@NotNull String pathString) {
        Set<?> realValues = getFocusValues(pathString);
        if (realValues.isEmpty()) {
            return null;
        } else if (realValues.size() == 1) {
            return realValues.iterator().next();
        } else {
            return realValues;
        }
    }

    @Override
    public @NotNull Set<?> getFocusValues(@NotNull String pathString) {
        CaseCorrelationContextType correlationContext = getCorrelationContext();
        if (correlationContext == null) {
            return Set.of();
        }
        ObjectReferenceType preFocusRef = correlationContext.getPreFocusRef();
        if (preFocusRef == null) {
            return Set.of();
        }
        PrismObject<?> preFocus = preFocusRef.getObject();
        if (preFocus == null) {
            return Set.of();
        }
        ItemPath path = PrismContext.get().itemPathParser().asItemPath(pathString);
        Collection<PrismValue> allValues = preFocus.getValue().getAllValues(path);
        return allValues.stream()
                .filter(Objects::nonNull)
                .map(PrismValue::getRealValue)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        return toStringPrefix() +
                ", processInstanceName='" + getCaseName() + '\'' +
                ", changeType=" + changeType +
                ", outcome=" + getCaseOrItemOutcome() +
                '}';
    }

    // This method is not used. It is here just for maven dependency plugin to detect the
    // dependency on workflow-api
    @SuppressWarnings("unused")
    private void notUsed() {
        ApprovalUtils.approvalBooleanValueFromUri("");
    }

    @Override
    protected void debugDumpCommon(StringBuilder sb, int indent) {
        super.debugDumpCommon(sb, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "processInstanceName", getCaseName(), indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "changeType", changeType, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "outcome", getCaseOrItemOutcome(), indent + 1);
    }
}
