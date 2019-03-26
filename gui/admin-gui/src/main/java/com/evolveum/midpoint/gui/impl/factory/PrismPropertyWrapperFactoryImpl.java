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

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.PrismValueWrapper;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;

/**
 * @author katka
 *
 */
public class PrismPropertyWrapperFactoryImpl<T> extends ItemWrapperFacotryImpl<PrismPropertyWrapper<T>, PrismPropertyValue<T>, PrismProperty<T>>{

	@Override
	public boolean match(ItemDefinition<?> def) {
		return def instanceof PrismPropertyDefinition;
	}

	@Override
	public void register() {
		getRegistry().addToRegistry(this);
	}

	@Override
	public int getOrder() {
		return Integer.MAX_VALUE;
	}

	@Override
	protected PrismPropertyValue<T> createNewValue(PrismProperty<T> item) throws SchemaException {
		PrismPropertyValue<T> newValue = getPrismContext().itemFactory().createPropertyValue();
		item.add(newValue);
		return newValue;
	}

	@Override
	protected PrismPropertyWrapper<T> createWrapper(PrismContainerValueWrapper<?> parent, PrismProperty<T> item,
			ItemStatus status) {
		PrismPropertyWrapper<T> propertyWrapper = new PrismPropertyWrapperImpl<>(parent, item, status);
		return propertyWrapper;
	}
	
	@Override
	public PrismValueWrapper<?> createValueWrapper(PrismPropertyWrapper<T> parent, PrismPropertyValue<T> value,
			ValueStatus status, WrapperContext context) throws SchemaException {
		// TODO Auto-generated method stub
		return null;
	}

}
