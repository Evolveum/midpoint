/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.api.request;

import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemEventCauseInformationType;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 *
 */
public abstract class Request implements Serializable {

	@NotNull protected final String caseOid;
	protected final WorkItemEventCauseInformationType causeInformation;

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
