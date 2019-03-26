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

import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.registry.GuiComponentRegistryImpl;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;

/**
 * @author katka
 *
 */
public class ContainerWrapperFactoryImpl<C extends Containerable> extends ItemWrapperFacotryImpl<PrismContainerWrapper<C>, PrismContainerValue<C>, PrismContainer<C>> {

	@Autowired private GuiComponentRegistryImpl registry;
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.impl.factory.WrapperFactory#match(com.evolveum.midpoint.prism.ItemDefinition)
	 */
	@Override
	public boolean match(ItemDefinition<?> def) {
		return def instanceof PrismContainerDefinition;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.impl.factory.WrapperFactory#register()
	 */
	@Override
	public void register() {
		registry.addToRegistry(this);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.impl.factory.WrapperFactory#getOrder()
	 */
	@Override
	public int getOrder() {
		return Integer.MAX_VALUE;
	}

	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.impl.factory.ItemWrapperFactory#createValueWrapper(com.evolveum.midpoint.prism.PrismValue, com.evolveum.midpoint.web.component.prism.ValueStatus, com.evolveum.midpoint.gui.impl.factory.WrapperContext)
	 */
	@Override
	public PrismValueWrapper<?> createValueWrapper(PrismContainerWrapper<C> parent, PrismContainerValue<C> value, ValueStatus status, WrapperContext context)
			throws SchemaException {
		PrismContainerValueWrapper<C> containerValueWrapper = new PrismContainerValueWrapperImpl<C>(parent, value, status);
		
		List<ItemWrapper<?,?,?,?>> wrappers = new ArrayList<>();
		for (ItemDefinition<?> def : parent.getDefinitions()) {
			ItemWrapperFactory<?, ?> factory = registry.findWrapperFactory(def);
			
			ItemWrapper<?,?,?,?> wrapper = factory.createWrapper(containerValueWrapper, def, context);
			wrappers.add(wrapper);
			
		}
		
		containerValueWrapper.getItems().addAll((Collection) wrappers);
		return containerValueWrapper;
	}

	@Override
	protected PrismContainerValue<C> createNewValue(PrismContainer<C> item) {
		return item.createNewValue();
	}

	@Override
	protected PrismContainerWrapper<C> createWrapper(PrismContainerValueWrapper<?> parent, PrismContainer<C> childContainer,
			ItemStatus status) {
		return new PrismContainerWrapperImpl<C>((PrismContainerValueWrapper<C>) parent, childContainer, status);
	}
	

}
