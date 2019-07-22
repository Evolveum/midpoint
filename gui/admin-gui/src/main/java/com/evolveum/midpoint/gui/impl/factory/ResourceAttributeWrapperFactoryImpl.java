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

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.LayerRefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.ResourceAttributeDefinitionPanel;
import com.evolveum.midpoint.gui.impl.prism.ResourceAttributeWrapper;
import com.evolveum.midpoint.gui.impl.prism.ResourceAttributeWrapperImpl;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;

/**
 * @author skublik
 *
 */
@Component
public class ResourceAttributeWrapperFactoryImpl<T> extends ItemWrapperFactoryImpl<ResourceAttributeWrapper<T>, PrismPropertyValue<T>, ResourceAttribute<T>, PrismPropertyValueWrapper<T>> {

	@Autowired private GuiComponentRegistry registry;
	
	@Override
	public boolean match(ItemDefinition<?> def) {
		return def instanceof ResourceAttributeDefinition;
	}
	
	@Override
	public int getOrder() {
		return Integer.MAX_VALUE-2;
	}

	@Override
	public PrismPropertyValueWrapper<T> createValueWrapper(ResourceAttributeWrapper<T> parent,
			PrismPropertyValue<T> value, ValueStatus status, WrapperContext context) throws SchemaException {
		PrismPropertyValueWrapper<T> valueWrapper = new PrismPropertyValueWrapper<>(parent, value, status);
		return valueWrapper;
	}

	@PostConstruct
	@Override
	public void register() {
		registry.addToRegistry(this);
	}

	@Override
	protected void setupWrapper(ResourceAttributeWrapper<T> wrapper) {
		
	}

	@Override
	protected PrismPropertyValue<T> createNewValue(ResourceAttribute<T> item) throws SchemaException {
		PrismPropertyValue<T> newValue = getPrismContext().itemFactory().createPropertyValue();
		item.add(newValue);
		return newValue;
	}

	@Override
	protected ResourceAttributeWrapper<T> createWrapper(PrismContainerValueWrapper<?> parent,
			ResourceAttribute<T> childContainer, ItemStatus status) {
		registry.registerWrapperPanel(new QName("ResourceAttributeDefinition"), ResourceAttributeDefinitionPanel.class);
		ResourceAttributeWrapper<T> propertyWrapper = new ResourceAttributeWrapperImpl<>(parent, childContainer, status);
		return propertyWrapper;
	}
}
