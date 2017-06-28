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

import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventOperationType;

/**
 * Event related to certification campaign.
 *  ADD = campaign started
 *  MODIFY = some state change was done (covered in detail via CertCampaignStageEvent)
 *  DELETE = campaign closed (_not_ deleted; that is not supported via this kind of events)
 *
 * @author mederly
 */
public class CertCampaignEvent extends AccessCertificationEvent {

    public CertCampaignEvent(LightweightIdentifierGenerator lightweightIdentifierGenerator, AccessCertificationCampaignType campaign, EventOperationType opType) {
        super(lightweightIdentifierGenerator, campaign, opType);
    }

    @Override
    public boolean isCategoryType(EventCategoryType eventCategoryType) {
        return super.isCategoryType(eventCategoryType) ||
                EventCategoryType.CERT_CAMPAIGN_EVENT.equals(eventCategoryType);
    }

    @Override
	public String debugDump(int indent) {
		StringBuilder sb = DebugUtil.createTitleStringBuilderLn(this.getClass(), indent);
		debugDumpCommon(sb, indent);
		return sb.toString();
	}
}
