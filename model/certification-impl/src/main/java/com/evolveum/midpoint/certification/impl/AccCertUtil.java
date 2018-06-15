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

package com.evolveum.midpoint.certification.impl;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * @author mederly
 */
@SuppressWarnings("WeakerAccess")
public class AccCertUtil {

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public static boolean isCaseRelevantForStage(AccessCertificationCaseType aCase, AccessCertificationCampaignType campaign) {
		return aCase.getStageNumber() == campaign.getStageNumber() && aCase.getIteration() == campaign.getIteration();
	}
}
