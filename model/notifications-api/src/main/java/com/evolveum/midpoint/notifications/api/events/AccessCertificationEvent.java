/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.notifications.api.events;

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
import org.apache.commons.lang.Validate;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.*;

/**
 * Any event that is related to access certification.
 *
 * @author mederly
 */
public abstract class AccessCertificationEvent extends BaseEvent {

    // all these must not be null
    protected AccessCertificationCampaignType campaign;
    protected OperationResultStatus status;
    protected EventOperationType operationType;


    public AccessCertificationEvent(LightweightIdentifierGenerator lightweightIdentifierGenerator, AccessCertificationCampaignType campaign, EventOperationType opType) {
        super(lightweightIdentifierGenerator);
        Validate.notNull(campaign, "campaign");
        Validate.notNull(opType, "opType");
        this.campaign = campaign;
        this.operationType = opType;
        this.status = OperationResultStatus.SUCCESS;            // TODO fix this temporary implementation
    }

    public AccessCertificationCampaignType getCampaign() {
        return campaign;
    }

    @Override
    public boolean isRelatedToItem(ItemPath itemPath) {
        return false;           // not supported for this kind of events
    }

    @Override
    public boolean isStatusType(EventStatusType eventStatusType) {
        return false;
    }

    @Override
    public boolean isOperationType(EventOperationType eventOperationType) {
        return this.operationType.equals(eventOperationType);
    }

    @Override
    public boolean isCategoryType(EventCategoryType eventCategoryType) {
        return EventCategoryType.ACCESS_CERTIFICATION_EVENT.equals(eventCategoryType);
    }

    public String getCampaignName() {
        return campaign.getName().getOrig();
    }

    public OperationResultStatus getStatus() {
        return status;
    }

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
