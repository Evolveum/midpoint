/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.wf.processes.common;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.common.security.MidPointPrincipal;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.activiti.SpringApplicationContextHolder;
import com.evolveum.midpoint.wf.jobs.JobController;
import com.evolveum.midpoint.wf.processes.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.processes.WorkflowResult;
import com.evolveum.midpoint.wf.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;

import javax.xml.bind.JAXBException;
import java.util.Map;

/**
 * @author mederly
 */
public class MidPointTaskListener implements TaskListener {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointTaskListener.class);
    private static final String DOT_CLASS = MidPointTaskListener.class.getName() + ".";

    @Override
    public void notify(DelegateTask delegateTask) {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("notify called; event name = {}, name = {}", delegateTask.getEventName(), delegateTask.getName());
        }

        OperationResult result = new OperationResult(DOT_CLASS + "notify");

        // auditing & notifications

        JobController jobController = SpringApplicationContextHolder.getProcessInstanceController();

        if (TaskListener.EVENTNAME_CREATE.equals(delegateTask.getEventName())) {
            jobController.auditWorkItemEvent(delegateTask.getVariables(), delegateTask.getId(), getWorkItemName(delegateTask), delegateTask.getAssignee(), AuditEventStage.REQUEST, result);
            jobController.notifyWorkItemCreated(
                    delegateTask.getName(),
                    delegateTask.getAssignee(),
                    getWorkItemName(delegateTask),
                    delegateTask.getVariables());
        } else if (TaskListener.EVENTNAME_COMPLETE.equals(delegateTask.getEventName())) {
            jobController.auditWorkItemEvent(delegateTask.getVariables(), delegateTask.getId(), getWorkItemName(delegateTask), delegateTask.getAssignee(), AuditEventStage.EXECUTION, result);
            jobController.notifyWorkItemCompleted(
                    delegateTask.getName(),
                    delegateTask.getAssignee(),
                    getWorkItemName(delegateTask),
                    delegateTask.getVariables(),
                    (String) delegateTask.getVariable(CommonProcessVariableNames.FORM_FIELD_DECISION));
        }
    }

    private String getWorkItemName(DelegateTask delegateTask) {
        return (String) delegateTask.getVariable(CommonProcessVariableNames.VARIABLE_PROCESS_INSTANCE_NAME);
    }

}
