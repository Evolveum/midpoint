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
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.registry.GuiComponentRegistryImpl;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;

/**
 * @author katka
 *
 */
public abstract class ItemWrapperFactoryImpl<IW extends ItemWrapper, PV extends PrismValue, I extends Item, VW extends PrismValueWrapper> implements ItemWrapperFactory<IW, VW, PV> {

	private static final transient Trace LOGGER = TraceManager.getTrace(ItemWrapperFactoryImpl.class);
	
	@Autowired private GuiComponentRegistryImpl registry; 
	@Autowired private PrismContext prismContext;
	
	@Override
	public IW createWrapper(PrismContainerValueWrapper<?> parent, ItemDefinition<?> def, WrapperContext context) throws SchemaException {
		ItemName name = def.getName();
		
		I childItem = (I) parent.getNewValue().findItem(name);
		ItemStatus status = ItemStatus.NOT_CHANGED;
		if (childItem == null) {
			childItem = (I) def.instantiate();
			status = ItemStatus.ADDED;
		}
		
		IW itemWrapper = createWrapper(parent, childItem, status);
		
		
		List<VW> valueWrappers  = createValuesWrapper(itemWrapper, childItem, context);
		itemWrapper.getValues().addAll((Collection) valueWrappers);
		itemWrapper.setShowEmpty(context.isCreateIfEmpty(), false);
		return itemWrapper;
	}
	
	
	protected <ID extends ItemDefinition<I>> List<VW> createValuesWrapper(IW itemWrapper, I item, WrapperContext context) throws SchemaException {
		List<VW> pvWrappers = new ArrayList<>();
		
		ID definition = (ID) item.getDefinition();
		ItemWrapperFactory<IW, VW, PV> factory = (ItemWrapperFactory<IW, VW, PV>) registry.findWrapperFactory(definition);
		
		if (item.isEmpty()) {
			if (shoudCreateEmptyValue(item, context)) {
				PV prismValue = createNewValue(item);
				VW valueWrapper =  factory.createValueWrapper(itemWrapper, prismValue, ValueStatus.ADDED, context);
				pvWrappers.add(valueWrapper);
			}
			return pvWrappers;
		}
		
		for (PV pcv : (List<PV>)item.getValues()) {
			VW valueWrapper = factory.createValueWrapper(itemWrapper, pcv, ValueStatus.NOT_CHANGED, context);
			pvWrappers.add(valueWrapper);
		}
		
		return pvWrappers;
	
	}

	protected abstract PV createNewValue(I item) throws SchemaException;
	
	protected abstract IW createWrapper(PrismContainerValueWrapper<?> parent, I childContainer, ItemStatus status);
	
	protected boolean shoudCreateEmptyValue(I item, WrapperContext context) {
		if (item.getDefinition().isEmphasized()) {
			return true;
		}
		 
		if (context.isCreateIfEmpty()) {
			return true;
		}
		
		return true;
	}
	
	/**
	 * @return the registry
	 */
	public GuiComponentRegistryImpl getRegistry() {
		return registry;
	}
	
	/**
	 * @return the prismContext
	 */
	public PrismContext getPrismContext() {
		return prismContext;
	}

//	@Override
//	public VW createValueWrapper(PV value, IW parent, ValueStatus status, WrapperContext context) throws SchemaException {
//		// TODO Auto-generated method stub
//		return null;
//	}

}
