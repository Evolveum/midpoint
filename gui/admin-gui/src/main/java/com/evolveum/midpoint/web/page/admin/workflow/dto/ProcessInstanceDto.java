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
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.apache.commons.lang.Validate;
import org.apache.wicket.Component;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Date;

/**
 * @author mederly
 */
public class ProcessInstanceDto extends Selectable {

    public static final String F_SHADOW_TASK = "shadowTask";
    public static final String F_NAME = "name";
    public static final String F_START_FORMATTED = "startFormatted";

    private TaskType task;

    public ProcessInstanceDto(TaskType task) {
        Validate.notNull(task, "Task is null");
        Validate.notNull(task.getWorkflowContext(), "Task has no workflow context");
        this.task = task;
    }

    public XMLGregorianCalendar getStartTimestamp() {
        return task.getWorkflowContext().getStartTimestamp();
    }

    public String getStartFormatted() {
        Date started = XmlTypeConverter.toDate(getStartTimestamp());
        return WebComponentUtil.formatDate(started);
    }

    public String getName() {
        return PolyString.getOrig(task.getName());
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

//    public String getAnswer() {
//        if (processInstanceState == null) {
//            return null;
//        }
//        return processInstanceState.getAnswer();
//    }

//    public boolean isAnswered() {
//        return getAnswer() != null;
//    }

    // null if not answered or answer is not true/false
//    public Boolean getAnswerAsBoolean() {
//        return ApprovalUtils.approvalBooleanValue(getAnswer());
//    }

//    public boolean isFinished() {
//        return processInstance.isFinished();
//    }

//    public ProcessInstanceState getInstanceState() {
//        return (ProcessInstanceState) processInstance.getState();
//    }

//    public String getShadowTaskOid() {
//        return processInstanceState.getShadowTaskOid();
//    }

    public void reviveIfNeeded(Component component) {
//        WebComponentUtil.reviveIfNeeded(processInstance, component);
//        WebComponentUtil.reviveIfNeeded(processInstanceState, component);
    }

    public String getTaskOid() {
        return task.getOid();
    }
}
