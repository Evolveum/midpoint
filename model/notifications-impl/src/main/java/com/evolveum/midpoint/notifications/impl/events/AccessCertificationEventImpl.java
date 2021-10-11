/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.events;

import com.evolveum.midpoint.notifications.api.events.AccessCertificationEvent;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventStatusType;
import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.*;

public abstract class AccessCertificationEventImpl extends BaseEventImpl implements AccessCertificationEvent {

    @NotNull final protected AccessCertificationCampaignType campaign;
    @NotNull final protected OperationResultStatus status;
    @NotNull final private EventOperationType operationType;

    AccessCertificationEventImpl(LightweightIdentifierGenerator lightweightIdentifierGenerator,
            @NotNull AccessCertificationCampaignType campaign, @NotNull EventOperationType opType) {
        super(lightweightIdentifierGenerator);
        this.campaign = campaign;
        this.operationType = opType;
        this.status = OperationResultStatus.SUCCESS;            // TODO fix this temporary implementation
    }

    @Override
    @NotNull
    public AccessCertificationCampaignType getCampaign() {
        return campaign;
    }

    @Override
    public boolean isRelatedToItem(ItemPath itemPath) {
        return false;           // not supported for this kind of events
    }

    @Override
    public boolean isStatusType(EventStatusType eventStatus) {
        return false;
    }

    @Override
    public boolean isOperationType(EventOperationType eventOperation) {
        return this.operationType.equals(eventOperation);
    }

    @Override
    public boolean isCategoryType(EventCategoryType eventCategory) {
        return EventCategoryType.ACCESS_CERTIFICATION_EVENT.equals(eventCategory);
    }

    @NotNull
    public OperationResultStatus getStatus() {
        return status;
    }

    @NotNull
    public EventOperationType getOperationType() {
        return operationType;
    }

    public AccessCertificationStageDefinitionType getCurrentStageDefinition() {
        if (campaign.getState() != IN_REVIEW_STAGE && campaign.getState() != REVIEW_STAGE_DONE) {
            return null;
        }
        return CertCampaignTypeUtil.findStageDefinition(campaign, campaign.getStageNumber());
    }

    @Override
    protected void debugDumpCommon(StringBuilder sb, int indent) {
        super.debugDumpCommon(sb, indent);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "campaign", campaign, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "status", status, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "operationType", operationType, indent + 1);
    }
}
