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

package com.evolveum.midpoint.wf.processors.primary;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.wf.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns.model.workflow.process_instance_state_2.PrimaryApprovalProcessInstanceState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBException;
import java.util.Map;

/**
 * @author Pavol
 */
@Component
public class PcpExternalizationHelper {

    @Autowired
    private MiscDataUtil miscDataUtil;

    public void externalizeState(PrismObject<? extends PrimaryApprovalProcessInstanceState> statePrism, Map<String, Object> variables) throws JAXBException, SchemaException {
        PrimaryApprovalProcessInstanceState state = statePrism.asObjectable();
        state.setMidPointProcessWrapper((String) variables.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_PROCESS_WRAPPER));
        state.setMidPointObjectOid((String) variables.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_OBJECT_OID));
        state.setMidPointObjectToBeAdded((ObjectType) variables.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_OBJECT_TO_BE_ADDED));
        state.setMidPointDelta(miscDataUtil.getObjectDeltaType(variables, true));
    }
}
