/*
 * Copyright (C) 2020-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl.events;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.notifications.api.OperationStatus;
import com.evolveum.midpoint.notifications.api.events.ResourceObjectEvent;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class ResourceObjectEventImpl extends BaseEventImpl implements ResourceObjectEvent {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectEventImpl.class);

    @NotNull private final ResourceOperationDescription operationDescription;
    @NotNull private final OperationStatus operationStatus;
    @NotNull private final ChangeType changeType;

    public ResourceObjectEventImpl(LightweightIdentifierGenerator lightweightIdentifierGenerator,
            @NotNull ResourceOperationDescription operationDescription, @NotNull OperationStatus status) {
        super(lightweightIdentifierGenerator);
        this.operationDescription = operationDescription;
        this.operationStatus = status;
        this.changeType = operationDescription.getObjectDelta().getChangeType();
    }

    @NotNull
    @Override
    public ResourceOperationDescription getOperationDescription() {
        return operationDescription;
    }

    @NotNull
    @Override
    public OperationStatus getOperationStatus() {
        return operationStatus;
    }

    @Override
    public boolean isStatusType(EventStatusType eventStatus) {
        return operationStatus.matchesEventStatusType(eventStatus);
    }

    @NotNull
    @Override
    public ChangeType getChangeType() {
        return changeType;
    }

    @Override
    public boolean isOperationType(EventOperationType eventOperation) {
        return changeTypeMatchesOperationType(changeType, eventOperation);
    }

    @Override
    public boolean isCategoryType(EventCategoryType eventCategory) {
        return eventCategory == EventCategoryType.RESOURCE_OBJECT_EVENT;
    }

    @Override
    public boolean isShadowKind(ShadowKindType shadowKindType) {
        ShadowKindType actualKind = operationDescription.getCurrentShadow().asObjectable().getKind();
        if (actualKind != null) {
            return actualKind.equals(shadowKindType);
        } else {
            return ShadowKindType.ACCOUNT.equals(shadowKindType);
        }
    }

    @Override
    public ShadowType getShadow() {
        PrismObject<? extends ShadowType> shadow = operationDescription.getCurrentShadow();
        return shadow != null ? shadow.asObjectable() : null;
    }

    @Override
    public boolean isShadowIntent(String intent) {
        if (StringUtils.isNotEmpty(intent)) {
            return intent.equals(operationDescription.getCurrentShadow().asObjectable().getIntent());
        } else {
            return StringUtils.isEmpty(operationDescription.getCurrentShadow().asObjectable().getIntent());
        }
    }

    @Override
    public ObjectDelta<ShadowType> getShadowDelta() {
        //noinspection unchecked
        return (ObjectDelta<ShadowType>) operationDescription.getObjectDelta();
    }

    @Override
    public boolean isRelatedToItem(ItemPath itemPath) {
        return containsItem(getShadowDelta(), itemPath);
    }

    @Override
    public String getShadowName() {
        PrismObject<? extends ShadowType> shadow = operationDescription.getCurrentShadow();
        if (shadow == null) {
            return null;
        } else if (shadow.asObjectable().getName() != null) {
            return shadow.asObjectable().getName().getOrig();
        } else {
            Collection<ShadowSimpleAttribute<?>> secondaryIdentifiers = ShadowUtil.getSecondaryIdentifiers(shadow);
            LOGGER.trace("secondary identifiers: {}", secondaryIdentifiers);
            if (secondaryIdentifiers != null) {
                // first phase = looking for "name" identifier
                for (ShadowSimpleAttribute<?> ra : secondaryIdentifiers) {
                    if (ra.getElementName() != null && ra.getElementName().getLocalPart().contains("name")) {
                        LOGGER.trace("Considering {} as a name", ra);
                        return String.valueOf(ra.getAnyRealValue());
                    }
                }
                // second phase = returning any value
                if (!secondaryIdentifiers.isEmpty()) {
                    return String.valueOf(secondaryIdentifiers.iterator().next().getAnyRealValue());
                }
            }
            return null;
        }
    }

    @Override
    public PolyStringType getResourceName() {
        return operationDescription.getResource().asObjectable().getName();
    }

    @Override
    public String getResourceOid() {
        return operationDescription.getResource().getOid();
    }

    @Override
    public String getPlaintextPassword() {
        ObjectDelta<? extends ShadowType> delta = operationDescription.getObjectDelta();
        if (delta != null) {
            try {
                return getMidpointFunctions().getPlaintextAccountPasswordFromDelta(delta);
            } catch (EncryptionException e) {
                LoggingUtils.logException(LOGGER, "Couldn't decrypt password from shadow delta: {}", e, delta.debugDump());
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public boolean hasContentToShow() {
        return hasContentToShow(false, false);
    }

    @Override
    public boolean hasContentToShow(boolean watchSynchronizationAttributes, boolean watchAuxiliaryAttributes) {
        ObjectDelta<ShadowType> delta = getShadowDelta();
        if (!delta.isModify()) {
            return true;
        } else if (!getTextFormatter().containsVisibleModifiedItems(delta.getModifications(),
                watchSynchronizationAttributes, watchAuxiliaryAttributes)) {
            LOGGER.trace("No relevant attributes in modify delta (watchSync={}, watchAux={})",
                    watchSynchronizationAttributes, watchAuxiliaryAttributes);
            return false;
        } else {
            return true;
        }
    }

    public String getContentAsFormattedList() {
        return getContentAsFormattedList(false, false, null, null);
    }

    @Override
    public String getContentAsFormattedList(Task task, OperationResult result) {
        return getContentAsFormattedList(false, false, task, result);
    }

    @Override
    public String getContentAsFormattedList(boolean showSynchronizationItems, boolean showAuxiliaryAttributes,
            Task task, OperationResult result) {
        final ObjectDelta<ShadowType> shadowDelta = getShadowDelta();
        if (shadowDelta == null) {
            return "";
        }
        if (shadowDelta.isAdd()) {
            return getTextFormatter()
                    .formatShadowAttributes(shadowDelta.getObjectToAdd().asObjectable(), showSynchronizationItems, false);
        } else if (shadowDelta.isModify()) {
            if (task == null) {
                return getTextFormatter().formatObjectModificationDelta(shadowDelta, showSynchronizationItems,
                        showAuxiliaryAttributes);
            }
            return getTextFormatter().formatObjectModificationDelta(shadowDelta, showSynchronizationItems,
                    showAuxiliaryAttributes, task, result);
        } else {
            return "";
        }
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(this.getClass(), indent);
        debugDumpCommon(sb, indent);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "operationStatus", operationStatus, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "accountOperationDescription", operationDescription, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "changeType", changeType, indent + 1);
        return sb.toString();
    }

    @Override
    public String toString() {
        return toStringPrefix() +
                ", changeType=" + changeType +
                ", operationStatus=" + operationStatus +
                '}';
    }
}
