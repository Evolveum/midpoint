/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl;

import com.evolveum.midpoint.schema.util.CaseWorkItemUtil;
import com.evolveum.midpoint.schema.util.ApprovalContextUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

/**
 * @author mederly
 */
public class ExpectedWorkItem {
	final String assigneeOid;
	final String targetOid;
	final ExpectedTask task;

	public ExpectedWorkItem(String assigneeOid, String targetOid, ExpectedTask task) {
		this.assigneeOid = assigneeOid;
		this.targetOid = targetOid;
		this.task = task;
	}

	public boolean matches(CaseWorkItemType actualWorkItem) {
		if (!assigneeOid.equals(actualWorkItem.getOriginalAssigneeRef().getOid())) {
			return false;
		}
		if (targetOid != null && !targetOid.equals(ApprovalContextUtil.getTargetRef(actualWorkItem).getOid())) {
			return false;
		}
		CaseType actualCase = CaseWorkItemUtil.getCaseRequired(actualWorkItem);
		return task.processName.equals(actualCase.getName().getOrig());
	}

	@Override
	public String toString() {
		return "ExpectedWorkItem{" +
				"assigneeOid='" + assigneeOid + '\'' +
				", targetOid='" + targetOid + '\'' +
				", task=" + task +
				'}';
	}
}
