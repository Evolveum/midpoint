/*
\ * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.gui.impl.factory;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerPanel;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.ProfilingClassLoggerContainerValueWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.ProfilingClassLoggerContainerWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.ProfilingClassLoggerPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;

/**
 * @author skublik
 *
 */
@Component
public class ProfilingClassLoggerWrapperFactoryImpl extends PrismContainerWrapperFactoryImpl<ClassLoggerConfigurationType>{

	private static final long serialVersionUID = 1L;
	
	private static final transient Trace LOGGER = TraceManager.getTrace(ProfilingClassLoggerWrapperFactoryImpl.class);
	
	@Autowired private GuiComponentRegistry registry;
	
	public static final QName PROFILING_LOGGER_PATH = new QName("profilingClassLogger");
	
	public static final String LOGGER_PROFILING = "PROFILING";
	
	@Override
	public boolean match(ItemDefinition<?> def) {
		return false;
	}
	
	@Override
	protected boolean canCreateValueWrapper(PrismContainerValue<ClassLoggerConfigurationType> value) {
		if(value == null || value.getRealValue() == null) {
			return false;
		}
		String loggerPackage = ((ClassLoggerConfigurationType)value.getRealValue()).getPackage();
		if(loggerPackage == null) {
			return false;
		}
		return loggerPackage.equals(LOGGER_PROFILING);
	}

	@Override
	protected PrismContainerWrapper<ClassLoggerConfigurationType> createWrapper(PrismContainerValueWrapper<?> parent,
			PrismContainer<ClassLoggerConfigurationType> childContainer, ItemStatus status) {
		PrismContainer<ClassLoggerConfigurationType> clone = childContainer.clone();
//		clone.setElementName(PROFILING_LOGGER_PATH);
		registry.registerWrapperPanel(PROFILING_LOGGER_PATH, ProfilingClassLoggerPanel.class);
		return new ProfilingClassLoggerContainerWrapperImpl<ClassLoggerConfigurationType>((PrismContainerValueWrapper<ClassLoggerConfigurationType>) parent, clone, status);
	}
	
	@Override
	protected <ID extends ItemDefinition<PrismContainer<ClassLoggerConfigurationType>>> List<PrismContainerValueWrapper<ClassLoggerConfigurationType>> createValuesWrapper(
			PrismContainerWrapper<ClassLoggerConfigurationType> itemWrapper, PrismContainer<ClassLoggerConfigurationType> item, WrapperContext context)
			throws SchemaException {
		List<PrismContainerValueWrapper<ClassLoggerConfigurationType>> pvWrappers = new ArrayList<>();
		
		for (PrismContainerValue<ClassLoggerConfigurationType> pcv : (List<PrismContainerValue<ClassLoggerConfigurationType>>)item.getValues()) {
			if(canCreateValueWrapper(pcv)) {
				PrismContainerValueWrapper<ClassLoggerConfigurationType> valueWrapper = createValueWrapper(itemWrapper, pcv, ValueStatus.NOT_CHANGED, context);
				pvWrappers.add(valueWrapper);
			}
		}
		
		if (pvWrappers.isEmpty()) {
			PrismContainerValue<ClassLoggerConfigurationType> prismValue = createNewValue(item);
			PrismContainerValueWrapper<ClassLoggerConfigurationType> valueWrapper =  createValueWrapper(itemWrapper, prismValue, ValueStatus.ADDED, context);
			((ClassLoggerConfigurationType)valueWrapper.getRealValue()).setPackage(LOGGER_PROFILING);
			pvWrappers.add(valueWrapper);
		}
		
		return pvWrappers;
	}
	
	@Override
	public PrismContainerValueWrapper<ClassLoggerConfigurationType> createContainerValueWrapper(PrismContainerWrapper<ClassLoggerConfigurationType> objectWrapper,
			PrismContainerValue<ClassLoggerConfigurationType> objectValue, ValueStatus status) {
		
		ClassLoggerConfigurationType logger = (ClassLoggerConfigurationType) objectValue.getRealValue();
		logger.setPackage(LOGGER_PROFILING);
		
		return new ProfilingClassLoggerContainerValueWrapperImpl(objectWrapper, objectValue, status);
	}
	
}
