/*
 * Copyright (c) 2010-2018 Evolveum
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
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerPanel;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.registry.GuiComponentRegistryImpl;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;

/**
 * @author katka
 *
 */
@Component
public class ProfilingClassLoggerWrapperFactoryImpl<C extends Containerable> extends ClassLoggerWrapperFactoryImpl<C>{

	private static final transient Trace LOGGER = TraceManager.getTrace(ProfilingClassLoggerWrapperFactoryImpl.class);
	
	@Autowired private GuiComponentRegistryImpl registry;
	
	private static final QName PATH_NAME = new QName("profilingClassLogger");
	
	@Override
	protected boolean createWrapper(PrismContainerValue<C> value) {
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
	protected PrismContainerWrapper<C> createWrapper(PrismContainerValueWrapper<?> parent, PrismContainer<C> childContainer,
			ItemStatus status) {
		PrismContainer<C> clone = childContainer.clone();
		clone.setElementName(PATH_NAME);
		registry.registerWrapperPanel(childContainer.getDefinition().getTypeName(), PrismContainerPanel.class);
		return new PrismContainerWrapperImpl<C>((PrismContainerValueWrapper<C>) parent, clone, status) {
			@Override
			public ItemName getName() {
				return PATH_NAME;
			}
		};
	}
	
	@Override
	protected <ID extends ItemDefinition<PrismContainer<C>>> List<PrismContainerValueWrapper<C>> createValuesWrapper(
			PrismContainerWrapper<C> itemWrapper, PrismContainer<C> item, WrapperContext context)
			throws SchemaException {
		List<PrismContainerValueWrapper<C>> pvWrappers = new ArrayList<>();
		
		for (PrismContainerValue<C> pcv : (List<PrismContainerValue<C>>)item.getValues()) {
			if(createWrapper(pcv)) {
				PrismContainerValueWrapper<C> valueWrapper = createValueWrapper(itemWrapper, pcv, ValueStatus.NOT_CHANGED, context);
				pvWrappers.add(valueWrapper);
			}
		}
		
		if (pvWrappers.isEmpty()) {
			PrismContainerValue<C> prismValue = createNewValue(item);
			PrismContainerValueWrapper<C> valueWrapper =  createValueWrapper(itemWrapper, prismValue, ValueStatus.ADDED, context);
			((ClassLoggerConfigurationType)valueWrapper.getRealValue()).setPackage(LOGGER_PROFILING);
			pvWrappers.add(valueWrapper);
		}
		
		return pvWrappers;
	}
	
}
