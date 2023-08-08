/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.EventProcessingContext;
import com.evolveum.midpoint.notifications.api.events.CertReviewEvent;
import com.evolveum.midpoint.notifications.impl.helpers.CertHelper;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.cases.ApprovalContextUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimpleReviewerNotifierType;

/**
 * Various reviewer-level notifications.
 */
@Component
public class SimpleReviewerNotifier extends AbstractGeneralNotifier<CertReviewEvent, SimpleReviewerNotifierType> {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleReviewerNotifier.class);

    @Autowired private CertHelper certHelper;

    @Override
    public @NotNull Class<CertReviewEvent> getEventType() {
        return CertReviewEvent.class;
    }

    @Override
    public @NotNull Class<SimpleReviewerNotifierType> getEventHandlerConfigurationType() {
        return SimpleReviewerNotifierType.class;
    }

    @Override
    protected boolean quickCheckApplicability(
            ConfigurationItem<? extends SimpleReviewerNotifierType> configuration,
            EventProcessingContext<? extends CertReviewEvent> ctx,
            OperationResult result) {
        var event = ctx.event();
        if (event.isAdd()) {
            return true;
        }
        if (event.isDelete()) {
            return false; // such events are not even created
        }
        AccessCertificationStageDefinitionType stageDef = event.getCurrentStageDefinition();
        if (stageDef == null) {
            return false; // should not occur
        }
        if (Boolean.FALSE.equals(stageDef.isNotifyOnlyWhenNoDecision())) {
            return true;
        }
        return !event.getCasesAwaitingResponseFromActualReviewer().isEmpty();
    }

    @Override
    protected String getSubject(
            ConfigurationItem<? extends SimpleReviewerNotifierType> configuration,
            String transport,
            EventProcessingContext<? extends CertReviewEvent> ctx,
            OperationResult result) {
        var event = ctx.event();
        String campaignName = event.getCampaignName();
        if (event.isAdd()) {
            return "Your review is requested in campaign " + campaignName;
        } else if (event.isModify()) {
            return "Deadline for your review in campaign " + campaignName + " is approaching";
        } else {
            throw new IllegalStateException("Unexpected review event type: neither ADD nor MODIFY");
        }
    }

    @Override
    protected String getBody(
            ConfigurationItem<? extends SimpleReviewerNotifierType> configuration,
            String transport,
            EventProcessingContext<? extends CertReviewEvent> ctx,
            OperationResult result) {
        StringBuilder body = new StringBuilder();
        var event = ctx.event();
        AccessCertificationCampaignType campaign = event.getCampaign();

        body.append("You have been requested to provide a review in a certification campaign.");
        body.append("\n");
        body.append("\nReviewer: ").append(valueFormatter.formatUserName(event.getActualReviewer(), result));
        if (!event.getActualReviewer().getOid().equals(event.getRequesteeOid())) {
            body.append("\nDeputy: ").append(valueFormatter.formatUserName(event.getRequestee(), result));
        }
        body.append("\n");
        body.append("\nCampaign: ").append(certHelper.getCampaignNameAndOid(event));
        body.append("\nState: ").append(certHelper.formatState(event));
        body.append("\n\n");
        AccessCertificationStageType stage = CertCampaignTypeUtil.getCurrentStage(campaign);
        if (stage != null) {
            body.append("Stage start time: ").append(XmlTypeConverter.toDate(stage.getStartTimestamp()));
            body.append("\nStage deadline: ").append(XmlTypeConverter.toDate(stage.getDeadline()));
            if (stage.getEscalationLevel() != null) {
                body.append("\nEscalation level: ").append(ApprovalContextUtil.getEscalationLevelInfo(stage.getEscalationLevel()));
            }
            if (stage.getDeadline() != null) {
                long delta = XmlTypeConverter.toMillis(stage.getDeadline()) - System.currentTimeMillis();
                if (delta > 0) {
                    if (event.isModify()) {
                        body.append("\n\nThis is to notify you that the stage ends in ");
                    } else {
                        body.append("\n\nThe stage ends in ");
                    }
                    body.append(DurationFormatUtils.formatDurationWords(delta, true, true));
                } else if (delta < 0) {
                    body.append("\n\nThe stage should have ended ");
                    body.append(DurationFormatUtils.formatDurationWords(-delta, true, true));
                    body.append(" ago");
                }
            }
            body.append("\n\n");
            body.append("There are ").append(event.getCases().size()).append(" cases to be reviewed by you. ");
            body.append("Out of them, ").append(event.getCasesAwaitingResponseFromActualReviewer().size()).append(" are still waiting for your response.");
        }

        return body.toString();
    }

    @Override
    protected Trace getLogger() {
        return LOGGER;
    }
}
