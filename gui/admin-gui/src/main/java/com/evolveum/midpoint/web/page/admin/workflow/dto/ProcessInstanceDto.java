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
import com.evolveum.midpoint.xml.ns._public.common.common_2a.WfProcessInstanceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.WfProcessInstanceVariableType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.WorkItemType;

import java.io.IOException;
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

    private WfProcessInstanceVariableType getVariableRawValue(String name) {
        for (WfProcessInstanceVariableType var : processInstance.getVariables()) {
            if (name.equals(var.getName())) {
                return var;
            }
        }
        return null;
    }

    public Object getVariable(String name) {
        WfProcessInstanceVariableType var = getVariableRawValue(name);
        if (var != null) {
            if (var.isEncoded()) {
                try {
                    return SerializationUtil.fromString(var.getValue());
                } catch (IOException e) {
                    throw new SystemException("Couldn't decode value of variable " + name, e);
                } catch (ClassNotFoundException e) {
                    throw new SystemException("Couldn't decode value of variable " + name, e);
                }
            } else {
                return var.getValue();
            }
        } else {
            return null;
        }
    }

    public String getAnswer() {
        return (String) getVariable(CommonProcessVariableNames.VARIABLE_WF_ANSWER);
    }

    public boolean isAnswered() {
        return getAnswer() != null;
    }

    // null if not answered or answer is not true/false
    public Boolean getAnswerAsBoolean() {
        return CommonProcessVariableNames.approvalBooleanValue(getAnswer());
    }

    public boolean isFinished() {
        return processInstance.isFinished();
    }

    public String getWatchingTaskOid() {
        return (String) getVariable(CommonProcessVariableNames.VARIABLE_MIDPOINT_TASK_OID);
    }
}
