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

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerPanel;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.PrismObjectValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.registry.GuiComponentRegistryImpl;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoginEventType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintPresentationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * @author skublik
 *
 */
@Component
public class ClassLoggerWrapperFactoryImpl<C extends Containerable> extends PrismContainerWrapperFactoryImpl<C>{

	private static final transient Trace LOGGER = TraceManager.getTrace(ClassLoggerWrapperFactoryImpl.class);
	
	public static final String LOGGER_PROFILING = "PROFILING";
	
	@Autowired private GuiComponentRegistryImpl registry;
	
	@Override
	public boolean match(ItemDefinition<?> def) {
		return false;
	}

	protected boolean createWrapper(PrismContainerValue<C> value) {
		if(value == null || value.getRealValue() == null) {
			return true;
		}
		String loggerPackage = ((ClassLoggerConfigurationType)value.getRealValue()).getPackage();
		if(loggerPackage == null) {
			return true;
		}
		return !loggerPackage.equals(LOGGER_PROFILING);
	}

	@Override
	protected <ID extends ItemDefinition<PrismContainer<C>>> List<PrismContainerValueWrapper<C>> createValuesWrapper(
			PrismContainerWrapper<C> itemWrapper, PrismContainer<C> item, WrapperContext context)
			throws SchemaException {
		List<PrismContainerValueWrapper<C>> pvWrappers = new ArrayList<>();
		
		ID definition = (ID) item.getDefinition();
		if (item.isEmpty()) {
			if (shoudCreateEmptyValue(item, context)) {
				PrismContainerValue<C> prismValue = createNewValue(item);
				PrismContainerValueWrapper<C> valueWrapper =  createValueWrapper(itemWrapper, prismValue, ValueStatus.ADDED, context);
				pvWrappers.add(valueWrapper);
			}
			return pvWrappers;
		}
		
		for (PrismContainerValue<C> pcv : (List<PrismContainerValue<C>>)item.getValues()) {
			if(createWrapper(pcv)) {
				PrismContainerValueWrapper<C> valueWrapper = createValueWrapper(itemWrapper, pcv, ValueStatus.NOT_CHANGED, context);
				pvWrappers.add(valueWrapper);
			}
		}
		
		return pvWrappers;
	
	}
}
