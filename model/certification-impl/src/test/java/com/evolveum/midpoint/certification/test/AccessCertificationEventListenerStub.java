package com.evolveum.midpoint.certification.test;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.certification.api.AccessCertificationEventListener;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public final class AccessCertificationEventListenerStub implements AccessCertificationEventListener {

    private final List<ReviewRequestEvent> reviewRequestedEvents;

    public AccessCertificationEventListenerStub() {
        this.reviewRequestedEvents = new ArrayList<>();
    }

    @Override
    public void onCampaignStart(AccessCertificationCampaignType campaign, Task task, OperationResult result) {
    }

    @Override
    public void onCampaignEnd(AccessCertificationCampaignType campaign, Task task, OperationResult result) {
    }

    @Override
    public void onCampaignStageStart(AccessCertificationCampaignType campaign, Task task, OperationResult result) {
    }

    @Override
    public void onCampaignStageDeadlineApproaching(AccessCertificationCampaignType campaign, Task task,
            OperationResult result) {
    }

    @Override
    public void onCampaignStageEnd(AccessCertificationCampaignType campaign, Task task, OperationResult result) {
    }

    @Override
    public void onReviewRequested(ObjectReferenceType reviewerOrDeputyRef, ObjectReferenceType actualReviewerRef,
            List<AccessCertificationCaseType> cases, AccessCertificationCampaignType campaign, Task task,
            OperationResult result) {
        this.reviewRequestedEvents.add(new ReviewRequestEvent(
                reviewerOrDeputyRef.getOid(),
                actualReviewerRef.getOid(),
                cases.stream().map(c -> c.asPrismContainerValue().getId()).toList(),
                campaign.getOid()));
    }

    @Override
    public void onReviewDeadlineApproaching(ObjectReferenceType reviewerOrDeputyRef,
            ObjectReferenceType actualReviewerRef, List<AccessCertificationCaseType> cases,
            AccessCertificationCampaignType campaign, Task task, OperationResult result) {
    }

    boolean reviewRequestedEventSentTo(String reviewerOrDeputy, String actualReviewer,
            List<Long> casesIds, String campaignOid) {
        return this.reviewRequestedEvents.contains(
                new ReviewRequestEvent(reviewerOrDeputy, actualReviewer, casesIds, campaignOid));
    }

    void reset() {
        this.reviewRequestedEvents.clear();
    }

    private record ReviewRequestEvent(
            String reviewerOrDeputy,
            String actualReviewer,
            List<Long> casesIds,
            String campaignOid) {
    }

}
