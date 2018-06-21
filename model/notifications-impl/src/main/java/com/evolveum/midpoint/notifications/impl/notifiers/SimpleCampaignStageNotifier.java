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

import com.evolveum.midpoint.notifications.api.events.CertCampaignStageEvent;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.impl.helpers.CertHelper;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GeneralNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimpleCampaignStageNotifierType;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;

/**
 * Various stage-level notifications.
 *
 * @author mederly
 */
@Component
public class SimpleCampaignStageNotifier extends GeneralNotifier {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleCampaignStageNotifier.class);

    @Autowired
    private CertHelper certHelper;

    @PostConstruct
    public void init() {
        register(SimpleCampaignStageNotifierType.class);
    }

    @Override
    protected boolean quickCheckApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
        if (!(event instanceof CertCampaignStageEvent)) {
            LOGGER.trace("SimpleCampaignStageNotifier is not applicable for this kind of event, continuing in the handler chain; event class = " + event.getClass());
            return false;
        }
        return true;
    }

    @Override
    protected String getSubject(Event event, GeneralNotifierType generalNotifierType, String transport, Task task, OperationResult result) {
        CertCampaignStageEvent csEvent = (CertCampaignStageEvent) event;
        String change;
        if (csEvent.isAdd()) {
            change = "started";
        } else if (csEvent.isDelete()) {
            change = "closed";
        } else if (csEvent.isModify()) {
            change = "about to be closed";
        } else {
            throw new IllegalStateException("Unexpected campaign event type: neither ADD nor MODIFY nor DELETE");
        }
        return "Campaign " + csEvent.getCampaignName()
                + " " + certHelper.getStageShortName(csEvent.getCampaign())
                + " " + change;
    }

    @Override
    protected String getBody(Event event, GeneralNotifierType generalNotifierType, String transport, Task task, OperationResult result) {
        StringBuilder body = new StringBuilder();
        CertCampaignStageEvent csEvent = (CertCampaignStageEvent) event;
        AccessCertificationCampaignType campaign = csEvent.getCampaign();

        body.append("A certification campaign stage ");
        if (csEvent.isAdd()) {
            body.append("has been started");
        } else if (csEvent.isDelete()) {
            body.append("has been closed");
        } else if (csEvent.isModify()) {
            body.append("is about to be closed");
        }
        body.append(".");
        body.append("\n\nCampaign: ");
        body.append(certHelper.getCampaignNameAndOid(csEvent));
        body.append("\nState: ");
        body.append(certHelper.formatState(csEvent));

        body.append("\n\nTime: ").append(new Date());     // the event is generated in the real time
        AccessCertificationStageType stage = CertCampaignTypeUtil.getCurrentStage(campaign);
        if (stage != null) {
            body.append("\n\nStage start time: ").append(XmlTypeConverter.toDate(stage.getStartTimestamp()));
            body.append("\nStage deadline time: ").append(XmlTypeConverter.toDate(stage.getDeadline()));
            if (csEvent.isModify() && stage.getDeadline() != null) {
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
        if (csEvent.isAdd() || csEvent.isDelete()) {
            body.append("\nRequester: ").append(formatRequester(event, result));
            body.append("\nOperation status: ").append(certHelper.formatStatus(csEvent));
            body.append("\n");
        }

        body.append("\n");
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
