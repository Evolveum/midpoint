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
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.registry.GuiComponentRegistryImpl;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * @author katka
 *
 */
public abstract class ItemWrapperFactoryImpl<IW extends ItemWrapper, PV extends PrismValue, I extends Item, VW extends PrismValueWrapper> implements ItemWrapperFactory<IW, VW, PV> {

	private static final transient Trace LOGGER = TraceManager.getTrace(ItemWrapperFactoryImpl.class);
	
	@Autowired private GuiComponentRegistryImpl registry; 
	@Autowired private PrismContext prismContext;
	@Autowired private ModelInteractionService modelInteractionService;
	
	@Override
	public IW createWrapper(PrismContainerValueWrapper<?> parent, ItemDefinition<?> def, WrapperContext context) throws SchemaException {
		ItemName name = def.getName();
		
		I childItem = (I) parent.getNewValue().findItem(name);
		ItemStatus status = ItemStatus.NOT_CHANGED;
		if (childItem == null) {
			if (!context.isForceCreate() && !canCreateNewWrapper(def)) {
				LOGGER.trace("Skipping reating wrapper for non-existent item. It is not supported for {}", def);
				return null;
			}
			
			childItem = (I) parent.getNewValue().findOrCreateItem(name);
			status = ItemStatus.ADDED;
			
		}
		
		return createWrapper(parent, childItem, status, context);
	}
	
	
	public IW createWrapper(Item childItem, ItemStatus status, WrapperContext context) throws SchemaException {
		return createWrapper(null, (I) childItem, status, context);
		
	};
	
	private IW createWrapper(PrismContainerValueWrapper<?> parent, I childItem, ItemStatus status, WrapperContext context) throws SchemaException {
		IW itemWrapper = createWrapper(parent, childItem, status);
		
		List<VW> valueWrappers  = createValuesWrapper(itemWrapper, (I) childItem, context);
		itemWrapper.getValues().addAll((Collection) valueWrappers);
		itemWrapper.setShowEmpty(context.isShowEmpty(), false);
		itemWrapper.setReadOnly(context.isReadOnly());
		
		setupWrapper(itemWrapper);
		
		return itemWrapper;
	}
	
	protected abstract void setupWrapper(IW wrapper);
	
	protected <ID extends ItemDefinition<I>> List<VW> createValuesWrapper(IW itemWrapper, I item, WrapperContext context) throws SchemaException {
		List<VW> pvWrappers = new ArrayList<>();
		
		ID definition = (ID) item.getDefinition();
		
		//TODO : prismContainer.isEmpty() interates and check is all prismcontainervalues are empty.. isn't it confusing?
		if (item.isEmpty() && item.getValues().isEmpty()) {
			if (shouldCreateEmptyValue(item, context)) {
				PV prismValue = createNewValue(item);
				VW valueWrapper =  createValueWrapper(itemWrapper, prismValue, ValueStatus.ADDED, context);
				pvWrappers.add(valueWrapper);
			}
			return pvWrappers;
		}
		
		for (PV pcv : (List<PV>)item.getValues()) {
			if(canCreateValueWrapper(pcv)){
				VW valueWrapper = createValueWrapper(itemWrapper, pcv, ValueStatus.NOT_CHANGED, context);
				pvWrappers.add(valueWrapper);
			}
		}
		
		return pvWrappers;
	
	}
	
	protected boolean canCreateNewWrapper(ItemDefinition<?> def) {
		return true;
	}

	protected boolean canCreateValueWrapper(PV pcv) {
		return true;
	}


	protected abstract PV createNewValue(I item) throws SchemaException;
	
	protected abstract IW createWrapper(PrismContainerValueWrapper<?> parent, I childContainer, ItemStatus status);
	
	protected boolean shouldCreateEmptyValue(I item, WrapperContext context) {
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
	
	@Override
	public boolean skipCreateWrapper(ItemDefinition<?> def, WrapperContext wrapperContext) {
		if (def.isOperational()) {
			LOGGER.trace("Skipping creating wrapper for {}, because it is operational.", def.getName());
			return true;
		}
		
		if (SearchFilterType.COMPLEX_TYPE.equals(def.getTypeName())) {
			LOGGER.trace("Skipping creating wrapper for search filter.", def.getName());
			return true;
		}
		
		if (def.isExperimental() && !WebModelServiceUtils.isEnableExperimentalFeature(modelInteractionService, wrapperContext.getTask(), wrapperContext.getResult())) {
			return true;
		}
		
		return false;
	}
	

//	@Override
//	public VW createValueWrapper(PV value, IW parent, ValueStatus status, WrapperContext context) throws SchemaException {
//		// TODO Auto-generated method stub
//		return null;
//	}

}
