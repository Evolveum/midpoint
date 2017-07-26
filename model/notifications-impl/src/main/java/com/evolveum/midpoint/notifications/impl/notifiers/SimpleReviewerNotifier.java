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
import com.evolveum.midpoint.notifications.api.events.CertReviewEvent;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.impl.helpers.CertHelper;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GeneralNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimpleReviewerNotifierType;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Various reviewer-level notifications.
 *
 * @author mederly
 */
@Component
public class SimpleReviewerNotifier extends GeneralNotifier {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleReviewerNotifier.class);

    @Autowired
    private MidpointFunctions midpointFunctions;

    @Autowired
    private CertHelper certHelper;

    @PostConstruct
    public void init() {
        register(SimpleReviewerNotifierType.class);
    }

    @Override
    protected boolean quickCheckApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
        if (!(event instanceof CertReviewEvent)) {
            LOGGER.trace("SimpleReviewerNotifier is not applicable for this kind of event, continuing in the handler chain; event class = " + event.getClass());
            return false;
        }
        CertReviewEvent reviewEvent = (CertReviewEvent) event;
        if (reviewEvent.isAdd()) {
            return true;
        }
        if (reviewEvent.isDelete()) {
            return false;                   // such events are not even created
        }
        AccessCertificationStageDefinitionType stageDef = reviewEvent.getCurrentStageDefinition();
        if (stageDef == null) {
            return false;                   // should not occur
        }
        if (Boolean.FALSE.equals(stageDef.isNotifyOnlyWhenNoDecision())) {
            return true;
        }
        if (reviewEvent.getCasesAwaitingResponseFromRequestee().isEmpty()) {
            return false;
        }
        return true;
    }

    @Override
    protected String getSubject(Event event, GeneralNotifierType generalNotifierType, String transport, Task task, OperationResult result) {
        CertReviewEvent reviewEvent = (CertReviewEvent) event;
        String campaignName = reviewEvent.getCampaignName();
        if (reviewEvent.isAdd()) {
            return "Your review is requested in campaign " + campaignName;
        } else if (reviewEvent.isModify()) {
            return "Deadline for your review in campaign " + campaignName + " is approaching";
        } else {
            throw new IllegalStateException("Unexpected review event type: neither ADD nor MODIFY");
        }
    }

    @Override
    protected String getBody(Event event, GeneralNotifierType generalNotifierType, String transport, Task task, OperationResult result) {
        StringBuilder body = new StringBuilder();
        CertReviewEvent reviewEvent = (CertReviewEvent) event;
        AccessCertificationCampaignType campaign = reviewEvent.getCampaign();

        body.append("You have been requested to provide a review in a certification campaign.");
        body.append("\n");
        body.append("\nReviewer: ").append(textFormatter.formatUserName(reviewEvent.getActualReviewer(), result));
        if (!reviewEvent.getActualReviewer().getOid().equals(reviewEvent.getRequesteeOid())) {
            body.append("\nDeputy: ").append(textFormatter.formatUserName(reviewEvent.getRequestee(), result));
        }
        body.append("\n");
        body.append("\nCampaign: ").append(certHelper.getCampaignNameAndOid(reviewEvent));
        body.append("\nState: ").append(certHelper.formatState(reviewEvent));
        body.append("\n\n");
        AccessCertificationStageType stage = CertCampaignTypeUtil.getCurrentStage(campaign);
        if (stage != null) {
            body.append("Stage start time: ").append(XmlTypeConverter.toDate(stage.getStartTimestamp()));
            body.append("\nStage deadline: ").append(XmlTypeConverter.toDate(stage.getDeadline()));
            if (stage.getEscalationLevel() != null) {
            	body.append("\nEscalation level: ").append(WfContextUtil.getEscalationLevelInfo(stage.getEscalationLevel()));
			}
            if (stage.getDeadline() != null) {
                long delta = XmlTypeConverter.toMillis(stage.getDeadline()) - System.currentTimeMillis();
                if (delta > 0) {
                    if (reviewEvent.isModify()) {
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
            body.append("There are ").append(reviewEvent.getCases().size()).append(" cases to be reviewed by you. ");
            body.append("Out of them, ").append(reviewEvent.getCasesAwaitingResponseFromActualReviewer().size()).append(" are still waiting for your response.");
        }

        return body.toString();
    }

    @Override
    protected Trace getLogger() {
        return LOGGER;
    }

}
