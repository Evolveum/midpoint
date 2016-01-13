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

package com.evolveum.midpoint.wf.impl.processors.primary;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns.model.workflow.common_forms_3.GeneralChangeApprovalWorkItemContents;
import com.evolveum.midpoint.xml.ns.model.workflow.common_forms_3.QuestionFormType;
import com.evolveum.midpoint.xml.ns.model.workflow.common_forms_3.WorkItemContents;
import com.evolveum.midpoint.xml.ns.model.workflow.process_instance_state_3.PrimaryChangeProcessorState;

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
    private PcpRepoAccessHelper pcpRepoAccessHelper;

    @Autowired
    private PrismContext prismContext;

    @Autowired
    private PrimaryChangeProcessor primaryChangeProcessor;

    @Autowired
    private MiscDataUtil miscDataUtil;

    public PrimaryChangeProcessorState externalizeState(Map<String, Object> variables) throws JAXBException, SchemaException {
        PrismContainerDefinition<PrimaryChangeProcessorState> extDefinition = prismContext.getSchemaRegistry().findContainerDefinitionByType(PrimaryChangeProcessorState.COMPLEX_TYPE);
        PrismContainer<PrimaryChangeProcessorState> extStateContainer = extDefinition.instantiate();
        PrimaryChangeProcessorState state = extStateContainer.createNewValue().asContainerable();

        state.setChangeAspect((String) variables.get(PcpProcessVariableNames.VARIABLE_MIDPOINT_CHANGE_ASPECT));

        String objectXml = (String) variables.get(PcpProcessVariableNames.VARIABLE_MIDPOINT_OBJECT_TO_BE_ADDED);
        if (objectXml != null) {
            ObjectType objectToBeAdded = (ObjectType) prismContext.parseObject(objectXml, PrismContext.LANG_XML).asObjectable();
            state.setObjectToBeAdded(objectToBeAdded);
        }
        //state.setFocusDelta(miscDataUtil.getFocusPrimaryObjectDeltaType(variables, true));
        state.asPrismContainerValue().setConcreteType(PrimaryChangeProcessorState.COMPLEX_TYPE);
        return state;
    }

    public PrismObject<? extends WorkItemContents> externalizeWorkItemContents(org.activiti.engine.task.Task task, Map<String, Object> processInstanceVariables, OperationResult result) throws JAXBException, ObjectNotFoundException, SchemaException {

        PrismObject<? extends GeneralChangeApprovalWorkItemContents> wicPrism = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(GeneralChangeApprovalWorkItemContents.class).instantiate();
        GeneralChangeApprovalWorkItemContents wic = wicPrism.asObjectable();

        PrismObject<? extends ObjectType> objectBefore = pcpRepoAccessHelper.getObjectBefore(processInstanceVariables, prismContext, result);
        if (objectBefore != null) {
            wic.setObjectOld(objectBefore.asObjectable());
            if (objectBefore.getOid() != null) {
                wic.setObjectOldRef(MiscSchemaUtil.createObjectReference(objectBefore.getOid(), SchemaConstants.C_OBJECT_TYPE));     // todo ...or will we determine real object type?
            }
        }

        wic.setObjectDelta(miscDataUtil.getFocusPrimaryObjectDeltaType(processInstanceVariables, true));

        PrismObject<? extends ObjectType> objectAfter = pcpRepoAccessHelper.getObjectAfter(processInstanceVariables, wic.getObjectDelta(), objectBefore, prismContext, result);
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
        return primaryChangeProcessor.getChangeAspect(variables).prepareQuestionForm(task, variables, result);
    }

    private PrismObject<? extends ObjectType> getRelatedObject(org.activiti.engine.task.Task task, Map<String, Object> variables, OperationResult result) throws SchemaException, ObjectNotFoundException {
        return primaryChangeProcessor.getChangeAspect(variables).prepareRelatedObject(task, variables, result);
    }

    private <T> T asObjectable(PrismObject<? extends T> prismObject) {
        return prismObject != null ? prismObject.asObjectable() : null;
    }

}
