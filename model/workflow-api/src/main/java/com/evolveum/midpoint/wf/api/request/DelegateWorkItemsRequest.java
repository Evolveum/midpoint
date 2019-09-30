/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.api.request;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 */
public class DelegateWorkItemsRequest extends Request {

	@Nullable private final XMLGregorianCalendar now;       // later we might move this up to Request (when needed)

	public static class SingleDelegation {
		private final long workItemId;
		@NotNull private final List<ObjectReferenceType> delegates;
		@NotNull private final WorkItemDelegationMethodType method;
		private final WorkItemEscalationLevelType targetEscalationInfo;
		private final Duration newDuration;

		public SingleDelegation(long workItemId,
				@NotNull List<ObjectReferenceType> delegates,
				@NotNull WorkItemDelegationMethodType method,
				WorkItemEscalationLevelType targetEscalationInfo, Duration newDuration) {
			this.workItemId = workItemId;
			this.delegates = delegates;
			this.method = method;
			this.targetEscalationInfo = targetEscalationInfo;
			this.newDuration = newDuration;
		}

		public long getWorkItemId() {
			return workItemId;
		}

		@NotNull
		public List<ObjectReferenceType> getDelegates() {
			return delegates;
		}

		@NotNull
		public WorkItemDelegationMethodType getMethod() {
			return method;
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
					", delegates=" + delegates +
					", method=" + method +
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
