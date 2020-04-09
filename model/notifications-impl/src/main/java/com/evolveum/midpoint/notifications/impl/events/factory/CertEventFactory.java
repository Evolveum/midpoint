/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.events.factory;

import java.util.List;

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
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

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

    public CertCampaignEvent createOnCampaignStartEvent(AccessCertificationCampaignType campaign, Task task) {
        CertCampaignEventImpl event = new CertCampaignEventImpl(idGenerator, campaign, EventOperationType.ADD);
        fillInEvent(campaign, task, event);
        return event;
    }

    private void fillInEvent(AccessCertificationCampaignType campaign, Task task, AccessCertificationEventImpl event) {
        event.setRequestee(new SimpleObjectRefImpl(notificationsUtil, campaign.getOwnerRef()));
        if (task != null) {
            event.setRequester(new SimpleObjectRefImpl(notificationsUtil, task.getOwner()));
            event.setChannel(task.getChannel());
        }
    }

    private void fillInReviewerRelatedEvent(ObjectReferenceType reviewerOrDeputyRef, ObjectReferenceType actualReviewerRef,
            Task task, CertReviewEventImpl event) {
        event.setRequestee(new SimpleObjectRefImpl(notificationsUtil, reviewerOrDeputyRef));
        event.setRequester(new SimpleObjectRefImpl(notificationsUtil, task.getOwner()));                    // or campaign owner?
        event.setActualReviewer(new SimpleObjectRefImpl(notificationsUtil, actualReviewerRef));
    }

    public CertCampaignEvent createOnCampaignEndEvent(AccessCertificationCampaignType campaign, Task task) {
        CertCampaignEventImpl event = new CertCampaignEventImpl(idGenerator, campaign, EventOperationType.DELETE);
        fillInEvent(campaign, task, event);
        return event;
    }

    public CertCampaignStageEvent createOnCampaignStageStartEvent(AccessCertificationCampaignType campaign, Task task) {
        CertCampaignStageEventImpl event = new CertCampaignStageEventImpl(idGenerator, campaign, EventOperationType.ADD);
        fillInEvent(campaign, task, event);
        return event;
    }

    public CertCampaignStageEvent createOnCampaignStageDeadlineApproachingEvent(AccessCertificationCampaignType campaign, Task task) {
        CertCampaignStageEventImpl event = new CertCampaignStageEventImpl(idGenerator, campaign, EventOperationType.MODIFY);
        fillInEvent(campaign, task, event);
        return event;
    }

    public CertCampaignStageEvent createOnCampaignStageEndEvent(AccessCertificationCampaignType campaign, Task task) {
        CertCampaignStageEventImpl event = new CertCampaignStageEventImpl(idGenerator, campaign, EventOperationType.DELETE);
        fillInEvent(campaign, task, event);
        return event;
    }

    public CertReviewEvent createReviewRequestedEvent(ObjectReferenceType reviewerOrDeputyRef, ObjectReferenceType actualReviewerRef,
            List<AccessCertificationCaseType> cases, AccessCertificationCampaignType campaign, Task task) {
        CertReviewEventImpl event = new CertReviewEventImpl(idGenerator, cases, campaign, EventOperationType.ADD);
        fillInReviewerRelatedEvent(reviewerOrDeputyRef, actualReviewerRef, task, event);
        return event;
    }

    public CertReviewEvent createReviewDeadlineApproachingEvent(ObjectReferenceType reviewerOrDeputyRef, ObjectReferenceType actualReviewerRef,
            List<AccessCertificationCaseType> cases, AccessCertificationCampaignType campaign, Task task) {
        CertReviewEventImpl event = new CertReviewEventImpl(idGenerator, cases, campaign, EventOperationType.MODIFY);
        fillInReviewerRelatedEvent(reviewerOrDeputyRef, actualReviewerRef, task, event);
        return event;
    }
}
