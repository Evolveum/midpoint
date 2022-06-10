/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.wf.impl;

import com.evolveum.midpoint.util.CheckedRunnable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

public class ApprovalInstruction {

    public final ExpectedWorkItem expectedWorkItem;
    public final boolean approval;
    public final String approverOid;
    public final String comment;
    public final CheckedRunnable beforeApproval, afterApproval;

    public ApprovalInstruction(ExpectedWorkItem expectedWorkItem, boolean approval, String approverOid, String comment,
            CheckedRunnable beforeApproval, CheckedRunnable afterApproval) {
        this.expectedWorkItem = expectedWorkItem;
        this.approval = approval;
        this.approverOid = approverOid;
        this.comment = comment;
        this.beforeApproval = beforeApproval;
        this.afterApproval = afterApproval;
    }

    public ApprovalInstruction(ExpectedWorkItem expectedWorkItem, boolean approval, String approverOid, String comment) {
        this(expectedWorkItem, approval, approverOid, comment, null, null);
    }

    public boolean matches(CaseWorkItemType actualWorkItem) {
        return expectedWorkItem == null || expectedWorkItem.matches(actualWorkItem);
    }

    @Override
    public String toString() {
        return "ApprovalInstruction{" +
                "expectedWorkItem=" + expectedWorkItem +
                ", approval=" + approval +
                ", approverOid='" + approverOid + '\'' +
                ", comment='" + comment + '\'' +
                '}';
    }
}
