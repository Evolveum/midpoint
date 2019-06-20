/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.api.request;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 */
public class DelegateWorkItemsRequest extends Request {

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

	public DelegateWorkItemsRequest(@NotNull String caseOid, WorkItemEventCauseInformationType causeInformation) {
		super(caseOid, causeInformation);
	}

	@NotNull
	public Collection<SingleDelegation> getDelegations() {
		return delegations;
	}
}
