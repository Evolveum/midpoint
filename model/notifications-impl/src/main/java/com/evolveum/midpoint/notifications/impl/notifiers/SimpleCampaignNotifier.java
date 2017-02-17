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

package com.evolveum.midpoint.notifications.impl.notifiers;

import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
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
    private MidpointFunctions midpointFunctions;

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
        if (event.isAdd() || event.isDelete()) {        // general modifications are not supported
            return true;
        } else {
            return false;
        }
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
