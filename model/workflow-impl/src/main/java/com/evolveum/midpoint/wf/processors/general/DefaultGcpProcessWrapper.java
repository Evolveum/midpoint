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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns.model.workflow.common_forms_2.WorkItemContents;
import com.evolveum.midpoint.xml.ns.model.workflow.process_instance_state_2.ProcessInstanceState;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBException;
import java.util.Map;

/**
 * Default implementation of GcpProcessWrapper.
 * Delegates everything to GcpExternalizationHelper.
 *
 * @author mederly
 */
@Component
public class DefaultGcpProcessWrapper implements GcpProcessWrapper {

    @Autowired
    private GcpExternalizationHelper gcpExternalizationHelper;

    @Override
    public PrismObject<? extends WorkItemContents> prepareWorkItemContents(Task task, Map<String, Object> processInstanceVariables, OperationResult result) throws JAXBException, ObjectNotFoundException, SchemaException {
        PrismObject<? extends WorkItemContents> prism = gcpExternalizationHelper.createNewWorkItemContents();
        gcpExternalizationHelper.fillInQuestionForm(prism.asObjectable().getQuestionForm().asPrismObject(), task, processInstanceVariables, result);
        return prism;
    }

    @Override
    public PrismObject<? extends ProcessInstanceState> externalizeInstanceState(Map<String, Object> variables) throws SchemaException {
        return gcpExternalizationHelper.createNewProcessInstanceState();
    }
}
