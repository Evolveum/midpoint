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

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemEventCauseInformationType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 *
 */
public class CompleteWorkItemsRequest extends Request {

	public static class SingleCompletion {
		private final long workItemId;
		@NotNull private final String outcome;
		private final String comment;
		private final ObjectDelta<? extends ObjectType> additionalDelta;

		public SingleCompletion(long workItemId, @NotNull String outcome, String comment,
				ObjectDelta<? extends ObjectType> additionalDelta) {
			this.workItemId = workItemId;
			this.outcome = outcome;
			this.comment = comment;
			this.additionalDelta = additionalDelta;
		}

		public long getWorkItemId() {
			return workItemId;
		}

		@NotNull
		public String getOutcome() {
			return outcome;
		}

		public String getComment() {
			return comment;
		}

		public ObjectDelta<? extends ObjectType> getAdditionalDelta() {
			return additionalDelta;
		}

		@Override
		public String toString() {
			return "SingleCompletion{" +
					"workItemId=" + workItemId +
					", outcome='" + outcome + '\'' +
					", comment='" + comment + '\'' +
					", additionalDelta=" + additionalDelta +
					'}';
		}
	}

	@NotNull private final Collection<SingleCompletion> completions = new ArrayList<>();

	public CompleteWorkItemsRequest(@NotNull String caseOid, WorkItemEventCauseInformationType causeInformation) {
		super(caseOid, causeInformation);
	}

	@NotNull
	public Collection<SingleCompletion> getCompletions() {
		return completions;
	}
}
