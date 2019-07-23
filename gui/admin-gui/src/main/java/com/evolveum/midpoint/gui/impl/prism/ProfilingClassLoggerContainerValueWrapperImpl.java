/*
 * Copyright (c) 2010-2019 Evolveum
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
package com.evolveum.midpoint.gui.impl.prism;

import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;

/**
 * @author skublik
 *
 */
public class ProfilingClassLoggerContainerValueWrapperImpl extends PrismContainerValueWrapperImpl<ClassLoggerConfigurationType> {

	private static final long serialVersionUID = 1L;

	private static final transient Trace LOGGER = TraceManager.getTrace(PrismReferenceValueWrapperImpl.class);
	
	public ProfilingClassLoggerContainerValueWrapperImpl(PrismContainerWrapper<ClassLoggerConfigurationType> parent, PrismContainerValue<ClassLoggerConfigurationType> pcv, ValueStatus status) {
		super(parent, pcv, status);
	}
	
	@Override
	public String getDisplayName() {
		return ColumnUtils.createStringResource("LoggingConfigPanel.profiling.entryExit").getString();
	}
	
	@Override
	public PrismContainerValue<ClassLoggerConfigurationType> getValueToAdd() throws SchemaException {
		PrismProperty<Object> level = getNewValue().findProperty(ClassLoggerConfigurationType.F_LEVEL);
		if(level != null && !level.isEmpty() && level.getRealValue() != null) {
			return super.getValueToAdd();
		}
		return null;
	}
}
