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

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType;
import org.apache.commons.lang.Validate;

/**
 * @author mederly
 */
public class ProcessInstanceNewDto extends Selectable {

    public static final String F_SHADOW_TASK = "shadowTask";
    public static final String F_SHADOW_TASK_EXISTING = "shadowTaskExisting";


    private WfContextType workflowContext;

    public ProcessInstanceNewDto(TaskType task) {
        Validate.notNull(task);
        Validate.notNull(task.getWorkflowContext());
        this.workflowContext = task.getWorkflowContext();
    }

    public String getStartedTime() {
        return workflowContext.getStartTimestamp() == null ? "-" : WebComponentUtil.formatDate(XmlTypeConverter.toDate(workflowContext.getStartTimestamp()));
    }

    public String getFinishedTime() {
        return workflowContext.getEndTimestamp() == null ? "-" : WebComponentUtil.formatDate(XmlTypeConverter.toDate(workflowContext.getEndTimestamp()));
    }

    public String getName() {
        return workflowContext.getProcessInstanceName();
    }

    public String getInstanceId() {
        return workflowContext.getProcessInstanceId();
    }

//    public List<WorkItemDto> getWorkItems() {
//        List<WorkItemDto> retval = new ArrayList<WorkItemDto>();
//        if (processInstance.getWorkItems() != null) {
//            for (WorkItemType workItem : processInstance.getWorkItems()) {
//                retval.add(new WorkItemDto(workItem));
//            }
//        }
//        return retval;
//    }

    public String getAnswer() {
        return workflowContext.getAnswer();
    }

    public boolean isAnswered() {
        return getAnswer() != null;
    }

    // null if not answered or answer is not true/false
    public Boolean getAnswerAsBoolean() {
        return ApprovalUtils.approvalBooleanValue(getAnswer());
    }

    public boolean isFinished() {
        return workflowContext.getEndTimestamp() != null;
    }

}
