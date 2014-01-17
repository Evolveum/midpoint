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

import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.activiti.SpringApplicationContextHolder;
import com.evolveum.midpoint.wf.jobs.JobController;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;

import javax.xml.bind.JAXBException;

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
            try {
                jobController.notifyWorkItemCreated(
                        delegateTask.getName(),
                        delegateTask.getAssignee(),
                        getWorkItemName(delegateTask),
                        delegateTask.getVariables());
            } catch (JAXBException|SchemaException e) {
                LoggingUtils.logException(LOGGER, "Couldn't audit work item create event", e);
            }
        } else if (TaskListener.EVENTNAME_COMPLETE.equals(delegateTask.getEventName())) {
            jobController.auditWorkItemEvent(delegateTask.getVariables(), delegateTask.getId(), getWorkItemName(delegateTask), delegateTask.getAssignee(), AuditEventStage.EXECUTION, result);
            try {
                jobController.notifyWorkItemCompleted(
                        delegateTask.getName(),
                        delegateTask.getAssignee(),
                        getWorkItemName(delegateTask),
                        delegateTask.getVariables(),
                        (String) delegateTask.getVariable(CommonProcessVariableNames.FORM_FIELD_DECISION));
            } catch (JAXBException|SchemaException e) {
                LoggingUtils.logException(LOGGER, "Couldn't audit work item complete event", e);
            }
        }
    }

    private String getWorkItemName(DelegateTask delegateTask) {
        return (String) delegateTask.getVariable(CommonProcessVariableNames.VARIABLE_PROCESS_INSTANCE_NAME);
    }

}
