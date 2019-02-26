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

package com.evolveum.midpoint.wf.api;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemEventCauseInformationType;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 *  Describes the "complete work item" action.
 */
public class CompleteAction implements Serializable {

	@NotNull private final WorkItemId workItemId;
	@NotNull private final CaseWorkItemType workItem;
	@NotNull private final String outcome;
	private final String comment;
	private final ObjectDelta<? extends ObjectType> additionalDelta;
	private final WorkItemEventCauseInformationType causeInformation;

	public CompleteAction(@NotNull WorkItemId workItemId, @NotNull CaseWorkItemType workItem,
			@NotNull String outcome, String comment, ObjectDelta<? extends ObjectType> additionalDelta,
			WorkItemEventCauseInformationType causeInformation) {
		this.workItemId = workItemId;
		this.workItem = workItem;
		this.outcome = outcome;
		this.comment = comment;
		this.additionalDelta = additionalDelta;
		this.causeInformation = causeInformation;
	}

	@NotNull
	public WorkItemId getWorkItemId() {
		return workItemId;
	}

	@NotNull
	public CaseWorkItemType getWorkItem() {
		return workItem;
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

	public WorkItemEventCauseInformationType getCauseInformation() {
		return causeInformation;
	}

	@Override
	public String toString() {
		return "CompleteAction{" +
				"workItem=" + workItem +
				", outcome='" + outcome + '\'' +
				", comment='" + comment + '\'' +
				", additionalDelta=" + additionalDelta +
				", causeInformation=" + causeInformation +
				'}';
	}

	public static List<CaseWorkItemType> getWorkItems(Collection<CompleteAction> actions) {
		return actions.stream().map(a -> a.getWorkItem()).collect(Collectors.toList());
	}
}
