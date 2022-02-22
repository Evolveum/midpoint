/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.impl;

import com.evolveum.midpoint.certification.api.AccessCertificationEventListener;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class AccCertEventHelper implements AccessCertificationEventListener {

    private Set<AccessCertificationEventListener> listeners = new HashSet<>();

    @SuppressWarnings("WeakerAccess")
    public void registerEventListener(AccessCertificationEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void onCampaignStart(AccessCertificationCampaignType campaign, Task task, OperationResult result) {
        for (AccessCertificationEventListener listener : listeners) {
            listener.onCampaignStart(campaign, task, result);
        }
    }

    @Override
    public void onCampaignEnd(AccessCertificationCampaignType campaign, Task task, OperationResult result) {
        for (AccessCertificationEventListener listener : listeners) {
            listener.onCampaignEnd(campaign, task, result);
        }
    }

    @Override
    public void onCampaignStageStart(AccessCertificationCampaignType campaign, Task task, OperationResult result) {
        for (AccessCertificationEventListener listener : listeners) {
            listener.onCampaignStageStart(campaign, task, result);
        }
    }

    @Override
    public void onCampaignStageDeadlineApproaching(AccessCertificationCampaignType campaign, Task task, OperationResult result) {
        for (AccessCertificationEventListener listener : listeners) {
            listener.onCampaignStageDeadlineApproaching(campaign, task, result);
        }
    }

    @Override
    public void onCampaignStageEnd(AccessCertificationCampaignType campaign, Task task, OperationResult result) {
        for (AccessCertificationEventListener listener : listeners) {
            listener.onCampaignStageEnd(campaign, task, result);
        }
    }

    @Override
    public void onReviewRequested(ObjectReferenceType reviewerOrDeputyRef, ObjectReferenceType actualReviewerRef,
            List<AccessCertificationCaseType> cases, AccessCertificationCampaignType campaign, Task task, OperationResult result) {
        for (AccessCertificationEventListener listener : listeners) {
            listener.onReviewRequested(reviewerOrDeputyRef, actualReviewerRef, cases, campaign, task, result);
        }
    }

    @Override
    public void onReviewDeadlineApproaching(ObjectReferenceType reviewerOrDeputyRef, ObjectReferenceType actualReviewerRef,
            List<AccessCertificationCaseType> cases, AccessCertificationCampaignType campaign, Task task, OperationResult result) {
        for (AccessCertificationEventListener listener : listeners) {
            listener.onReviewDeadlineApproaching(reviewerOrDeputyRef, actualReviewerRef, cases, campaign, task, result);
        }
    }
}
