/*
 * Copyright (c) 2010-2017 Evolveum
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

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemEventCauseInformationType;

/**
 * What caused the operation.
 * Primarily used to simplify passing parameters to WorkflowListener.
 *
 * @author mederly
 */
public class WorkItemOperationSourceInfo {

	private final ObjectReferenceType initiatorRef;
	private final WorkItemEventCauseInformationType cause;
	private final AbstractWorkItemActionType source;

	public WorkItemOperationSourceInfo(ObjectReferenceType initiatorRef,
			WorkItemEventCauseInformationType cause,
			AbstractWorkItemActionType source) {
		this.initiatorRef = initiatorRef;
		this.cause = cause;
		this.source = source;
	}

	public ObjectReferenceType getInitiatorRef() {
		return initiatorRef;
	}

	public WorkItemEventCauseInformationType getCause() {
		return cause;
	}

	public AbstractWorkItemActionType getSource() {
		return source;
	}
}
