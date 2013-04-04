/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.workflow.dto;

import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.wf.api.ProcessInstance;
import com.evolveum.midpoint.wf.api.WorkItem;

/**
 * @author mederly
 */
public class ProcessInstanceDto extends Selectable {

    ProcessInstance processInstance;

    public ProcessInstanceDto(ProcessInstance processInstance) {
        this.processInstance = processInstance;
    }

    public String getStarted() {
        return processInstance.getStartTime() == null ? "-" : WebMiscUtil.getFormatedDate(processInstance.getStartTime());
    }

    public String getFinished() {
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

    public String getTasks() {
        if (processInstance.getWorkItems() == null || processInstance.getWorkItems().isEmpty()) {
            return "-";
        } else {
            StringBuffer sb = new StringBuffer();
            for (WorkItem wi : processInstance.getWorkItems()) {
                sb.append(wi.getTaskId() + ": " + wi.getName());
                if (!wi.getAssignee().isEmpty()) {
                    sb.append(", assigned to ");
                    sb.append(wi.getAssigneeName());
                }
                if (!wi.getCandidates().isEmpty()) {
                    sb.append("(candidates: ");
                    sb.append(wi.getCandidates());
                    sb.append(")");
                }
                if (wi.getCreateTime() != null) {
                	
                	
                    sb.append(", created on " + WebMiscUtil.getFormatedDate(wi.getCreateTime()));
                }
                sb.append("\n");
            }
            return sb.toString();
        }
    }

    public String getDetails() {
        return processInstance.getDetails();
    }
}
