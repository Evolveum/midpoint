/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.impl;

import com.evolveum.midpoint.model.impl.trigger.SingleTriggerHandler;
import com.evolveum.midpoint.model.api.trigger.TriggerHandlerRegistry;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.schema.util.CertCampaignTypeUtil.norm;

@Component
public class AccessCertificationCloseStageApproachingTriggerHandler implements SingleTriggerHandler {

    static final String HANDLER_URI = AccessCertificationConstants.NS_CERTIFICATION_TRIGGER_PREFIX + "/close-stage-approaching/handler-3";

    private static final Trace LOGGER = TraceManager.getTrace(AccessCertificationCloseStageApproachingTriggerHandler.class);

    @Autowired private TriggerHandlerRegistry triggerHandlerRegistry;
    @Autowired private AccCertEventHelper eventHelper;
    @Autowired private AccCertQueryHelper queryHelper;
    @Autowired private AccCertUpdateHelper updateHelper;

    @PostConstruct
    private void initialize() {
        triggerHandlerRegistry.register(HANDLER_URI, this);
    }

    @Override
    public <O extends ObjectType> void handle(@NotNull PrismObject<O> prismObject, @NotNull TriggerType trigger,
            @NotNull RunningTask task, @NotNull OperationResult result) {
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
            List<AccessCertificationCaseType> caseList = queryHelper.getAllCurrentIterationCases(
                    campaign.getOid(), norm(campaign.getIteration()), result);
            Collection<String> reviewers = CertCampaignTypeUtil.getActiveReviewers(caseList);
            for (String reviewerOid : reviewers) {
                List<AccessCertificationCaseType> reviewerCaseList = queryHelper.selectOpenCasesForReviewer(caseList, reviewerOid);
                ObjectReferenceType actualReviewerRef = ObjectTypeUtil.createObjectRef(reviewerOid, ObjectTypes.USER);
                for (ObjectReferenceType reviewerOrDeputyRef : updateHelper.getReviewerAndDeputies(actualReviewerRef, task, result)) {
                    eventHelper.onReviewDeadlineApproaching(reviewerOrDeputyRef, actualReviewerRef, reviewerCaseList, campaign, task, result);
                }
            }
        } catch (SchemaException|RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't generate 'deadline approaching' notifications", e);
            // do not retry this trigger execution
        }
    }
}
