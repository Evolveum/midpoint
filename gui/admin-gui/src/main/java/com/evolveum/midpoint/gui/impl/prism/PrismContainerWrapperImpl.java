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
package com.evolveum.midpoint.gui.impl.prism;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.MutablePrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;

/**
 * @author katka
 *
 */
public class PrismContainerWrapperImpl<C extends Containerable> extends ItemWrapperImpl<PrismContainerValue<C>, PrismContainer<C>, PrismContainerDefinition<C>, PrismContainerValueWrapper<C>> implements PrismContainerWrapper<C>, Serializable{

	private static final long serialVersionUID = 1L;
	
	private boolean showOnTopLevel;
	
	private boolean expanded;
	

	public PrismContainerWrapperImpl(PrismContainerValueWrapper<?> parent, PrismContainer<C> item, ItemStatus status) {
		super(parent, item, status);
		//TODO move to factory
		this.expanded = !item.isEmpty();
	}

	@Override
	public void setExpanded(boolean expanded) {
		this.expanded = expanded;
	}

	@Override
	public boolean isExpanded() {
		return expanded;
	}

	@Override
	public void setShowOnTopLevel(boolean setShowOnTopLevel) {
		this.showOnTopLevel = setShowOnTopLevel;
	}

	@Override
	public boolean isShowOnTopLevel() {
		return showOnTopLevel;
	}
	
	@Override
	public Class<C> getCompileTimeClass() {
		return getItemDefinition().getCompileTimeClass();
	}

	@Override
	public ComplexTypeDefinition getComplexTypeDefinition() {
		return getItemDefinition().getComplexTypeDefinition();
	}

	@Override
	public String getDefaultNamespace() {
		return getItemDefinition().getDefaultNamespace();
	}

	@Override
	public List<String> getIgnoredNamespaces() {
		return getItemDefinition().getIgnoredNamespaces();
	}

	@Override
	public List<? extends ItemDefinition> getDefinitions() {
		return getItemDefinition().getDefinitions();
	}

	@Override
	public List<PrismPropertyDefinition> getPropertyDefinitions() {
		return getItemDefinition().getPropertyDefinitions();
	}

	@Override
	public ContainerDelta<C> createEmptyDelta(ItemPath path) {
		return getItemDefinition().createEmptyDelta(path);
	}

	@Override
	public PrismContainerDefinition<C> clone() {
		return getItemDefinition().clone();
	}

	@Override
	public PrismContainerDefinition<C> cloneWithReplacedDefinition(QName itemName, ItemDefinition newDefinition) {
		return getItemDefinition().cloneWithReplacedDefinition(itemName, newDefinition);
	}

	@Override
	public void replaceDefinition(QName itemName, ItemDefinition newDefinition) {
		getItemDefinition().replaceDefinition(itemName, newDefinition);
	}

	@Override
	public PrismContainerValue<C> createValue() {
		return getItemDefinition().createValue();
	}

	@Override
	public boolean canRepresent(QName type) {
		return getItemDefinition().canRepresent(type);
	}

	@Override
	public MutablePrismContainerDefinition<C> toMutable() {
		return getItemDefinition().toMutable();
	}

	@Override
	public <ID extends ItemDefinition> ID findLocalItemDefinition(QName name, Class<ID> clazz, boolean caseInsensitive) {
		return getItemDefinition().findLocalItemDefinition(name, clazz, caseInsensitive);
	}

	@Override
	public <ID extends ItemDefinition> ID findNamedItemDefinition(QName firstName, ItemPath rest, Class<ID> clazz) {
		return getItemDefinition().findNamedItemDefinition(firstName, rest, clazz);
	}

		
	//TODO : unify with PrismContainerImpl findContainer();
	@Override
	public <T extends Containerable> PrismContainerWrapper<T> findContainer(ItemPath path) throws SchemaException {
		return findItem(path, PrismContainerWrapper.class);
	}

	private PrismContainerValueWrapper<C> findValue(Long id) {
		if (isSingleValue()) {
			if (getValues() != null) {
				return getValues().iterator().next();
			}
		}
		
		for (PrismContainerValueWrapper<C> value : getValues()) {
			PrismContainerValue<C> newValue = value.getNewValue();
			if (id == null) {
				//TODO : what to do?? can be recently added
				return null;
			}
			if (id.equals(newValue.getId())) {
				return value;
			}
		}
		
		return null;
	}
	
