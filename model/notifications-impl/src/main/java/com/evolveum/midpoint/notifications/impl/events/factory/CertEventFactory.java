/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.events.factory;

import java.util.List;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.events.CertCampaignEvent;
import com.evolveum.midpoint.notifications.api.events.CertCampaignStageEvent;
import com.evolveum.midpoint.notifications.api.events.CertReviewEvent;
import com.evolveum.midpoint.notifications.impl.NotificationFunctionsImpl;
import com.evolveum.midpoint.notifications.impl.SimpleObjectRefImpl;
import com.evolveum.midpoint.notifications.impl.events.AccessCertificationEventImpl;
import com.evolveum.midpoint.notifications.impl.events.CertCampaignEventImpl;
import com.evolveum.midpoint.notifications.impl.events.CertCampaignStageEventImpl;
import com.evolveum.midpoint.notifications.impl.events.CertReviewEventImpl;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.task.api.Task;

/**
 * Creates certification events
 *
 * TODO do we really need this class?
 */
@Component
public class CertEventFactory {

    @Autowired
    private LightweightIdentifierGenerator idGenerator;

    @Autowired
    private NotificationFunctionsImpl notificationsUtil;

    public CertCampaignEvent createOnCampaignStartEvent(AccessCertificationCampaignType campaign, Task task, OperationResult result) {
        CertCampaignEventImpl event = new CertCampaignEventImpl(idGenerator, campaign, EventOperationType.ADD);
        fillInEvent(campaign, task, event, result);
        return event;
    }

    private void fillInEvent(AccessCertificationCampaignType campaign, Task task, AccessCertificationEventImpl event, OperationResult result) {
        event.setRequestee(new SimpleObjectRefImpl(notificationsUtil, campaign.getOwnerRef()));
        if (task != null) {
            PrismObject<? extends FocusType> taskOwner = task.getOwner(result);
            event.setRequester(new SimpleObjectRefImpl(notificationsUtil, taskOwner));
            event.setChannel(task.getChannel());
        }
    }

    private void fillInReviewerRelatedEvent(ObjectReferenceType reviewerOrDeputyRef, ObjectReferenceType actualReviewerRef,
            Task task, CertReviewEventImpl event, OperationResult result) {
        event.setRequestee(new SimpleObjectRefImpl(notificationsUtil, reviewerOrDeputyRef));
        PrismObject<? extends FocusType> taskOwner = task.getOwner(result);
        event.setRequester(new SimpleObjectRefImpl(notificationsUtil, taskOwner));
        event.setActualReviewer(new SimpleObjectRefImpl(notificationsUtil, actualReviewerRef));
    }

    public CertCampaignEvent createOnCampaignEndEvent(AccessCertificationCampaignType campaign, Task task, OperationResult result) {
        CertCampaignEventImpl event = new CertCampaignEventImpl(idGenerator, campaign, EventOperationType.DELETE);
        fillInEvent(campaign, task, event, result);
        return event;
    }

    public CertCampaignStageEvent createOnCampaignStageStartEvent(AccessCertificationCampaignType campaign, Task task, OperationResult result) {
        CertCampaignStageEventImpl event = new CertCampaignStageEventImpl(idGenerator, campaign, EventOperationType.ADD);
        fillInEvent(campaign, task, event, result);
        return event;
    }

    public CertCampaignStageEvent createOnCampaignStageDeadlineApproachingEvent(AccessCertificationCampaignType campaign, Task task, OperationResult result) {
        CertCampaignStageEventImpl event = new CertCampaignStageEventImpl(idGenerator, campaign, EventOperationType.MODIFY);
        fillInEvent(campaign, task, event, result);
        return event;
    }

    public CertCampaignStageEvent createOnCampaignStageEndEvent(AccessCertificationCampaignType campaign, Task task, OperationResult result) {
        CertCampaignStageEventImpl event = new CertCampaignStageEventImpl(idGenerator, campaign, EventOperationType.DELETE);
        fillInEvent(campaign, task, event, result);
        return event;
    }

    public CertReviewEvent createReviewRequestedEvent(ObjectReferenceType reviewerOrDeputyRef, ObjectReferenceType actualReviewerRef,
            List<AccessCertificationCaseType> cases, AccessCertificationCampaignType campaign, Task task, OperationResult result) {
        CertReviewEventImpl event = new CertReviewEventImpl(idGenerator, cases, campaign, EventOperationType.ADD);
        fillInReviewerRelatedEvent(reviewerOrDeputyRef, actualReviewerRef, task, event, result);
        return event;
    }

    public CertReviewEvent createReviewDeadlineApproachingEvent(ObjectReferenceType reviewerOrDeputyRef, ObjectReferenceType actualReviewerRef,
            List<AccessCertificationCaseType> cases, AccessCertificationCampaignType campaign, Task task, OperationResult result) {
        CertReviewEventImpl event = new CertReviewEventImpl(idGenerator, cases, campaign, EventOperationType.MODIFY);
        fillInReviewerRelatedEvent(reviewerOrDeputyRef, actualReviewerRef, task, event, result);
        return event;
    }
}
