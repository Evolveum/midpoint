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

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.SerializationUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.wf.processes.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.util.WfVariablesUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.WfProcessInstanceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.WfProcessInstanceVariableType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.WorkItemType;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
public class ProcessInstanceDto extends Selectable {

    public static final String F_WATCHING_TASK_OID = "watchingTaskOid";

    WfProcessInstanceType processInstance;

    public ProcessInstanceDto(WfProcessInstanceType processInstance) {
        this.processInstance = processInstance;
    }

    public String getStartedTime() {
        return processInstance.getStartTimestamp() == null ? "-" : WebMiscUtil.formatDate(XmlTypeConverter.toDate(processInstance.getStartTimestamp()));
    }

    public String getFinishedTime() {
        return processInstance.getEndTimestamp() == null ? "-" : WebMiscUtil.formatDate(XmlTypeConverter.toDate(processInstance.getEndTimestamp()));
    }

    public String getName() {
        return PolyString.getOrig(processInstance.getName());
    }

    public String getInstanceId() {
        return processInstance.getProcessInstanceId();
    }

    public WfProcessInstanceType getProcessInstance() {
        return processInstance;
    }

    public List<WorkItemDto> getWorkItems() {
        List<WorkItemDto> retval = new ArrayList<WorkItemDto>();
        if (processInstance.getWorkItems() != null) {
            for (WorkItemType workItem : processInstance.getWorkItems()) {
                retval.add(new WorkItemDto(workItem));
            }
        }
        return retval;
    }

    public String getAnswer() {
        return WfVariablesUtil.getAnswer(processInstance);
    }

    public boolean isAnswered() {
        return WfVariablesUtil.isAnswered(processInstance);
    }

    // null if not answered or answer is not true/false
    public Boolean getAnswerAsBoolean() {
        return WfVariablesUtil.getAnswerAsBoolean(processInstance);
    }

    public boolean isFinished() {
        return processInstance.isFinished();
    }

    public String getWatchingTaskOid() {
        return WfVariablesUtil.getWatchingTaskOid(processInstance);
    }

    // fixme (parametrize)
    public Object getVariable(String name) {
        return WfVariablesUtil.getVariable(processInstance, name, Serializable.class);
    }
}