	@Override
	public <X> PrismPropertyWrapper<X> findProperty(ItemPath propertyPath) throws SchemaException {
		return findItem(propertyPath, PrismPropertyWrapper.class);
	}

	@Override
	public PrismReferenceWrapper findReference(ItemPath path) throws SchemaException {
		return findItem(path, PrismReferenceWrapper.class);
	}

	@Override
	public <T extends Containerable> PrismContainerValueWrapper<T> findContainerValue(ItemPath path) {
		// TODO Auto-generated method stub
		return null;
	}

	
	@Override
	public boolean isEmpty() {
		return getItem().isEmpty();
	}

	@Override
	public <IW extends ItemWrapper> IW findItem(ItemPath path, Class<IW> type) throws SchemaException {
		if (ItemPath.isEmpty(path)) {
			if (type.isAssignableFrom(this.getClass())) {
				return (IW) this;
			}
    		return null;
    	}
    	
		Long id = path.firstToIdOrNull();
    	PrismContainerValueWrapper<C> cval = findValue(id);
    	if (cval == null) {
    		return null;
    	}
    	// descent to the correct value
	    ItemPath rest = path.startsWithId() ? path.rest() : path;
    	return cval.findItem(rest, type);
	}

	@Override
	public String debugDump(int indent) {
		return super.debugDump(indent);
	}
	
	protected <C extends Containerable> void cleanupEmptyContainers(PrismContainer<C> container) {
		List<PrismContainerValue<C>> values = container.getValues();
		List<PrismContainerValue<C>> valuesToBeRemoved = new ArrayList<>();
		for (PrismContainerValue<C> value : values) {
			List<Item<?,?>> items = value.getItems();
			if (items != null) {
				Iterator<Item<?,?>> iterator = items.iterator();
				while (iterator.hasNext()) {
					Item<?,?> item = iterator.next();

					if (item instanceof PrismContainer) {
						cleanupEmptyContainers((PrismContainer) item);

						if (item.isEmpty()) {
							iterator.remove();
						}
					} else {
//						cleanupEmptyItem()
					}
				}
			}

			if (items == null || value.isEmpty()) {
				valuesToBeRemoved.add(value);
			}
		}

		container.removeAll(valuesToBeRemoved);
	}
	
	@Override
	public <D extends ItemDelta<PrismContainerValue<C>, PrismContainerDefinition<C>>> Collection<D> getDelta() throws SchemaException {
		
		Collection<D> deltas = new ArrayList<D>();
		
		
		ContainerDelta<C> delta = createEmptyDelta(getPath());
		
		switch (getStatus()) {
		
		case ADDED:
			for (PrismContainerValueWrapper<C> pVal : getValues()) {
				PrismContainerValue<C> valueToAdd = pVal.getValueToAdd();
				if (valueToAdd != null) {
					delta.addValueToAdd(valueToAdd);
				}
			}
			
			if (delta.isEmpty()) {
				return null;
			}
			
			return (Collection) MiscUtil.createCollection(delta);
		case NOT_CHANGED:
			
			for (PrismContainerValueWrapper<C> pVal : getValues()) {
				switch (pVal.getStatus()) {
					case ADDED: 
						PrismContainerValue<C> valueToAdd = pVal.getValueToAdd();
						if (valueToAdd != null) {
							delta.addValueToAdd(valueToAdd);
						}
						break;
					case NOT_CHANGED:
						for (ItemWrapper iw : pVal.getItems()) {
							Collection iwDetlas = iw.getDelta();
							if (iwDetlas != null && !iwDetlas.isEmpty()) {
								deltas.addAll(iwDetlas);
							}
						}
						break;
						
					case DELETED:
						delta.addValueToDelete(pVal.getOldValue().clone());
						break;
				}
			}
			
			break;
		case DELETED :
			for (PrismContainerValueWrapper<C> pVal : getValues()) {
				delta.addValueToDelete(pVal.getOldValue().clone());
			}
			break;
			
		}
		
		if (!delta.isEmpty()) {
			deltas.add((D) delta);
		}
		
		if (deltas.isEmpty()) {
			return null;
		}
		
		return deltas;
	}
	
	@Override
	public boolean isVisible(ItemVisibilityHandler visibilityHandler) {
		
		if (getComplexTypeDefinition().getTypeName().equals(MetadataType.COMPLEX_TYPE)) {
			return (getParent() != null && getParent().isShowMetadata());
		}
		
		return super.isVisible(visibilityHandler);
	}

		
}
