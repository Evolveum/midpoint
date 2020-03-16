/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.events;

import com.evolveum.midpoint.notifications.api.events.CertCampaignStageEvent;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventOperationType;

/**
 * Event related to certification campaign stage.
 *  ADD = stage opened (including remediation stage)
 *  MODIFY = stage deadline is approaching
 *  DELETE = stage closed
 *
 * @author mederly
 */
public class CertCampaignStageEventImpl extends AccessCertificationEventImpl implements CertCampaignStageEvent {

    public CertCampaignStageEventImpl(LightweightIdentifierGenerator lightweightIdentifierGenerator, AccessCertificationCampaignType campaign, EventOperationType opType) {
        super(lightweightIdentifierGenerator, campaign, opType);
    }

    @Override
    public boolean isCategoryType(EventCategoryType eventCategory) {
        return super.isCategoryType(eventCategory) ||
                EventCategoryType.CERT_CAMPAIGN_STAGE_EVENT.equals(eventCategory);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(this.getClass(), indent);
        debugDumpCommon(sb, indent);
        return sb.toString();
    }

}
