/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.certification.impl;

import com.evolveum.midpoint.certification.api.CertificationManager;
import com.evolveum.midpoint.model.impl.trigger.SingleTriggerHandler;
import com.evolveum.midpoint.model.api.trigger.TriggerHandlerRegistry;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.IN_REVIEW_STAGE;

@Component
public class AccessCertificationCloseStageTriggerHandler implements SingleTriggerHandler {

    static final String HANDLER_URI = AccessCertificationConstants.NS_CERTIFICATION_TRIGGER_PREFIX + "/close-stage/handler-3";

    private static final Trace LOGGER = TraceManager.getTrace(AccessCertificationCloseStageTriggerHandler.class);

    @Autowired private TriggerHandlerRegistry triggerHandlerRegistry;
    @Autowired private CertificationManager certificationManager;

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
            LOGGER.info("Automatically closing current stage of {}", ObjectTypeUtil.toShortString(campaign));

            if (campaign.getState() != IN_REVIEW_STAGE) {
                LOGGER.warn("Campaign {} is not in a review stage; this 'close stage' trigger will be ignored.", ObjectTypeUtil.toShortString(campaign));
                return;
            }

            int currentStageNumber = campaign.getStageNumber();
            certificationManager.closeCurrentStage(campaign.getOid(), task, result);
            if (currentStageNumber < CertCampaignTypeUtil.getNumberOfStages(campaign)) {
                LOGGER.info("Automatically opening next stage of {}", ObjectTypeUtil.toShortString(campaign));
                certificationManager.openNextStage(campaign.getOid(), task, result);
            } else {
                LOGGER.info("Automatically starting remediation for {}", ObjectTypeUtil.toShortString(campaign));
                certificationManager.startRemediation(campaign.getOid(), task, result);
            }
        } catch (CommonException | RuntimeException e) {
            String message = "Couldn't close current campaign and possibly advance to the next one";
            LoggingUtils.logUnexpectedException(LOGGER, message, e);
            throw new SystemException(message + ": " + e.getMessage(), e);
        }
    }
}
