/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.api.request;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemDelegationMethodType.REPLACE_ASSIGNEES;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Request to delegate work items (of a given case).
 */
public class DelegateWorkItemsRequest extends Request {

    /**
     * The delegation time to use. Later we may move this up to Request (when needed).
     */
    @Nullable private final XMLGregorianCalendar now;

    public static class SingleDelegation {

        /** The work item which is delegated. */
        private final long workItemId;

        /** Details of the delegation. */
        @NotNull private final WorkItemDelegationRequestType delegationRequest;

        /** TODO what is this?! */
        private final WorkItemEscalationLevelType targetEscalationInfo;

        /** TODO */
        private final Duration newDuration;

        public SingleDelegation(
                long workItemId,
                @NotNull WorkItemDelegationRequestType delegationRequest,
                WorkItemEscalationLevelType targetEscalationInfo,
                Duration newDuration) {
            this.workItemId = workItemId;
            this.delegationRequest = delegationRequest;
            this.targetEscalationInfo = targetEscalationInfo;
            this.newDuration = newDuration;
        }

        public long getWorkItemId() {
            return workItemId;
        }

        @NotNull
        public List<ObjectReferenceType> getDelegates() {
            return delegationRequest.getDelegate();
        }

        @NotNull
        public WorkItemDelegationMethodType getMethod() {
            return Objects.requireNonNullElse(
                    delegationRequest.getMethod(), REPLACE_ASSIGNEES);
        }

        public String getComment() {
            return delegationRequest.getComment();
        }

        public WorkItemEscalationLevelType getTargetEscalationInfo() {
            return targetEscalationInfo;
        }

        public Duration getNewDuration() {
            return newDuration;
        }

        @Override
        public String toString() {
            return "SingleDelegation{" +
                    "workItemId=" + workItemId +
                    ", delegationRequest=" + delegationRequest +
                    ", targetEscalationInfo=" + targetEscalationInfo +
                    ", newDuration=" + newDuration +
                    '}';
        }
    }

    @NotNull private final Collection<SingleDelegation> delegations = new ArrayList<>();

    public DelegateWorkItemsRequest(@NotNull String caseOid, WorkItemEventCauseInformationType causeInformation,
            @Nullable XMLGregorianCalendar now) {
        super(caseOid, causeInformation);
        this.now = now;
    }

    @NotNull
    public Collection<SingleDelegation> getDelegations() {
        return delegations;
    }

    @Nullable
    public XMLGregorianCalendar getNow() {
        return now;
    }

    @Override
    public String toString() {
        return "DelegateWorkItemsRequest{" +
                "now=" + now +
                ", delegations=" + delegations +
                ", caseOid='" + caseOid + '\'' +
                ", causeInformation=" + causeInformation +
                '}';
    }
}
