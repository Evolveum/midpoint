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

import com.evolveum.midpoint.model.impl.trigger.TriggerHandler;
import com.evolveum.midpoint.model.impl.trigger.TriggerHandlerRegistry;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.List;

/**
 * @author mederly
 *
 */
@Component
public class AccessCertificationCloseStageApproachingTriggerHandler implements TriggerHandler {
	
	public static final String HANDLER_URI = AccessCertificationConstants.NS_CERTIFICATION_TRIGGER_PREFIX + "/close-stage-approaching/handler-3";
	
	private static final transient Trace LOGGER = TraceManager.getTrace(AccessCertificationCloseStageApproachingTriggerHandler.class);

	@Autowired private TriggerHandlerRegistry triggerHandlerRegistry;
	@Autowired private AccCertEventHelper eventHelper;
	@Autowired private AccCertQueryHelper queryHelper;
	@Autowired private AccCertUpdateHelper updateHelper;

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
			LOGGER.info("Generating 'deadline approaching' events for {}", ObjectTypeUtil.toShortString(campaign));
			if (campaign.getState() != AccessCertificationCampaignStateType.IN_REVIEW_STAGE) {
				LOGGER.warn("Campaign is not in review stage; exiting");
				return;
			}

			eventHelper.onCampaignStageDeadlineApproaching(campaign, task, result);
			List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaign.getOid(), null, null, result);
			Collection<String> reviewers = eventHelper.getCurrentActiveReviewers(caseList);
			for (String reviewerOid : reviewers) {
				List<AccessCertificationCaseType> reviewerCaseList = queryHelper.selectOpenCasesForReviewer(caseList, reviewerOid);
				ObjectReferenceType actualReviewerRef = ObjectTypeUtil.createObjectRef(reviewerOid, ObjectTypes.USER);
				for (ObjectReferenceType reviewerOrDeputyRef : updateHelper.getReviewerAndDeputies(actualReviewerRef, task, result)) {
					eventHelper.onReviewDeadlineApproaching(reviewerOrDeputyRef, actualReviewerRef, reviewerCaseList, campaign, task, result);
				}
			}
		} catch (SchemaException|RuntimeException e) {
			LoggingUtils.logException(LOGGER, "Couldn't generate 'deadline approaching' notifications", e);
		}
	}
}
