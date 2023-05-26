/*
 * Copyright (c) 2010-2018 Evolveum and contributors
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
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SystemException;
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

@Component
public class AccessCertificationCampaignReiterationTriggerHandler implements SingleTriggerHandler {

    static final String HANDLER_URI = AccessCertificationConstants.NS_CERTIFICATION_TRIGGER_PREFIX + "/reiterate-campaign/handler-3";

    private static final Trace LOGGER = TraceManager.getTrace(AccessCertificationCampaignReiterationTriggerHandler.class);

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
