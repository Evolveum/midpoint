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

import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemEventCauseInformationType;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 *
 */
public abstract class Request implements Serializable {

	@NotNull private final String caseOid;
	private final WorkItemEventCauseInformationType causeInformation;

	public Request(@NotNull String caseOid,
			WorkItemEventCauseInformationType causeInformation) {
		this.caseOid = caseOid;
		this.causeInformation = causeInformation;
	}

	@NotNull
	public String getCaseOid() {
		return caseOid;
	}

	public WorkItemEventCauseInformationType getCauseInformation() {
		return causeInformation;
	}
}
