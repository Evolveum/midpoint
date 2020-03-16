/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.events;

import com.evolveum.midpoint.notifications.api.events.CertReviewEvent;
import com.evolveum.midpoint.notifications.api.events.SimpleObjectRef;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WorkItemTypeUtil;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * Event related to given certification case.
 *  ADD = case created (first stage)
 *  MODIFY = stage deadline is approaching
 *  DELETE = stage closed
 *
 * @author mederly
 */
public class CertReviewEventImpl extends AccessCertificationEventImpl implements CertReviewEvent {

    private List<AccessCertificationCaseType> cases;
    /**
     * Actual reviewer - the person which the work item is assigned to. I.e. _not_ his deputy.
     * Must be set to non-null value just after instantiation (TODO do more cleanly)
     */
    private SimpleObjectRef actualReviewer;

    public CertReviewEventImpl(LightweightIdentifierGenerator idGenerator, List<AccessCertificationCaseType> cases,
            AccessCertificationCampaignType campaign, EventOperationType opType) {
        super(idGenerator, campaign, opType);
        this.cases = cases;
    }

    @Override
    public SimpleObjectRef getActualReviewer() {
        return actualReviewer;
    }

    public void setActualReviewer(SimpleObjectRef actualReviewer) {
        this.actualReviewer = actualReviewer;
    }

    @Override
    public boolean isCategoryType(EventCategoryType eventCategory) {
        return super.isCategoryType(eventCategory) ||
                EventCategoryType.CERT_CASE_EVENT.equals(eventCategory);
    }

    @NotNull
    @Override
    public Collection<AccessCertificationCaseType> getCasesAwaitingResponseFromActualReviewer() {
        List<AccessCertificationCaseType> rv = new ArrayList<>();
        for (AccessCertificationCaseType aCase : cases) {
            if (awaitsResponseFrom(aCase, actualReviewer.getOid(), campaign.getStageNumber())) {
                rv.add(aCase);
            }
        }
        return rv;
    }

    private boolean awaitsResponseFrom(AccessCertificationCaseType aCase, String reviewerOid, int currentStageNumber) {
        for (AccessCertificationWorkItemType workItem : aCase.getWorkItem()) {
            if (workItem.getStageNumber() == currentStageNumber
                    && WorkItemTypeUtil.getOutcome(workItem) == null
                    && workItem.getCloseTimestamp() == null
                    && ObjectTypeUtil.containsOid(workItem.getAssigneeRef(), reviewerOid)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<AccessCertificationCaseType> getCases() {
        return cases;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(this.getClass(), indent);
        debugDumpCommon(sb, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "cases", cases, indent + 1);
        return sb.toString();
    }
}
