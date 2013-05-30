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

package com.evolveum.midpoint.wf.processes.general;

import com.evolveum.midpoint.wf.activiti.SpringApplicationContextHolder;
import com.evolveum.midpoint.wf.processes.CommonProcessVariableNames;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;

/**
 * @author mederly
 */
public class MidPointTaskListener implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
        if (TaskListener.EVENTNAME_CREATE.equals(delegateTask.getEventName())) {
            SpringApplicationContextHolder.getProcessInstanceController().notifyWorkItemCreated(
                    delegateTask.getName(),
                    delegateTask.getAssignee(),
                    (String) delegateTask.getVariable(CommonProcessVariableNames.VARIABLE_PROCESS_INSTANCE_NAME),
                    delegateTask.getVariables());
        } else if (TaskListener.EVENTNAME_COMPLETE.equals(delegateTask.getEventName())) {
            SpringApplicationContextHolder.getProcessInstanceController().notifyWorkItemCompleted(
                    delegateTask.getName(),
                    delegateTask.getAssignee(),
                    (String) delegateTask.getVariable(CommonProcessVariableNames.VARIABLE_PROCESS_INSTANCE_NAME),
                    delegateTask.getVariables(),
                    (Boolean) delegateTask.getVariable(CommonProcessVariableNames.FORM_FIELD_DECISION));
        }
    }
}
