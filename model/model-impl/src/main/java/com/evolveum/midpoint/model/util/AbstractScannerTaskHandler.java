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
package com.evolveum.midpoint.model.util;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.lens.Clockwork;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensUtil;
import com.evolveum.midpoint.model.util.AbstractSearchIterativeResultHandler;
import com.evolveum.midpoint.model.util.AbstractSearchIterativeTaskHandler;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.GreaterFilter;
import com.evolveum.midpoint.prism.query.LessFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * 
 * @author Radovan Semancik
 *
 */
@Component
public abstract class AbstractScannerTaskHandler<O extends ObjectType> extends AbstractSearchIterativeTaskHandler<O> {

	protected XMLGregorianCalendar lastScanTimestamp;
	protected XMLGregorianCalendar thisScanTimestamp;
		
    @Autowired(required = true)
    protected Clock clock;
        	
	private static final transient Trace LOGGER = TraceManager.getTrace(AbstractScannerTaskHandler.class);

	public AbstractScannerTaskHandler(Class<O> type, String taskName, String taskOperationPrefix) {
		super(type, taskName, taskOperationPrefix);
	}

	@Override
	protected boolean initialize(TaskRunResult runResult, Task task, OperationResult opResult) {
		boolean cont = super.initialize(runResult, task, opResult);
		if (!cont) {
			return cont;
		}
		
		lastScanTimestamp = null;
    	PrismProperty<XMLGregorianCalendar> lastScanTimestampProperty = task.getExtension(SchemaConstants.MODEL_EXTENSION_LAST_SCAN_TIMESTAMP_PROPERTY_NAME);
        if (lastScanTimestampProperty != null) {
            lastScanTimestamp = lastScanTimestampProperty.getValue().getValue();
        }
        
        thisScanTimestamp = clock.currentTimeXMLGregorianCalendar();
		        
        return true;
	}
		
    @Override
	protected void finish(TaskRunResult runResult, Task task, OperationResult opResult) throws SchemaException {
		super.finish(runResult, task, opResult);
		
		PrismPropertyDefinition<XMLGregorianCalendar> lastScanTimestampDef = new PrismPropertyDefinition<XMLGregorianCalendar>(
				SchemaConstants.MODEL_EXTENSION_LAST_SCAN_TIMESTAMP_PROPERTY_NAME, SchemaConstants.MODEL_EXTENSION_LAST_SCAN_TIMESTAMP_PROPERTY_NAME,
				DOMUtil.XSD_DATETIME, prismContext);
		PropertyDelta<XMLGregorianCalendar> lastScanTimestampDelta = new PropertyDelta<XMLGregorianCalendar>(
				new ItemPath(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_LAST_SCAN_TIMESTAMP_PROPERTY_NAME), lastScanTimestampDef);
		lastScanTimestampDelta.setValueToReplace(new PrismPropertyValue<XMLGregorianCalendar>(thisScanTimestamp));
		task.modifyExtension(lastScanTimestampDelta);
	}

	@Override
    public String getCategoryName(Task task) {
        return TaskCategory.SYSTEM;
    }

    @Override
    public List<String> getCategoryNames() {
        return null;
    }
}
