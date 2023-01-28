/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.dto;

import com.evolveum.midpoint.certification.api.OutcomeUtils;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.cases.ApprovalContextUtil;
import com.evolveum.midpoint.schema.util.cases.WorkItemTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing a particular workItem.
 *
 * TODO cleanup a bit
 */
public class CertWorkItemDto extends CertCaseOrWorkItemDto {

    public static final String F_COMMENT = "comment";
    @SuppressWarnings("unused")
    public static final String F_RESPONSE = "response";
    public static final String F_REVIEWER_NAME = "reviewerName";

    @NotNull private final AccessCertificationWorkItemType workItem;
    private List<ObjectReferenceType> reviewerRefList;
    private List<String> reviewerNameList = new ArrayList<>();

    public CertWorkItemDto(@NotNull AccessCertificationWorkItemType workItem, @NotNull PageBase page) {
        //noinspection ConstantConditions
        super(CertCampaignTypeUtil.getCase(workItem), page);
        this.workItem = workItem;
        this.reviewerRefList = workItem.getAssigneeRef();
        computeReviewerNameList();
    }

    public String getComment() {
        return WorkItemTypeUtil.getComment(workItem);
    }

    public void setComment(String value) {
        if (workItem.getOutput() == null) {
            workItem.beginOutput().comment(value);
        } else {
            workItem.getOutput().comment(value);
        }
    }

    public AccessCertificationResponseType getResponse() {
        return OutcomeUtils.fromUri(WorkItemTypeUtil.getOutcome(workItem));
    }

    public long getWorkItemId() {
        return workItem.getId();
    }

    public Integer getEscalationLevelNumber() {
        int n = WorkItemTypeUtil.getEscalationLevelNumber(workItem);
        return n != 0 ? n : null;
    }

    public String getEscalationLevelInfo() {
        return ApprovalContextUtil.getEscalationLevelInfo(workItem);
    }

    public void computeReviewerNameList(){
        if (reviewerRefList == null){
            return;
        }
        reviewerRefList.forEach(reviewerRef -> {
            reviewerNameList.add(WebComponentUtil.getDisplayNameAndName(reviewerRef));
        });
    }

    public List<String> getReviewerNameList() {
        return reviewerNameList;
    }

    public void setReviewerName(List<String> reviewerNameList) {
        this.reviewerNameList = reviewerNameList;
    }

    public List<ObjectReferenceType> getReviewerRefList() {
        return reviewerRefList;
    }

    public void setReviewerRefList(List<ObjectReferenceType> reviewerRefList) {
        this.reviewerRefList = reviewerRefList;
    }
}
