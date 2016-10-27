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

package com.evolveum.midpoint.wf.impl.processes.itemApproval;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.wf.impl.tasks.WfTaskCreationInstruction;
import com.evolveum.midpoint.wf.impl.messages.ProcessEvent;
import com.evolveum.midpoint.wf.impl.processes.BaseProcessMidPointInterface;
import com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author mederly
 */
@Component
public class ItemApprovalProcessInterface extends BaseProcessMidPointInterface {

    public static final String PROCESS_DEFINITION_KEY = "ItemApproval";

    @Autowired
    private PrismContext prismContext;

    public void prepareStartInstruction(WfTaskCreationInstruction instruction) {
        instruction.setProcessName(PROCESS_DEFINITION_KEY);
        instruction.setSimple(false);
        instruction.setSendStartConfirmation(true);
        instruction.setProcessInterfaceBean(this);
    }

    @Override public DecisionType extractDecision(Map<String, Object> variables) {
        DecisionType decision = new DecisionType();

        decision.setResultAsString((String) variables.get(CommonProcessVariableNames.FORM_FIELD_DECISION));
        decision.setApproved(ApprovalUtils.approvalBooleanValue(decision.getResultAsString()));
        decision.setComment((String) variables.get(CommonProcessVariableNames.FORM_FIELD_COMMENT));

        // TODO - what with other fields (approver, dateTime)?

        return decision;
    }

    @Override
    public List<ObjectReferenceType> prepareApprovedBy(ProcessEvent event) {
        List<ObjectReferenceType> retval = new ArrayList<ObjectReferenceType>();
        if (!ApprovalUtils.isApproved(getAnswer(event.getVariables()))) {
            return retval;
        }
        List<Decision> allDecisions = event.getVariable(ProcessVariableNames.ALL_DECISIONS, List.class);
        for (Decision decision : allDecisions) {
            if (decision.isApproved()) {
                retval.add(MiscSchemaUtil.createObjectReference(decision.getApproverOid(), SchemaConstants.C_USER_TYPE));
            }
        }
        return retval;
    }


}
