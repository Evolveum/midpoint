/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import com.evolveum.midpoint.notifications.api.EventProcessingContext;
import com.evolveum.midpoint.notifications.api.events.CertCampaignStageEvent;
import com.evolveum.midpoint.notifications.impl.helpers.CertHelper;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimpleCampaignStageNotifierType;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Various stage-level notifications.
 */
@Component
public class SimpleCampaignStageNotifier extends AbstractGeneralNotifier<CertCampaignStageEvent, SimpleCampaignStageNotifierType> {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleCampaignStageNotifier.class);

    @Autowired private CertHelper certHelper;

    @Override
    public @NotNull Class<CertCampaignStageEvent> getEventType() {
        return CertCampaignStageEvent.class;
    }

    @Override
    public @NotNull Class<SimpleCampaignStageNotifierType> getEventHandlerConfigurationType() {
        return SimpleCampaignStageNotifierType.class;
    }

    @Override
    protected String getSubject(
            ConfigurationItem<? extends SimpleCampaignStageNotifierType> configuration,
            String transport,
            EventProcessingContext<? extends CertCampaignStageEvent> ctx,
            OperationResult result) {
        var event = ctx.event();
        String change;
        if (event.isAdd()) {
            change = "started";
        } else if (event.isDelete()) {
            change = "closed";
        } else if (event.isModify()) {
            change = "about to be closed";
        } else {
            throw new IllegalStateException("Unexpected campaign event type: neither ADD nor MODIFY nor DELETE");
        }
        return "Campaign " + event.getCampaignName()
                + " " + certHelper.getStageShortName(event.getCampaign())
                + " " + change;
    }

    @Override
    protected String getBody(
            ConfigurationItem<? extends SimpleCampaignStageNotifierType> configuration,
            String transport,
            EventProcessingContext<? extends CertCampaignStageEvent> ctx,
            OperationResult result) {
        StringBuilder body = new StringBuilder();
        var event = ctx.event();
        AccessCertificationCampaignType campaign = event.getCampaign();

        body.append("A certification campaign stage ");
        if (event.isAdd()) {
            body.append("has been started");
        } else if (event.isDelete()) {
            body.append("has been closed");
        } else if (event.isModify()) {
            body.append("is about to be closed");
        }
        body.append(".");
        body.append("\n\nCampaign: ");
        body.append(certHelper.getCampaignNameAndOid(event));
        body.append("\nState: ");
        body.append(certHelper.formatState(event));

        body.append("\n\nTime: ").append(new Date());     // the event is generated in the real time
        AccessCertificationStageType stage = CertCampaignTypeUtil.getCurrentStage(campaign);
        if (stage != null) {
            body.append("\n\nStage start time: ").append(XmlTypeConverter.toDate(stage.getStartTimestamp()));
            body.append("\nStage deadline time: ").append(XmlTypeConverter.toDate(stage.getDeadline()));
            if (event.isModify() && stage.getDeadline() != null) {
                long delta = XmlTypeConverter.toMillis(stage.getDeadline()) - System.currentTimeMillis();
                if (delta > 0) {
                    body.append("\n\nStage ends in ");
                    body.append(DurationFormatUtils.formatDurationWords(delta, true, true));
                } else if (delta < 0) {
                    body.append("\n\nStage should have ended ");
                    body.append(DurationFormatUtils.formatDurationWords(-delta, true, true));
                    body.append(" ago");
                }
            }
        }
        body.append("\n");
        if (event.isAdd() || event.isDelete()) {
            body.append("\nRequester: ").append(formatRequester(event, result));
            body.append("\nOperation status: ").append(certHelper.formatStatus(event));
            body.append("\n");
        }

        body.append("\n");
        certHelper.appendStatistics(body, campaign, ctx.task(), result);

        body.append("\n\n");
        addRequesterAndChannelInformation(body, event, result);

        return body.toString();
    }

    @Override
    protected Trace getLogger() {
        return LOGGER;
    }
}
