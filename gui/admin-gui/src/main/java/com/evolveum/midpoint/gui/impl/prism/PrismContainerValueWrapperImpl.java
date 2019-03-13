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

import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
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
	
	private PrismContainerWrapperImpl<C> parent;
	
	private List<ItemWrapperImpl<?, ?, ?>> items;
	
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
	public boolean hasMetadata() {
		for (ItemWrapperImpl<?,?,?> container : items) {
			if (container.getItemDefinition().getTypeName().equals(MetadataType.COMPLEX_TYPE)) {
				return true;
			}
		}
		
		return false;
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
	
	protected List<PrismContainerWrapperImpl<C>> getContainers() {
		List<PrismContainerWrapperImpl<C>> containers = new ArrayList<>();
		for (ItemWrapperImpl<?,?,?> container : items) {
			if (container instanceof PrismContainerWrapperImpl) {
				containers.add((PrismContainerWrapperImpl<C>) container);
			}
		}
		return containers;
	}
	
	protected List<? extends ItemWrapperImpl<?,?,?>> getNonContainers() {
		List<? extends ItemWrapperImpl<?,?,?>> nonContainers = new ArrayList<>();
		for (ItemWrapperImpl<?,?,?> item : items) {
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
}
