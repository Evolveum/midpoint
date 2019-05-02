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
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.collections.EnumerationUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.wicket.model.IModel;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingComponentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author katka
 *
 */
@Component
public class LoggingPackageWrapperFactoryImpl<T> extends PrismPropertyWrapperFactoryImpl<T>{

	@Override
	public boolean match(ItemDefinition<?> def) {
		return def instanceof PrismPropertyDefinition
				&& QNameUtil.match(def.getName(), ClassLoggerConfigurationType.F_PACKAGE);
	}

	@Override
	protected PrismPropertyWrapper<T> createWrapper(PrismContainerValueWrapper<?> parent, PrismProperty<T> item,
			ItemStatus status) {
		getRegistry().registerWrapperPanel(item.getDefinition().getTypeName(), PrismPropertyPanel.class);
		PrismPropertyWrapper<T> propertyWrapper = new PrismPropertyWrapperImpl<>(parent, item, status);
		propertyWrapper.setPredefinedValues(getPredefinedValues());
		return propertyWrapper;
	}
	
	private LookupTableType getPredefinedValues() {
		LookupTableType lookupTable = new LookupTableType();
        List<LookupTableRowType> list = lookupTable.createRowList();
        List<StandardLoggerType> standardLoggers = EnumUtils.getEnumList(StandardLoggerType.class);
    	List<LoggingComponentType> componentLoggers = EnumUtils.getEnumList(LoggingComponentType.class);
    	
    	for(StandardLoggerType standardLogger : standardLoggers) {
    		LookupTableRowType row = new LookupTableRowType();
    		row.setKey(standardLogger.getValue());
    		row.setValue(standardLogger.getValue());
    		PolyStringType label = new PolyStringType("StandardLoggerType." + standardLogger.name());
    		PolyStringTranslationType translation = new PolyStringTranslationType();
    		translation.setKey("StandardLoggerType." + standardLogger.name());
    		label.setTranslation(translation);
    		row.setLabel(label);
    		list.add(row);
    	}
    	for(LoggingComponentType componentLogger : componentLoggers) {
    		LookupTableRowType row = new LookupTableRowType();
    			String value = ComponentLoggerType.getPackageByValue(componentLogger);
    		row.setKey(value);
    		row.setValue(value);
    		PolyStringType label = new PolyStringType("LoggingComponentType." + componentLogger.name());
    		PolyStringTranslationType translation = new PolyStringTranslationType();
    		translation.setKey("LoggingComponentType." + componentLogger.name());
    		label.setTranslation(translation);
    		row.setLabel(label);
    		list.add(row);
    	}
        return lookupTable;
		
	}

}
