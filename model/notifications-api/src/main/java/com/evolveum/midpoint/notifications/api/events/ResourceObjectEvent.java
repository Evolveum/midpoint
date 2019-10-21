/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.api.events;

import com.evolveum.midpoint.notifications.api.OperationStatus;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang.StringUtils;

/**
 * @author mederly
 */
public class ResourceObjectEvent extends BaseEvent {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectEvent.class);

    private OperationStatus operationStatus;        // status of the operation

    private ResourceOperationDescription accountOperationDescription;

    private ChangeType changeType;

    // the following two are currently unused
    private boolean activationRequested;
    private boolean deactivationRequested;

    public ResourceObjectEvent(LightweightIdentifierGenerator lightweightIdentifierGenerator) {
        super(lightweightIdentifierGenerator);
    }

    public ResourceOperationDescription getAccountOperationDescription() {
        return accountOperationDescription;
    }

    public void setAccountOperationDescription(ResourceOperationDescription accountOperationDescription) {
        this.accountOperationDescription = accountOperationDescription;
    }

    public boolean isActivationRequested() {
        return activationRequested;
    }

    public void setActivationRequested(boolean activationRequested) {
        this.activationRequested = activationRequested;
    }

    public boolean isDeactivationRequested() {
        return deactivationRequested;
    }

    public void setDeactivationRequested(boolean deactivationRequested) {
        this.deactivationRequested = deactivationRequested;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public void setChangeType(ChangeType changeType) {
        this.changeType = changeType;
    }

    @Override
    public boolean isOperationType(EventOperationType eventOperationType) {
        return changeTypeMatchesOperationType(changeType, eventOperationType);
    }

    @Override
    public boolean isCategoryType(EventCategoryType eventCategoryType) {
        return eventCategoryType == EventCategoryType.RESOURCE_OBJECT_EVENT;
    }

    public boolean isShadowKind(ShadowKindType shadowKindType) {
        ShadowKindType actualKind = accountOperationDescription.getCurrentShadow().asObjectable().getKind();
        if (actualKind != null) {
            return actualKind.equals(shadowKindType);
        } else {
            return ShadowKindType.ACCOUNT.equals(shadowKindType);
        }
    }

    public ShadowType getShadow() {
        PrismObject<? extends ShadowType> shadow = accountOperationDescription.getCurrentShadow();
        return shadow != null ? shadow.asObjectable() : null;
    }

    public boolean isShadowIntent(String intent) {
        if (StringUtils.isNotEmpty(intent)) {
            return intent.equals(accountOperationDescription.getCurrentShadow().asObjectable().getIntent());
        } else {
            return StringUtils.isEmpty(accountOperationDescription.getCurrentShadow().asObjectable().getIntent());
        }
    }

    public ObjectDelta<ShadowType> getShadowDelta() {
        return (ObjectDelta<ShadowType>) accountOperationDescription.getObjectDelta();
    }

    public OperationStatus getOperationStatus() {
        return operationStatus;
    }

    public void setOperationStatus(OperationStatus operationStatus) {
        this.operationStatus = operationStatus;
    }

    @Override
    public boolean isStatusType(EventStatusType eventStatusType) {
        return operationStatus.matchesEventStatusType(eventStatusType);
    }

    @Override
    public boolean isRelatedToItem(ItemPath itemPath) {
        return containsItem(getShadowDelta(), itemPath);
    }

    @Override
    public String toString() {
        return "ResourceObjectEvent{" +
                "base=" + super.toString() +
                ", changeType=" + changeType +
                ", operationStatus=" + operationStatus +
                '}';
    }

    public String getShadowName() {
        return getNotificationFunctions().getShadowName(getAccountOperationDescription().getCurrentShadow());
    }

    public PolyStringType getResourceName() {
        return getAccountOperationDescription().getResource().asObjectable().getName();
    }

    public String getResourceOid() {
        return getAccountOperationDescription().getResource().getOid();
    }

    public String getPlaintextPassword() {
        ObjectDelta delta = getAccountOperationDescription().getObjectDelta();
        return delta != null ? getNotificationFunctions().getPlaintextPasswordFromDelta(delta) : null;
    }

    public String getContentAsFormattedList() {
        return getContentAsFormattedList(false, false);
    }

    public String getContentAsFormattedList(boolean showSynchronizationItems, boolean showAuxiliaryAttributes) {
        return getNotificationFunctions().getContentAsFormattedList(this, showSynchronizationItems, showAuxiliaryAttributes);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(this.getClass(), indent);
        debugDumpCommon(sb, indent);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "operationStatus", operationStatus, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "accountOperationDescription", accountOperationDescription, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "changeType", changeType, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "activationRequested", activationRequested, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "deactivationRequested", deactivationRequested, indent + 1);
        return sb.toString();
    }
}
