/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import java.util.Date;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.EventProcessingContext;
import com.evolveum.midpoint.notifications.api.events.CertCampaignEvent;
import com.evolveum.midpoint.notifications.impl.helpers.CertHelper;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimpleCampaignNotifierType;

/**
 * Various campaign-level notifications.
 */
@Component
public class SimpleCampaignNotifier extends AbstractGeneralNotifier<CertCampaignEvent, SimpleCampaignNotifierType> {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleCampaignNotifier.class);

    @Autowired
    private CertHelper certHelper;

    @Override
    public @NotNull Class<CertCampaignEvent> getEventType() {
        return CertCampaignEvent.class;
    }

    @Override
    public @NotNull Class<SimpleCampaignNotifierType> getEventHandlerConfigurationType() {
        return SimpleCampaignNotifierType.class;
    }

    @Override
    protected boolean quickCheckApplicability(
            ConfigurationItem<? extends SimpleCampaignNotifierType> configuration,
            EventProcessingContext<? extends CertCampaignEvent> ctx,
            OperationResult result) {
        // general modifications are not supported
        return ctx.event().isAdd() || ctx.event().isDelete();
    }

    @Override
    protected String getSubject(
            ConfigurationItem<? extends SimpleCampaignNotifierType> configuration,
            String transport,
            EventProcessingContext<? extends CertCampaignEvent> ctx,
            OperationResult result) {
        var event = ctx.event();
        String change;
        if (event.isAdd()) {
            change = "started";
        } else if (event.isDelete()) {
            change = "closed";
        } else {
            throw new IllegalStateException("Unexpected campaign event type: neither ADD nor DELETE");
        }
        return "Campaign " + event.getCampaignName() + " " + change;
    }

    @Override
    protected String getBody(
            ConfigurationItem<? extends SimpleCampaignNotifierType> configuration,
            String transport,
            EventProcessingContext<? extends CertCampaignEvent> ctx,
            OperationResult result) {
        var event = ctx.event();
        StringBuilder body = new StringBuilder();
        AccessCertificationCampaignType campaign = event.getCampaign();

        body.append("Campaign ");
        body.append(certHelper.getCampaignNameAndOid(event));
        body.append(" was ");
        if (event.isAdd()) {
            body.append("STARTED");
        } else if (event.isDelete()) {
            body.append("CLOSED");
        } else {
            throw new IllegalStateException("Unexpected campaign event type: neither ADD nor DELETE");
        }
        body.append(".\n\n");

        body.append("Time: ").append(new Date());     // the event is generated in the real time
        body.append("\nRequester: ").append(formatRequester(event, result));
        body.append("\nOperation status: ").append(certHelper.formatStatus(event));

        body.append("\n\nCurrent state: ").append(certHelper.formatState(event));
        body.append("\n\n");
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
