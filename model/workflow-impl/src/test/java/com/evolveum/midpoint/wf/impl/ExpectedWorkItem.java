/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.wf.impl;

import com.evolveum.midpoint.schema.util.cases.ApprovalContextUtil;
import com.evolveum.midpoint.schema.util.cases.CaseTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

import static com.evolveum.midpoint.prism.Referencable.getOid;

public class ExpectedWorkItem {

    final String assigneeOid;
    final String targetOid;
    final ExpectedTask task;

    public ExpectedWorkItem(String assigneeOid, String targetOid, ExpectedTask task) {
        this.assigneeOid = assigneeOid;
        this.targetOid = targetOid;
        this.task = task;
    }

    public boolean matches(CaseWorkItemType actualWorkItem) {
        if (!assigneeOid.equals(actualWorkItem.getOriginalAssigneeRef().getOid())) {
            return false;
        }
        if (targetOid != null && !targetOid.equals(getOid(ApprovalContextUtil.getTargetRef(actualWorkItem)))) {
            return false;
        }
        CaseType actualCase = CaseTypeUtil.getCaseRequired(actualWorkItem);
        return task.processName.equals(actualCase.getName().getOrig());
    }

    @Override
    public String toString() {
        return "ExpectedWorkItem{" +
                "assigneeOid='" + assigneeOid + '\'' +
                ", targetOid='" + targetOid + '\'' +
                ", task=" + task +
                '}';
    }
}
