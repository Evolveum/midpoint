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

package com.evolveum.midpoint.certification.impl;

import com.evolveum.midpoint.certification.api.AccessCertificationEventListener;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author mederly
 */
@Component
public class AccCertEventHelper implements AccessCertificationEventListener {

    private Set<AccessCertificationEventListener> listeners = new HashSet<>();

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
    public void onReviewRequested(ObjectReferenceType reviewerRef, List<AccessCertificationCaseType> cases, AccessCertificationCampaignType campaign, Task task, OperationResult result) {
        for (AccessCertificationEventListener listener : listeners) {
            listener.onReviewRequested(reviewerRef, cases, campaign, task, result);
        }
    }

    @Override
    public void onReviewDeadlineApproaching(ObjectReferenceType reviewerRef, List<AccessCertificationCaseType> cases, AccessCertificationCampaignType campaign, Task task, OperationResult result) {
        for (AccessCertificationEventListener listener : listeners) {
            listener.onReviewDeadlineApproaching(reviewerRef, cases, campaign, task, result);
        }
    }

    // returns reviewers for non-closed work items
    public Collection<String> getCurrentActiveReviewers(List<AccessCertificationCaseType> caseList) {
        Set<String> oids = new HashSet<>();
        for (AccessCertificationCaseType aCase : caseList) {
			for (AccessCertificationWorkItemType workItem : aCase.getWorkItem()) {
				if (workItem.getClosedTimestamp() == null) {
					for (ObjectReferenceType reviewerRef : workItem.getReviewerRef()) {
						oids.add(reviewerRef.getOid());
					}
				}
			}
        }
        return oids;
    }

}
