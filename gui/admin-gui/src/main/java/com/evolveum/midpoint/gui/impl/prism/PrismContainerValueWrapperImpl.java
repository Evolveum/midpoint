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

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.factory.PrismReferenceWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;

/**
 * @author katka
 *
 */
public class PrismContainerValueWrapperImpl<C extends Containerable> implements PrismContainerValueWrapper<C>{

	private static final long serialVersionUID = 1L;

	private PrismContainerValue<C> oldValue;
	private PrismContainerValue<C> newValue;
	
	private boolean expanded;
	private boolean showMetadata;
	private boolean sorted; 
	
	private ValueStatus status;
	
	private PrismContainerWrapper<C> parent;
	
	private List<ItemWrapper<?, ?, ?,?>> items;
	
	public PrismContainerValueWrapperImpl(PrismContainerWrapper<C> parent, PrismContainerValue<C> pcv, ValueStatus status) {
		this.parent = parent;
		this.newValue = pcv;
		this.oldValue = pcv.clone();
		this.status = status;
	}
	
	@Override
	public C getRealValue() {
		return newValue.asContainerable();
	}
	
	@Override
	public String getDisplayName() {
		return null;
	}
	
	@Override
	public String getHelpText() {
		return WebPrismUtil.getHelpText(getContainerDefinition());
	}
	
	@Override
	public boolean isExpanded() {
		return expanded;
	}
	
	@Override
	public void setExpanded(boolean expanded) {
		this.expanded = expanded;
	}
	
	@Override
	public PrismContainerValue<C> getNewValue() {
		return newValue;
	}
	
	@Override
	public boolean hasMetadata() {
		for (ItemWrapper<?,?,?,?> container : items) {
			if (container.getTypeName().equals(MetadataType.COMPLEX_TYPE)) {
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public PrismContainerWrapper<C> getParent() {
		return parent;
	}
	
	@Override
	public List<ItemWrapper<?, ?, ?,?>> getItems() {
		return items;
	}
	
	
	@Override
	public boolean isShowMetadata() {
		return showMetadata;
	}
	
	@Override
	public void setShowMetadata(boolean showMetadata) {
		this.showMetadata = showMetadata;
	}
	
	@Override
	public boolean isSorted() {
		return sorted;
	}
	
	@Override
	public void setSorted(boolean sorted) {
		this.sorted = sorted;
	}
	
	@Override
	public List<PrismContainerDefinition<C>> getChildContainers() {
		List<PrismContainerDefinition<C>> childContainers = new ArrayList<>();
		for (ItemDefinition<?> def : getContainerDefinition().getDefinitions()) {
			if (!(def instanceof PrismContainerDefinition) || def.isSingleValue()) {
				continue;
			}
			
			ContainerStatus objectStatus = findObjectStatus();
			
			boolean allowed = false;
			switch (objectStatus) {
				case ADDING:
					allowed = def.canAdd();
					break;
				case MODIFYING:
				case DELETING:
					allowed = def.canModify();
			}
			
			if (allowed) {
				childContainers.add((PrismContainerDefinition<C>)def);
			}
		}
		
		return childContainers;
	}
	
	@Override
	public ValueStatus getStatus() {
		return status;
	}
	
	@Override
	public void setStatus(ValueStatus status) {
		this.status = status;
	}
	
	@Override
	public List<PrismContainerWrapper<C>> getContainers() {
		List<PrismContainerWrapper<C>> containers = new ArrayList<>();
		for (ItemWrapper<?,?,?,?> container : items) {
			if (container instanceof PrismContainerWrapper) {
				containers.add((PrismContainerWrapper<C>) container);
			}
		}
		return containers;
	}
	
	@Override
	public List<? extends ItemWrapper<?,?,?,?>> getNonContainers() {
		List<? extends ItemWrapper<?,?,?,?>> nonContainers = new ArrayList<>();
		for (ItemWrapper<?,?,?,?> item : items) {
			if (item instanceof PrismPropertyWrapperImpl<?>) {
				((List)nonContainers).add(item);
			}
		}
		return nonContainers;
	}
	
	private PrismContainerDefinition<C> getContainerDefinition() {
		return newValue.getDefinition();
	}

	private ContainerStatus findObjectStatus() {
		return ContainerStatus.ADDING;
	}

	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper#findContainer(com.evolveum.midpoint.prism.path.ItemPath)
	 */
	@Override
	public <T extends Containerable> PrismContainerWrapper<T> findContainer(ItemPath path) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper#findProperty(com.evolveum.midpoint.prism.path.ItemPath)
	 */
	@Override
	public <X> PrismPropertyWrapper<X> findProperty(ItemPath propertyPath) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper#findReference(com.evolveum.midpoint.prism.path.ItemPath)
	 */
	@Override
	public PrismReferenceWrapper findReference(ItemPath path) {
		// TODO Auto-generated method stub
		return null;
	}
}
