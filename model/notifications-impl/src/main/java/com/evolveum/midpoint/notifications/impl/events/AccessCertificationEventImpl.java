/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.notifications.impl.events;

import static com.evolveum.midpoint.util.MiscUtil.or0;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.IN_REVIEW_STAGE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.REVIEW_STAGE_DONE;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.notifications.api.events.AccessCertificationEvent;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public abstract class AccessCertificationEventImpl extends BaseEventImpl implements AccessCertificationEvent {

    @NotNull protected final AccessCertificationCampaignType campaign;
    @NotNull protected final OperationResultStatus status;
    @NotNull private final EventOperationType operationType;

    AccessCertificationEventImpl(LightweightIdentifierGenerator lightweightIdentifierGenerator,
            @NotNull AccessCertificationCampaignType campaign, @NotNull EventOperationType opType) {
        super(lightweightIdentifierGenerator);
        this.campaign = campaign;
        this.operationType = opType;
        this.status = OperationResultStatus.SUCCESS; // TODO fix this temporary implementation
    }

    @Override
    @NotNull
    public AccessCertificationCampaignType getCampaign() {
        return campaign;
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
        return CertCampaignTypeUtil.findStageDefinition(campaign, or0(campaign.getStageNumber()));
    }

    @Override
    protected void debugDumpCommon(StringBuilder sb, int indent) {
        super.debugDumpCommon(sb, indent);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "campaign", campaign, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "status", status, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "operationType", operationType, indent + 1);
    }
}
