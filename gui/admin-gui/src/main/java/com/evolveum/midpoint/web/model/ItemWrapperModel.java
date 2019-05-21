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

import javax.xml.namespace.QName;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.factory.WrapperContext;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.task.api.Task;
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
	
	<ID extends ItemDefinition> ItemWrapper getItemWrapperForHeader(Class<ID> type, PageBase pageBase) {
		if(path.isEmpty()) {
			return null;
		}
		
		ItemWrapper item = null;
		ItemDefinition def = null;
		ItemPath path = this.path;
		try {
		if (fromContainerValue) {
			return null;
		} else {
			PrismContainerWrapper container = (PrismContainerWrapper) this.parent.getObject();
			def = container.getItem().getDefinition().findItemDefinition(path, type);
		}
		if (!type.isAssignableFrom(def.getClass())) {
			return null;
		}
		return createItemWrapper(def.instantiate(), pageBase);
			} catch (SchemaException e) {
				LOGGER.error("Cannot get {} with path {} from parent {}\nReason: {}", ItemWrapper.class, path, this.parent.getObject(), e.getMessage(), e);
				return null;
			}
	}

	private ItemWrapper createItemWrapper(Item i, PageBase pageBase) throws SchemaException {
		Task task = pageBase.createSimpleTask("Create wrapper for column header");
		return pageBase.createItemWrapper(i, ItemStatus.NOT_CHANGED, new WrapperContext(task, task.getResult()));
	}
	
}
