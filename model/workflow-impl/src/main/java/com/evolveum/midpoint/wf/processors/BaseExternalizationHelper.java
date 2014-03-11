/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.processors;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.wf.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.xml.ns.model.workflow.process_instance_state_2.ProcessInstanceState;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

/**
 * Helps with process state externalization.
 *
 * (Will probably acquire additional duties later.)
 *
 * @author mederly
 */
@Component
public class BaseExternalizationHelper {

    public void externalizeState(PrismObject<? extends ProcessInstanceState> statePrism, Map<String, Object> variables) {
        ProcessInstanceState state = statePrism.asObjectable();
        state.setProcessInstanceName((String) variables.get(CommonProcessVariableNames.VARIABLE_PROCESS_INSTANCE_NAME));
        state.setAnswer((String) variables.get(CommonProcessVariableNames.VARIABLE_WF_ANSWER));
        state.setStartTime(XmlTypeConverter.createXMLGregorianCalendar((Date) variables.get(CommonProcessVariableNames.VARIABLE_START_TIME)));
        state.setShadowTaskOid((String) variables.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_TASK_OID));
        state.setChangeProcessor((String) variables.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_CHANGE_PROCESSOR));
        state.setRequesterOid((String) variables.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_REQUESTER_OID));
        state.setState((String) variables.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_STATE));
    }

}
