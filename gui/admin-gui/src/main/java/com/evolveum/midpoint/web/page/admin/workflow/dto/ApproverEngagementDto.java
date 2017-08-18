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

package com.evolveum.midpoint.web.page.admin.workflow.dto;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serializable;

/**
 * GUI-friendly information about an engagement of given approver in a historic, current or future execution of an approval stage.
 *
 * @author mederly
 */
public class ApproverEngagementDto implements Serializable {

	private static final long serialVersionUID = 1L;

	@NotNull private final ObjectReferenceType approverRef;                // with the whole object, if possible
	@Nullable private final String externalWorkItemId;
	@Nullable private AbstractWorkItemOutputType output;
	@Nullable private XMLGregorianCalendar completedAt;
	@Nullable private ObjectReferenceType completedBy;                // the user that really completed the work item originally assigned to that approver
	private boolean last;

	ApproverEngagementDto(@NotNull ObjectReferenceType approverRef, @Nullable String externalWorkItemId) {
		this.approverRef = approverRef;
		this.externalWorkItemId = externalWorkItemId;
	}

	@NotNull
	public ObjectReferenceType getApproverRef() {
		return approverRef;
	}

	@Nullable
	public String getExternalWorkItemId() {
		return externalWorkItemId;
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

	public boolean isLast() {
		return last;
	}

	public void setLast(boolean last) {
		this.last = last;
	}
}
