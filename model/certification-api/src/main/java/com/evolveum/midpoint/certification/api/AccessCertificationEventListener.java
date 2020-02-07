/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.api;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import java.util.List;

/**
 * An interface through which external observers can be notified about certification related events.
 *
 * EXPERIMENTAL. This interface will probably change in near future.
 *
 * @author mederly
 */
public interface AccessCertificationEventListener {

    /**
     * This method is called by certification module when a certification campaign starts.
     *  @param campaign TODO
     * @param task
     * @param result implementer should report its result here
     */
    void onCampaignStart(AccessCertificationCampaignType campaign, Task task, OperationResult result);

    /**
     * This method is called by certification module when a certification campaign ends (i.e. is closed).
     *  @param campaign TODO
     * @param task
     * @param result implementer should report its result here
     */
    void onCampaignEnd(AccessCertificationCampaignType campaign, Task task, OperationResult result);

    /**
     * This method is called by certification module when a certification campaign stage starts.
     *  @param campaign TODO
     * @param task
     * @param result implementer should report its result here
     */
    void onCampaignStageStart(AccessCertificationCampaignType campaign, Task task, OperationResult result);

    /**
     * This method is called by certification module when a certification campaign stage deadline is approaching.
     * The strategy for calling this method (e.g. how often and exactly how many days/hours before the deadline)
     * will be configurable in the future.
     *  @param campaign TODO
     * @param task
     * @param result implementer should report its result here
     */
    void onCampaignStageDeadlineApproaching(AccessCertificationCampaignType campaign, Task task, OperationResult result);

    /**
     * This method is called by certification module when a certification campaign stage ends (i.e. is closed).
     *  @param campaign TODO
     * @param task
     * @param result implementer should report its result here
     */
    void onCampaignStageEnd(AccessCertificationCampaignType campaign, Task task, OperationResult result);

    /**
     * This method is called when a review is requested
     * @param result implementer should report its result here
     */
    void onReviewRequested(ObjectReferenceType reviewerOrDeputyRef, ObjectReferenceType actualReviewerRef,
            List<AccessCertificationCaseType> cases, AccessCertificationCampaignType campaign, Task task, OperationResult result);

    /**
     * This method is called by certification module when a certification case review deadline is approaching.
     * The strategy for calling this method (e.g. how often and exactly how many days/hours before the deadline)
     * will be configurable in the future.
     * @param result implementer should report its result here
     */
    void onReviewDeadlineApproaching(ObjectReferenceType reviewerOrDeputyRef, ObjectReferenceType actualReviewerRef,
            List<AccessCertificationCaseType> cases, AccessCertificationCampaignType campaign, Task task, OperationResult result);

    /*
     * This method is called by certification module when a certification case is decided.
     * I.e. when there we don't expect any changes in the state of this case as part of the approval processes.
     * (Either when it's finally rejected or when last stage is closed.)
     * Changes related to remediation are not covered here.
     *
     * NOT IMPLEMENTED YET.
     *
     * @param aCase TODO
     * @param campaign TODO
     * @param result implementer should report its result here
     */
    //void onCaseFinalDecision(AccessCertificationCaseType aCase, AccessCertificationCampaignType campaign, Task task, OperationResult result);

    /*
     *  TODO
     *  onCaseDecision
     *  onCaseRemediation
     */

}
