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

package com.evolveum.midpoint.notifications.impl.events.cert;

import com.evolveum.midpoint.notifications.api.events.AccessCertificationEvent;
import com.evolveum.midpoint.notifications.api.events.CertCampaignEvent;
import com.evolveum.midpoint.notifications.api.events.CertCampaignStageEvent;
import com.evolveum.midpoint.notifications.api.events.CertReviewEvent;
import com.evolveum.midpoint.notifications.impl.NotificationFunctionsImpl;
import com.evolveum.midpoint.notifications.impl.SimpleObjectRefImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author mederly
 */
@Component
public class CertEventFactory {

    @Autowired
    private LightweightIdentifierGenerator idGenerator;

    @Autowired
    private NotificationFunctionsImpl notificationsUtil;

    public CertCampaignEvent createOnCampaignStartEvent(AccessCertificationCampaignType campaign, Task task, OperationResult result) {
        CertCampaignEvent event = new CertCampaignEvent(idGenerator, campaign, EventOperationType.ADD);
        fillInEvent(campaign, task, event);
        return event;
    }

    protected void fillInEvent(AccessCertificationCampaignType campaign, Task task, AccessCertificationEvent event) {
        event.setRequestee(new SimpleObjectRefImpl(notificationsUtil, campaign.getOwnerRef()));
        if (task != null) {
            event.setRequester(new SimpleObjectRefImpl(notificationsUtil, task.getOwner()));
            event.setChannel(task.getChannel());
        }
    }

    protected void fillInReviewerRelatedEvent(ObjectReferenceType reviewerRef, Task task, AccessCertificationEvent event) {
        event.setRequestee(new SimpleObjectRefImpl(notificationsUtil, reviewerRef));
        event.setRequester(new SimpleObjectRefImpl(notificationsUtil, task.getOwner()));        // or campaign owner?
    }

    public CertCampaignEvent createOnCampaignEndEvent(AccessCertificationCampaignType campaign, Task task, OperationResult result) {
        CertCampaignEvent event = new CertCampaignEvent(idGenerator, campaign, EventOperationType.DELETE);
        fillInEvent(campaign, task, event);
        return event;
    }

    public CertCampaignStageEvent createOnCampaignStageStartEvent(AccessCertificationCampaignType campaign, Task task, OperationResult result) {
        CertCampaignStageEvent event = new CertCampaignStageEvent(idGenerator, campaign, EventOperationType.ADD);
        fillInEvent(campaign, task, event);
        return event;
    }

    public CertCampaignStageEvent createOnCampaignStageDeadlineApproachingEvent(AccessCertificationCampaignType campaign, Task task, OperationResult result) {
        CertCampaignStageEvent event = new CertCampaignStageEvent(idGenerator, campaign, EventOperationType.MODIFY);
        fillInEvent(campaign, task, event);
        return event;
    }

    public CertCampaignStageEvent createOnCampaignStageEndEvent(AccessCertificationCampaignType campaign, Task task, OperationResult result) {
        CertCampaignStageEvent event = new CertCampaignStageEvent(idGenerator, campaign, EventOperationType.DELETE);
        fillInEvent(campaign, task, event);
        return event;
    }

    public CertReviewEvent createReviewRequestedEvent(ObjectReferenceType reviewerRef, List<AccessCertificationCaseType> cases, AccessCertificationCampaignType campaign, Task task, OperationResult result) {
        CertReviewEvent event = new CertReviewEvent(idGenerator, cases, campaign, EventOperationType.ADD);
        fillInReviewerRelatedEvent(reviewerRef, task, event);
        return event;
    }

    public CertReviewEvent createReviewDeadlineApproachingEvent(ObjectReferenceType reviewerRef, List<AccessCertificationCaseType> cases, AccessCertificationCampaignType campaign, Task task, OperationResult result) {
        CertReviewEvent event = new CertReviewEvent(idGenerator, cases, campaign, EventOperationType.MODIFY);
        fillInReviewerRelatedEvent(reviewerRef, task, event);
        return event;
    }
}
