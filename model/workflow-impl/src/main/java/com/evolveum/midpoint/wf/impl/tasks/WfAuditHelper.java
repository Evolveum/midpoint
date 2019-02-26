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

package com.evolveum.midpoint.wf.impl.tasks;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.wf.impl.processors.ChangeProcessor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 */
@Component
public class WfAuditHelper {

	@Autowired private AuditService auditService;

	public void auditProcessStart(CaseType aCase, WfContextType wfContext,
			ChangeProcessor changeProcessor, Task opTask, OperationResult result) {
		auditProcessStartEnd(aCase, AuditEventStage.REQUEST, wfContext, changeProcessor, opTask, result);
	}

	public void auditProcessEnd(CaseType aCase, WfContextType wfContext, ChangeProcessor changeProcessor, Task opTask,
			OperationResult result) {
		auditProcessStartEnd(aCase, AuditEventStage.EXECUTION, wfContext, changeProcessor, opTask, result);
	}

	private void auditProcessStartEnd(CaseType aCase, AuditEventStage stage, WfContextType wfContext,
			ChangeProcessor changeProcessor, Task opTask, OperationResult result) {
		AuditEventRecord auditEventRecord = changeProcessor.prepareProcessInstanceAuditRecord(aCase, stage, wfContext, result);
		auditService.audit(auditEventRecord, opTask);
	}

}
