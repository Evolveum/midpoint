/*
 * Copyright (c) 2010-2015 Evolveum
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
package com.evolveum.midpoint.certification.impl;

import com.evolveum.midpoint.certification.api.CertificationManager;
import com.evolveum.midpoint.model.impl.trigger.TriggerHandler;
import com.evolveum.midpoint.model.impl.trigger.TriggerHandlerRegistry;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author mederly
 *
 */
@Component
public class AccessCertificationCloseStageTriggerHandler implements TriggerHandler {
	
	public static final String HANDLER_URI = AccessCertificationConstants.NS_CERTIFICATION_TRIGGER_PREFIX + "/close-stage/handler-3";
	
	private static final transient Trace LOGGER = TraceManager.getTrace(AccessCertificationCloseStageTriggerHandler.class);

	@Autowired
	private TriggerHandlerRegistry triggerHandlerRegistry;
	
	@Autowired
	private CertificationManager certificationManager;
	
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
			LOGGER.info("Automatically closing current stage of {}", ObjectTypeUtil.toShortString(campaign));

			int currentStageNumber = campaign.getStageNumber();
			certificationManager.closeCurrentStage(campaign.getOid(), currentStageNumber, task, result);
			if (currentStageNumber < CertCampaignTypeUtil.getNumberOfStages(campaign)) {
				LOGGER.info("Automatically opening next stage of {}", ObjectTypeUtil.toShortString(campaign));
				certificationManager.openNextStage(campaign.getOid(), currentStageNumber + 1, task, result);
			}
		} catch (SchemaException|ObjectNotFoundException|ObjectAlreadyExistsException|SecurityViolationException|RuntimeException e) {
			LoggingUtils.logException(LOGGER, "Couldn't close current campaign and possibly advance to the next one", e);
		}
	}
}
