/*
 * Copyright (c) 2010-2019 Evolveum
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
package com.evolveum.midpoint.web.model;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.factory.WrapperContext;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;

/**
 * @author katka
 *
 */
public abstract class ItemWrapperModel<C extends Containerable, IW extends ItemWrapper> implements IModel<IW> {

	private static final transient Trace LOGGER  = TraceManager.getTrace(ItemWrapperModel.class);
	
	
	private IModel<?> parent;
	protected ItemPath path;
	private boolean fromContainerValue;
	
	
	ItemWrapperModel(IModel<?> parent, ItemPath path, boolean fromContainerValue) {
		this.parent = parent;
		this.path = path;
		this.fromContainerValue = fromContainerValue;
	}
	
	<W extends ItemWrapper> W getItemWrapper(Class<W> type) {
		try {
			
			if (fromContainerValue) {
				return findItemWrapperInContainerValue(type, (PrismContainerValueWrapper<C>)parent.getObject(), path);
			}
			
			return findItemWrapperInContainer(type, (PrismContainerWrapper<C>)parent.getObject(), path);
		} catch (SchemaException e) {
			LOGGER.error("Cannot get {} with path {} from parent {}\nReason: {}", type, path, parent, e.getMessage(), e);
			return null;
		}
	}
	
	private <W extends ItemWrapper> W findItemWrapperInContainerValue(Class<W> type, PrismContainerValueWrapper containerValue,
			ItemPath path)
			throws SchemaException {
		LOGGER.trace("Finding {} with path {} in {}", type.getSimpleName(), path, containerValue);
		return (W) containerValue.findItem(path, type);
	}
	
	private <W extends ItemWrapper> W findItemWrapperInContainer(Class<W> type, PrismContainerWrapper container, ItemPath path)
			throws SchemaException {
		LOGGER.trace("Finding {} with path {} in {}", type.getSimpleName(), path, container);
		return (W)container.findItem(path, type);
	}
	
	<W extends ItemWrapper> W getItemWrapperForHeader(Class<W> type, PageBase pageBase) {
		W item = getItemWrapper(type);
		if(item != null) {
			return item;
		}
		if(path.isEmpty()) {
			return null;
		}
		
		ItemPath usedPath = null;
		item = (W)parent.getObject();
		ItemPath path = this.path;
		while(!path.isEmpty()) {
			Object partOfPath = path.first();
			path = path.rest();
			if(ItemPath.isId(partOfPath)){
				usedPath = ItemPath.create(partOfPath);
				continue;
			}
			if(usedPath != null && ItemPath.isId(usedPath)){
				usedPath = ItemPath.create(usedPath, partOfPath);
			} else {
				usedPath = ItemPath.create(partOfPath);
			}
			
			try {
				
				if (item instanceof PrismContainerValueWrapper) {
					item = (W)findItemWrapperInContainerValue(ItemWrapper.class, (PrismContainerValueWrapper) item, usedPath);
				} else if (item instanceof PrismContainerWrapper) {
					PrismContainerWrapper container = (PrismContainerWrapper) item;
					if(item.isSingleValue()) {
						item = (W)findItemWrapperInContainer(ItemWrapper.class, container, usedPath);
					} else {
						PrismContainerValueWrapper value;
						if(!container.getValues().isEmpty()) {
							value = (PrismContainerValueWrapper)container.getValues().iterator().next();
						} else {
							WrapperContext context = new WrapperContext(null, null);
							PrismContainerValue<?> newValue = pageBase.getPrismContext().itemFactory().createContainerValue();
							value = pageBase.createValueWrapper(container, newValue, ValueStatus.ADDED, context);
						}
						LOGGER.trace("Finding {} with path {} in {}", ItemWrapper.class.getSimpleName(), usedPath, value);
						item = (W)value.findItem(usedPath, ItemWrapper.class);
					}
				} else {
					return null;
				}
				
			} catch (SchemaException e) {
				LOGGER.error("Cannot get {} with path {} from parent {}\nReason: {}", ItemWrapper.class, usedPath, item, e.getMessage(), e);
				return null;
			}
			
		}
		System.out.println("XXXXXXXXXXXXx item " + item.debugDump());
		System.out.println("XXXXXXXXXXXXx condition  " + type.isAssignableFrom(item.getClass()));
		System.out.println("XXXXXXXXXXXXx ---------------------------- ");
		if (type.isAssignableFrom(item.getClass())) {
			return item;
		}
		return null;
	}
	
}
