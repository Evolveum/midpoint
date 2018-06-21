/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.certification.dto;

import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterEntry;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;

/**
 * @author mederly
 */
public enum CertCampaignStateFilter {
	ALL,
	NOT_CLOSED,
	CREATED,
	IN_REVIEW_STAGE,
	REVIEW_STAGE_DONE,
	IN_REMEDIATION,
	CLOSED;

	public S_AtomicFilterEntry appendFilter(S_AtomicFilterEntry q) {
		switch (this) {
			case ALL:
				return q;
			case NOT_CLOSED:
				return q.block().not().item(AccessCertificationCampaignType.F_STATE)
						.eq(AccessCertificationCampaignStateType.CLOSED).endBlock().and();
			case CREATED:
				return q.item(AccessCertificationCampaignType.F_STATE).eq(AccessCertificationCampaignStateType.CREATED).and();
			case IN_REVIEW_STAGE:
				return q.item(AccessCertificationCampaignType.F_STATE)
						.eq(AccessCertificationCampaignStateType.IN_REVIEW_STAGE).and();
			case REVIEW_STAGE_DONE:
				return q.item(AccessCertificationCampaignType.F_STATE)
						.eq(AccessCertificationCampaignStateType.REVIEW_STAGE_DONE).and();
			case IN_REMEDIATION:
				return q.item(AccessCertificationCampaignType.F_STATE).eq(AccessCertificationCampaignStateType.IN_REMEDIATION)
						.and();
			case CLOSED:
				return q.item(AccessCertificationCampaignType.F_STATE).eq(AccessCertificationCampaignStateType.CLOSED).and();
			default:
				throw new SystemException("Unknown value for StatusFilter: " + this);
		}
	}
}
