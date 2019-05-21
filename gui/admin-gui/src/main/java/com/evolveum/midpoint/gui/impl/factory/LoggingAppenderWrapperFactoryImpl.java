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

import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.wicket.model.IModel;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.configuration.component.ComponentLoggerType;
import com.evolveum.midpoint.gui.impl.page.admin.configuration.component.StandardLoggerType;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerPanel;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyPanel;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.PrismValueWrapper;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.PropertyWrapper;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AppenderConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingComponentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author skublik
 *
 */
@Component
public class LoggingAppenderWrapperFactoryImpl<T> extends PrismPropertyWrapperFactoryImpl<T>{

	@Override
	public boolean match(ItemDefinition<?> def) {
		return def instanceof PrismPropertyDefinition
				&& QNameUtil.match(def.getName(), ClassLoggerConfigurationType.F_APPENDER);
	}
	
	@Override
	public int getOrder() {
		return Integer.MAX_VALUE-1;
	}

	@Override
	protected PrismPropertyWrapper<T> createWrapper(PrismContainerValueWrapper<?> parent, PrismProperty<T> item,
			ItemStatus status) {
		getRegistry().registerWrapperPanel(item.getDefinition().getTypeName(), PrismPropertyPanel.class);
		PrismPropertyWrapper<T> propertyWrapper = new PrismPropertyWrapperImpl<>(parent, item, status);
		propertyWrapper.setPredefinedValues(getPredefinedValues(parent));
		return propertyWrapper;
	}
	
	private LookupTableType getPredefinedValues(PrismContainerValueWrapper<?> parent) {
		LookupTableType lookupTable = new LookupTableType();
        List<LookupTableRowType> list = lookupTable.createRowList();
        
        if(parent == null || parent.getParent() == null || parent.getParent().getParent() == null) {
        	return lookupTable;
        }
        
        
        if(!(parent.getParent().getParent().getRealValue() instanceof LoggingConfigurationType)) {
        	throw new IllegalArgumentException("LoggingConfigurationType not found in parent for Appender");
        }
        
        LoggingConfigurationType loggingConfig = (LoggingConfigurationType) parent.getParent().getParent().getRealValue();
        
        for (AppenderConfigurationType appender : loggingConfig.getAppender()) {
        		LookupTableRowType row = new LookupTableRowType();
        		String name = appender.getName();
        		row.setKey(name);
        		row.setValue(name);
        		row.setLabel(new PolyStringType(name));
        		list.add(row);
        }
        return lookupTable;
	}

}
