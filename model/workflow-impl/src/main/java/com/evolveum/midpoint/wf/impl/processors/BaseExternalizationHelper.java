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

package com.evolveum.midpoint.wf.impl.processors;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.wf.impl.processes.ProcessInterfaceFinder;
import com.evolveum.midpoint.wf.impl.processes.ProcessMidPointInterface;
import com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns.model.workflow.process_instance_state_3.ProcessInstanceState;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private PrismContext prismContext;

    @Autowired
    private ProcessInterfaceFinder processInterfaceFinder;

    public PrismObject<ProcessInstanceState> externalizeState(Map<String, Object> variables) {
        PrismObjectDefinition<ProcessInstanceState> extDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByType(ProcessInstanceState.COMPLEX_TYPE);
        PrismObject<ProcessInstanceState> extStateObject = extDefinition.instantiate();
        ProcessInstanceState extState = extStateObject.asObjectable();

        extState.setProcessInstanceName((String) variables.get(CommonProcessVariableNames.VARIABLE_PROCESS_INSTANCE_NAME));
        extState.setStartTime(XmlTypeConverter.createXMLGregorianCalendar((Date) variables.get(CommonProcessVariableNames.VARIABLE_START_TIME)));
        extState.setShadowTaskOid((String) variables.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_TASK_OID));
        extState.setChangeProcessor((String) variables.get(CommonProcessVariableNames.VARIABLE_CHANGE_PROCESSOR));

        ProcessMidPointInterface processMidPointInterface = processInterfaceFinder.getProcessInterface(variables);
        extState.setAnswer(processMidPointInterface.getAnswer(variables));
        extState.setState(processMidPointInterface.getState(variables));

        return extStateObject;
    }
}
