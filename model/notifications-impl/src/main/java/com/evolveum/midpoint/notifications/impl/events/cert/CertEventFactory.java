/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
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

    protected void fillInReviewerRelatedEvent(ObjectReferenceType reviewerOrDeputyRef, ObjectReferenceType actualReviewerRef,
			Task task, CertReviewEvent event) {
        event.setRequestee(new SimpleObjectRefImpl(notificationsUtil, reviewerOrDeputyRef));
        event.setRequester(new SimpleObjectRefImpl(notificationsUtil, task.getOwner()));        			// or campaign owner?
		event.setActualReviewer(new SimpleObjectRefImpl(notificationsUtil, actualReviewerRef));
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

    public CertReviewEvent createReviewRequestedEvent(ObjectReferenceType reviewerOrDeputyRef, ObjectReferenceType actualReviewerRef,
            List<AccessCertificationCaseType> cases, AccessCertificationCampaignType campaign, Task task, OperationResult result) {
        CertReviewEvent event = new CertReviewEvent(idGenerator, cases, campaign, EventOperationType.ADD);
        fillInReviewerRelatedEvent(reviewerOrDeputyRef, actualReviewerRef, task, event);
        return event;
    }

    public CertReviewEvent createReviewDeadlineApproachingEvent(ObjectReferenceType reviewerOrDeputyRef, ObjectReferenceType actualReviewerRef,
            List<AccessCertificationCaseType> cases, AccessCertificationCampaignType campaign, Task task, OperationResult result) {
        CertReviewEvent event = new CertReviewEvent(idGenerator, cases, campaign, EventOperationType.MODIFY);
        fillInReviewerRelatedEvent(reviewerOrDeputyRef, actualReviewerRef, task, event);
        return event;
    }
}
