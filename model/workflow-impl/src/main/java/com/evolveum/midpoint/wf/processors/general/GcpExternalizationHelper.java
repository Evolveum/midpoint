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

package com.evolveum.midpoint.wf.processors.general;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.activiti.ActivitiEngine;
import com.evolveum.midpoint.wf.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns.model.workflow.common_forms_3.QuestionFormType;
import com.evolveum.midpoint.xml.ns.model.workflow.common_forms_3.RoleApprovalFormType;
import com.evolveum.midpoint.xml.ns.model.workflow.common_forms_3.WorkItemContents;
import com.evolveum.midpoint.xml.ns.model.workflow.process_instance_state_3.ProcessInstanceState;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.TaskFormData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.util.Map;

/**
 * @author mederly
 */
@Component
public class GcpExternalizationHelper {

    private static final Trace LOGGER = TraceManager.getTrace(GcpExternalizationHelper.class);

    @Autowired
    private PrismContext prismContext;

    @Autowired
    private ActivitiEngine activitiEngine;

    public PrismObject<ProcessInstanceState> createNewProcessInstanceState() {
        return (PrismObject) prismContext.getSchemaRegistry().findObjectDefinitionByType(ProcessInstanceState.COMPLEX_TYPE).instantiate();
    }

    public PrismObject<? extends WorkItemContents> createNewWorkItemContents() {
        PrismObjectDefinition<WorkItemContents> wicDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByType(WorkItemContents.COMPLEX_TYPE);
        PrismObject<WorkItemContents> wicPrism = wicDefinition.instantiate();

        PrismObjectDefinition<QuestionFormType> formDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByType(QuestionFormType.COMPLEX_TYPE);
        PrismObject<QuestionFormType> formPrism = formDefinition.instantiate();

        wicPrism.asObjectable().setQuestionForm(formPrism.asObjectable());
        return wicPrism;
    }

    public void fillInQuestionForm(PrismObject<? extends QuestionFormType> formPrism, org.activiti.engine.task.Task task, Map<String, Object> processInstanceVariables, OperationResult result) throws JAXBException, ObjectNotFoundException, SchemaException {
        TaskFormData data = activitiEngine.getFormService().getTaskFormData(task.getId());
        for (FormProperty formProperty : data.getFormProperties()) {
            if (formProperty.isReadable() && !formProperty.getId().startsWith(CommonProcessVariableNames.FORM_BUTTON_PREFIX)) {
                LOGGER.trace("- processing property {} having value {}", formProperty.getId(), formProperty.getValue());
                if (formProperty.getValue() != null) {
                    QName propertyName = new QName(SchemaConstants.NS_WFCF, formProperty.getId());
                    PrismPropertyDefinition<String> prismPropertyDefinition = new PrismPropertyDefinition<>(propertyName, DOMUtil.XSD_STRING, prismContext);
                    PrismProperty<String> prismProperty = prismPropertyDefinition.instantiate();
                    prismProperty.addRealValue(formProperty.getValue());
                    formPrism.add(prismProperty);
                }
            }
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Resulting prism object instance = " + formPrism.debugDump());
        }
    }

}
