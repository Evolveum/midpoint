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

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.wf.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns.model.workflow.common_forms_2.GeneralChangeApprovalWorkItemContents;
import com.evolveum.midpoint.xml.ns.model.workflow.common_forms_2.QuestionFormType;
import com.evolveum.midpoint.xml.ns.model.workflow.common_forms_2.WorkItemContents;
import com.evolveum.midpoint.xml.ns.model.workflow.process_instance_state_2.PrimaryApprovalProcessInstanceState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBException;
import java.util.Map;

/**
 * @author mederly
 */
@Component
public class PcpExternalizationHelper {

    @Autowired
    private MiscDataUtil miscDataUtil;

    @Autowired
    private PrismContext prismContext;

    @Autowired
    private PrimaryChangeProcessor primaryChangeProcessor;

    public void externalizeState(PrismObject<? extends PrimaryApprovalProcessInstanceState> statePrism, Map<String, Object> variables) throws JAXBException, SchemaException {
        PrimaryApprovalProcessInstanceState state = statePrism.asObjectable();
        state.setMidPointProcessWrapper((String) variables.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_PROCESS_WRAPPER));
        state.setMidPointObjectOid((String) variables.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_OBJECT_OID));

        String objectXml = (String) variables.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_OBJECT_TO_BE_ADDED);
        if (objectXml != null) {
            ObjectType objectToBeAdded = (ObjectType) prismContext.parseObject(objectXml, PrismContext.LANG_XML).asObjectable();
            state.setMidPointObjectToBeAdded(objectToBeAdded);
        }
        state.setMidPointDelta(miscDataUtil.getObjectDeltaType(variables, true));
    }

    public PrismObject<? extends WorkItemContents> prepareWorkItemContents(org.activiti.engine.task.Task task, Map<String, Object> processInstanceVariables, OperationResult result) throws JAXBException, ObjectNotFoundException, SchemaException {

        PrismObject<? extends GeneralChangeApprovalWorkItemContents> wicPrism = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(GeneralChangeApprovalWorkItemContents.class).instantiate();
        GeneralChangeApprovalWorkItemContents wic = wicPrism.asObjectable();

        PrismObject<? extends ObjectType> objectBefore = miscDataUtil.getObjectBefore(processInstanceVariables, prismContext, result);
        if (objectBefore != null) {
            wic.setObjectOld(objectBefore.asObjectable());
            if (objectBefore.getOid() != null) {
                wic.setObjectOldRef(MiscSchemaUtil.createObjectReference(objectBefore.getOid(), SchemaConstants.C_OBJECT_TYPE));     // todo ...or will we determine real object type?
            }
        }

        wic.setObjectDelta(miscDataUtil.getObjectDeltaType(processInstanceVariables, true));

        PrismObject<? extends ObjectType> objectAfter = miscDataUtil.getObjectAfter(processInstanceVariables, wic.getObjectDelta(), objectBefore, prismContext, result);
        if (objectAfter != null) {
            wic.setObjectNew(objectAfter.asObjectable());
            if (objectAfter.getOid() != null) {
                wic.setObjectNewRef(MiscSchemaUtil.createObjectReference(objectAfter.getOid(), SchemaConstants.C_OBJECT_TYPE));     // todo ...or will we determine real object type?
            }
        }

        PrismObject<? extends ObjectType> relatedObject = getRelatedObject(task, processInstanceVariables, result);
        if (relatedObject != null) {
            wic.setRelatedObject(relatedObject.asObjectable());
            if (relatedObject.getOid() != null) {
                wic.setRelatedObjectRef(MiscSchemaUtil.createObjectReference(relatedObject.getOid(), SchemaConstants.C_OBJECT_TYPE));     // todo ...or will we determine real object type?
            }
        }

        wic.setQuestionForm(asObjectable(getQuestionForm(task, processInstanceVariables, result)));
        return wicPrism;
    }

    private PrismObject<? extends QuestionFormType> getQuestionForm(org.activiti.engine.task.Task task, Map<String, Object> variables, OperationResult result) throws SchemaException, ObjectNotFoundException {
        return primaryChangeProcessor.getProcessWrapper(variables).getRequestSpecificData(task, variables, result);
    }

    private PrismObject<? extends ObjectType> getRelatedObject(org.activiti.engine.task.Task task, Map<String, Object> variables, OperationResult result) throws SchemaException, ObjectNotFoundException {
        return primaryChangeProcessor.getProcessWrapper(variables).getRelatedObject(task, variables, result);
    }

    private <T> T asObjectable(PrismObject<? extends T> prismObject) {
        return prismObject != null ? prismObject.asObjectable() : null;
    }

}
