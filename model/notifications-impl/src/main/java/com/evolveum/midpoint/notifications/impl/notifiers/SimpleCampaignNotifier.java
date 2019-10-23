/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import com.evolveum.midpoint.notifications.api.events.CertCampaignEvent;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.impl.helpers.CertHelper;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GeneralNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimpleCampaignNotifierType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;

/**
 * Various campaign-level notifications.
 *
 * @author mederly
 */
@Component
public class SimpleCampaignNotifier extends GeneralNotifier {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleCampaignNotifier.class);

    @Autowired
    private CertHelper certHelper;

    @PostConstruct
    public void init() {
        register(SimpleCampaignNotifierType.class);
    }

    @Override
    protected boolean quickCheckApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
        if (!(event instanceof CertCampaignEvent)) {
            LOGGER.trace("SimpleCampaignNotifier is not applicable for this kind of event, continuing in the handler chain; event class = " + event.getClass());
            return false;
        }
        // general modifications are not supported
        return event.isAdd() || event.isDelete();
    }

    @Override
    protected String getSubject(Event event, GeneralNotifierType generalNotifierType, String transport, Task task, OperationResult result) {
        CertCampaignEvent campaignEvent = (CertCampaignEvent) event;
        String change;
        if (campaignEvent.isAdd()) {
            change = "started";
        } else if (campaignEvent.isDelete()) {
            change = "closed";
        } else {
            throw new IllegalStateException("Unexpected campaign event type: neither ADD nor DELETE");
        }
        return "Campaign " + campaignEvent.getCampaignName() + " " + change;
    }

    @Override
    protected String getBody(Event event, GeneralNotifierType generalNotifierType, String transport, Task task, OperationResult result) {
        StringBuilder body = new StringBuilder();
        CertCampaignEvent campaignEvent = (CertCampaignEvent) event;
        AccessCertificationCampaignType campaign = campaignEvent.getCampaign();

        body.append("Campaign ");
        body.append(certHelper.getCampaignNameAndOid(campaignEvent));
        body.append(" was ");
        if (campaignEvent.isAdd()) {
            body.append("STARTED");
        } else if (campaignEvent.isDelete()) {
            body.append("CLOSED");
        } else {
            throw new IllegalStateException("Unexpected campaign event type: neither ADD nor DELETE");
        }
        body.append(".\n\n");

        body.append("Time: ").append(new Date());     // the event is generated in the real time
        body.append("\nRequester: ").append(formatRequester(event, result));
        body.append("\nOperation status: ").append(certHelper.formatStatus(campaignEvent));

        body.append("\n\nCurrent state: ").append(certHelper.formatState(campaignEvent));
        body.append("\n\n");
        certHelper.appendStatistics(body, campaign, task, result);

        body.append("\n\n");
        functions.addRequesterAndChannelInformation(body, event, result);

        return body.toString();
    }

    @Override
    protected Trace getLogger() {
        return LOGGER;
    }

}
