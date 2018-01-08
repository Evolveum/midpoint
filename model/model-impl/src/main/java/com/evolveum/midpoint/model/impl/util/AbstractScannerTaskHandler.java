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
package com.evolveum.midpoint.model.impl.util;

import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.prism.PrismPropertyDefinitionImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 *
 * @author Radovan Semancik
 *
 */
@Component
public abstract class AbstractScannerTaskHandler<O extends ObjectType, H extends AbstractScannerResultHandler<O>>
		extends AbstractSearchIterativeModelTaskHandler<O,H> {

    @Autowired(required = true)
    protected Clock clock;

	private static final transient Trace LOGGER = TraceManager.getTrace(AbstractScannerTaskHandler.class);

	public AbstractScannerTaskHandler(Class<O> type, String taskName, String taskOperationPrefix) {
		super(taskName, taskOperationPrefix);
	}

	@Override
	protected boolean initializeRun(H handler, TaskRunResult runResult,
			Task task, OperationResult opResult) {
		boolean cont = super.initializeRun(handler, runResult, task, opResult);
		if (!cont) {
			return cont;
		}

		XMLGregorianCalendar lastScanTimestamp = null;
    	PrismProperty<XMLGregorianCalendar> lastScanTimestampProperty = task.getExtensionProperty(SchemaConstants.MODEL_EXTENSION_LAST_SCAN_TIMESTAMP_PROPERTY_NAME);
        if (lastScanTimestampProperty != null) {
            lastScanTimestamp = lastScanTimestampProperty.getValue().getValue();
        }
        handler.setLastScanTimestamp(lastScanTimestamp);

        handler.setThisScanTimestamp(clock.currentTimeXMLGregorianCalendar());

        return true;
	}

    @Override
	protected void finish(H handler, TaskRunResult runResult, Task task, OperationResult opResult) throws SchemaException {
		super.finish(handler, runResult, task, opResult);

		PrismPropertyDefinition<XMLGregorianCalendar> lastScanTimestampDef = new PrismPropertyDefinitionImpl<>(
				SchemaConstants.MODEL_EXTENSION_LAST_SCAN_TIMESTAMP_PROPERTY_NAME,
				DOMUtil.XSD_DATETIME, prismContext);
		PropertyDelta<XMLGregorianCalendar> lastScanTimestampDelta = new PropertyDelta<XMLGregorianCalendar>(
				new ItemPath(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_LAST_SCAN_TIMESTAMP_PROPERTY_NAME), lastScanTimestampDef, prismContext);
		lastScanTimestampDelta.setValueToReplace(new PrismPropertyValue<XMLGregorianCalendar>(handler.getThisScanTimestamp()));
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
