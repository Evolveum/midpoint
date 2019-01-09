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

import com.evolveum.midpoint.certification.api.CertificationManager;
import com.evolveum.midpoint.model.impl.trigger.TriggerHandler;
import com.evolveum.midpoint.model.impl.trigger.TriggerHandlerRegistry;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_4.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_4.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_4.TriggerType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author mederly
 *
 */
@Component
public class AccessCertificationCampaignReiterationTriggerHandler implements TriggerHandler {

	static final String HANDLER_URI = AccessCertificationConstants.NS_CERTIFICATION_TRIGGER_PREFIX + "/reiterate-campaign/handler-3";

	private static final transient Trace LOGGER = TraceManager.getTrace(AccessCertificationCampaignReiterationTriggerHandler.class);

	@Autowired private TriggerHandlerRegistry triggerHandlerRegistry;
	@Autowired private CertificationManager certificationManager;

	@PostConstruct
	private void initialize() {
		triggerHandlerRegistry.register(HANDLER_URI, this);
	}

	@Override
	public <O extends ObjectType> void handle(PrismObject<O> prismObject, TriggerType trigger, Task task, OperationResult result) {
		try {
			ObjectType object = prismObject.asObjectable();
			if (!(object instanceof AccessCertificationCampaignType)) {
				LOGGER.error("Trigger of this type is supported only on {} objects, not on {}",
						AccessCertificationCampaignType.class.getSimpleName(), object.getClass().getName());
				return;
			}
			AccessCertificationCampaignType campaign = (AccessCertificationCampaignType) object;
			LOGGER.info("Automatically reiterating {}", ObjectTypeUtil.toShortString(campaign));
			if (CertCampaignTypeUtil.isReiterable(campaign)) {
				certificationManager.reiterateCampaign(campaign.getOid(), task, result);
				certificationManager.openNextStage(campaign.getOid(), task, result);
			} else {
				LOGGER.warn("Campaign {} is not reiterable, exiting.", ObjectTypeUtil.toShortString(campaign));
			}
		} catch (CommonException | RuntimeException e) {
			String message = "Couldn't reiterate the campaign and possibly advance to the next one";
			LoggingUtils.logUnexpectedException(LOGGER, message, e);
			throw new SystemException(message + ": " + e.getMessage(), e);
		}
	}
}
