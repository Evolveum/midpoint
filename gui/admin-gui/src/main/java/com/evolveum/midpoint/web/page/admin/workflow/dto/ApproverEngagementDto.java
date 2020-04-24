/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.workflow.dto;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serializable;

/**
 * GUI-friendly information about an engagement of given approver in a historic, current or future execution of an approval stage.
 */
public class ApproverEngagementDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull private final ObjectReferenceType approverRef;                // with the whole object, if possible
    @Nullable private AbstractWorkItemOutputType output;
    @Nullable private XMLGregorianCalendar completedAt;
    @Nullable private ObjectReferenceType completedBy;                // the user that really completed the work item originally assigned to that approver
    @Nullable private ObjectReferenceType attorney;                   // the attorney (of completedBy)
    private boolean last;

    ApproverEngagementDto(@NotNull ObjectReferenceType approverRef) {
        this.approverRef = approverRef;
    }

    @NotNull
    public ObjectReferenceType getApproverRef() {
        return approverRef;
    }

    @Nullable
    public AbstractWorkItemOutputType getOutput() {
        return output;
    }

    public void setOutput(@Nullable AbstractWorkItemOutputType output) {
        this.output = output;
    }

    @Nullable
    public XMLGregorianCalendar getCompletedAt() {
        return completedAt;
    }

    void setCompletedAt(@Nullable XMLGregorianCalendar completedAt) {
        this.completedAt = completedAt;
    }

    @Nullable
    public ObjectReferenceType getCompletedBy() {
        return completedBy;
    }

    void setCompletedBy(@Nullable ObjectReferenceType completedBy) {
        this.completedBy = completedBy;
    }

    @Nullable
    public ObjectReferenceType getAttorney() {
        return attorney;
    }

    void setAttorney(@Nullable ObjectReferenceType attorney) {
        this.attorney = attorney;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }
}
