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
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoginEventType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintPresentationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * @author katka
 *
 */
@Component
public class PrismContainerWrapperFactoryImpl<C extends Containerable> extends ItemWrapperFactoryImpl<PrismContainerWrapper<C>, PrismContainerValue<C>, PrismContainer<C>, PrismContainerValueWrapper<C>> implements PrismContainerWrapperFactory<C>{

	private static final transient Trace LOGGER = TraceManager.getTrace(PrismContainerWrapperFactoryImpl.class);
	
	@Autowired private GuiComponentRegistryImpl registry;
	
	@Override
	public boolean match(ItemDefinition<?> def) {
		return  def instanceof PrismContainerDefinition;
	}

	@PostConstruct
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
	public PrismContainerValueWrapper<C> createValueWrapper(PrismContainerWrapper<C> parent, PrismContainerValue<C> value, ValueStatus status, WrapperContext context)
			throws SchemaException {
		PrismContainerValueWrapper<C> containerValueWrapper = createContainerValueWrapper(parent, value, status);
		containerValueWrapper.setExpanded(!value.isEmpty());
		
		
		List<ItemWrapper<?,?,?,?>> wrappers = new ArrayList<>();
		for (ItemDefinition<?> def : parent.getDefinitions()) {
			
			if (def.isOperational()) {
				continue;
			}
			
			if (SearchFilterType.COMPLEX_TYPE.equals(def.getTypeName())) {
				continue;
			}
			
			ItemWrapperFactory<?, ?, ?> factory = registry.findWrapperFactory(def);
			
			ItemWrapper<?,?,?,?> wrapper = factory.createWrapper(containerValueWrapper, def, context);
			wrappers.add(wrapper);
			
		}
		
		containerValueWrapper.getItems().addAll((Collection) wrappers);
		containerValueWrapper.sort();
		return containerValueWrapper;
	}

	@Override
	protected PrismContainerValue<C> createNewValue(PrismContainer<C> item) {
		return item.createNewValue();
	}

	@Override
	protected PrismContainerWrapper<C> createWrapper(PrismContainerValueWrapper<?> parent, PrismContainer<C> childContainer,
			ItemStatus status) {
		registry.registerWrapperPanel(childContainer.getDefinition().getTypeName(), PrismContainerPanel.class);
		return new PrismContainerWrapperImpl<C>((PrismContainerValueWrapper<C>) parent, childContainer, status);
	}
	
	@Override
	public PrismContainerValueWrapper<C> createContainerValueWrapper(PrismContainerWrapper<C> objectWrapper, PrismContainerValue<C> objectValue, ValueStatus status) {
		return new PrismContainerValueWrapperImpl<C>(objectWrapper, objectValue, status);
	}
	

}
