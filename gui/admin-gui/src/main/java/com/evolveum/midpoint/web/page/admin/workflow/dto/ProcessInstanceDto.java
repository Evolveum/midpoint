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

package com.evolveum.midpoint.web.page.admin.workflow.dto;

import com.evolveum.midpoint.web.component.model.delta.DeltaDto;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.wf.api.ProcessInstance;
import com.evolveum.midpoint.wf.api.WorkItem;
import com.evolveum.midpoint.wf.processes.CommonProcessVariableNames;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
public class ProcessInstanceDto extends Selectable {

    public static final String F_WATCHING_TASK_OID = "watchingTaskOid";

    ProcessInstance processInstance;

    public ProcessInstanceDto(ProcessInstance processInstance) {
        this.processInstance = processInstance;
    }

    public String getStartedTime() {
        return processInstance.getStartTime() == null ? "-" : WebMiscUtil.getFormatedDate(processInstance.getStartTime());
    }

    public String getFinishedTime() {
        return processInstance.getEndTime() == null ? "-" : WebMiscUtil.getFormatedDate(processInstance.getEndTime());
    }

    public String getName() {
        return processInstance.getName();
    }

    public String getInstanceId() {
        return processInstance.getProcessId();
    }

    public ProcessInstance getProcessInstance() {
        return processInstance;
    }

    public List<WorkItemDto> getWorkItems() {
        List<WorkItemDto> retval = new ArrayList<WorkItemDto>();
        if (processInstance.getWorkItems() != null) {
            for (WorkItem workItem : processInstance.getWorkItems()) {
                retval.add(new WorkItemDto(workItem));
            }
        }
        return retval;
    }

//    public String getTasks() {
//        if (processInstance.getWorkItems() == null || processInstance.getWorkItems().isEmpty()) {
//            return "-";
//        } else {
//            StringBuffer sb = new StringBuffer();
//            for (WorkItem wi : processInstance.getWorkItems()) {
//                sb.append(wi.getTaskId() + ": " + wi.getName());
//                if (!wi.getAssignee().isEmpty()) {
//                    sb.append(", assigned to ");
//                    sb.append(wi.getAssigneeName());
//                }
//                if (!wi.getCandidates().isEmpty()) {
//                    sb.append("(candidates: ");
//                    sb.append(wi.getCandidates());
//                    sb.append(")");
//                }
//                if (wi.getCreateTime() != null) {
//
//
//                    sb.append(", created on " + WebMiscUtil.getFormatedDate(wi.getCreateTime()));
//                }
//                sb.append("\n");
//            }
//            return sb.toString();
//        }
//    }

    public Object getVariable(String name) {
        return processInstance.getVariables().get(name);
    }

    public Boolean getAnswer() {
        return (Boolean) processInstance.getVariables().get(CommonProcessVariableNames.VARIABLE_WF_ANSWER);
    }

    public boolean isFinished() {
        return processInstance.isFinished();
    }

    public String getWatchingTaskOid() {
        return (String) processInstance.getVariable(CommonProcessVariableNames.VARIABLE_MIDPOINT_TASK_OID);
    }
}
