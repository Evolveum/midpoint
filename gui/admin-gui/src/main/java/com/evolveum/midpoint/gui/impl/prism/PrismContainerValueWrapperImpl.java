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

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.factory.PrismReferenceValueWrapperImpl;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;

/**
 * @author katka
 *
 */
public class PrismContainerValueWrapperImpl<C extends Containerable> extends PrismValueWrapperImpl<C, PrismContainerValue<C>> implements PrismContainerValueWrapper<C> {

	private static final long serialVersionUID = 1L;

	private static final transient Trace LOGGER = TraceManager.getTrace(PrismReferenceValueWrapperImpl.class);
	
	private boolean expanded;
	private boolean showMetadata;
	private boolean sorted; 
	private boolean showEmpty;
	private boolean readOnly;
	private boolean selected;
	
	private List<ItemWrapper<?, ?, ?,?>> items = new ArrayList<>();
	
	public PrismContainerValueWrapperImpl(PrismContainerWrapper<C> parent, PrismContainerValue<C> pcv, ValueStatus status) {
		super(parent, pcv, status);
	}
	
	@Override
	public PrismContainerValue<C> getValueToAdd() throws SchemaException {
		Collection<ItemDelta> modifications = new ArrayList<ItemDelta>();
		for (ItemWrapper<?, ?, ?, ?> itemWrapper : items) {
			Collection<ItemDelta> subDelta =  itemWrapper.getDelta();
			
			if (subDelta != null && !subDelta.isEmpty()) {
				modifications.addAll(subDelta);
			}
		}
		if (!modifications.isEmpty()) {
			PrismContainerValue<C> valueToAdd = getOldValue().clone();
			for (ItemDelta delta : modifications) {
				delta.applyTo(valueToAdd);
			}
			return valueToAdd;
		}
	
		
		return null;
	}
	
	@Override
	public <ID extends ItemDelta> void applyDelta(ID delta) throws SchemaException {
		if (delta == null) {
			return;
		}
		
		LOGGER.trace("Applying {} to {}", delta, getNewValue());
		delta.applyTo(getNewValue());
	}
	
	@Override
	public void setRealValue(C realValue) {
		LOGGER.trace("Nothing to do");
	}
	
	@Override
	public String getDisplayName() {
		if (getParent().isSingleValue()) {
			return getParent().getDisplayName();
		}
		return WebComponentUtil.getDisplayName(getNewValue());
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
		for (ItemWrapper<?,?,?,?> container : items) {
			if (container.getTypeName().equals(MetadataType.COMPLEX_TYPE)) {
				return true;
			}
		}
		
		return false;
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
			if (!(item instanceof PrismContainerWrapper)) {
				((List)nonContainers).add(item);
			}
		}
		return nonContainers;
	}
	
	private PrismContainerDefinition<C> getContainerDefinition() {
		return getNewValue().getDefinition();
	}

	private ContainerStatus findObjectStatus() {
		return ContainerStatus.ADDING;
	}
	
		
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper#findContainer(com.evolveum.midpoint.prism.path.ItemPath)
	 */
	@Override
	public <T extends Containerable> PrismContainerWrapper<T> findContainer(ItemPath path) throws SchemaException {
		PrismContainerWrapper<T> container = findItem(path, PrismContainerWrapper.class);
		return container;
	}
     
	@Override
	public <IW extends ItemWrapper> IW findItem(ItemPath path, Class<IW> type) throws SchemaException {
		Object first = path.first();
    	if (!ItemPath.isName(first)) {
    		throw new IllegalArgumentException("Attempt to lookup item using a non-name path "+path+" in "+this);
    	}
    	ItemName subName = ItemPath.toName(first);
    	ItemPath rest = path.rest();
        IW item = findItemByQName(subName);
        if (item != null) {
            if (rest.isEmpty()) {
                if (type.isAssignableFrom(item.getClass())) {
                    return (IW) item;
                } 
            } else {
                // Go deeper
                if (item instanceof PrismContainerWrapper) {
                    return ((PrismContainerWrapper<?>)item).findItem(rest, type);
                } 
            }
        }
        
        return null;
	}
        
        private <IW extends ItemWrapper> IW findItemByQName(QName subName) throws SchemaException {
	        if (items == null) {
	            return null;
	        }
	        IW matching = null;
	        for (ItemWrapper<?, ?, ?, ?> item : items) {
	            if (QNameUtil.match(subName, item.getName())) {
	                if (matching != null) {
	                    String containerName = getParent() != null ? DebugUtil.formatElementName(getParent().getName()) : "";
	                    throw new SchemaException("More than one items matching " + subName + " in container " + containerName);
	                } else {
	                    matching = (IW) item;
	                }
	            }
	        }
	        return matching;
	    }

	@Override
	public <X> PrismPropertyWrapper<X> findProperty(ItemPath propertyPath) throws SchemaException {
		return findItem(propertyPath, PrismPropertyWrapper.class);
	}

	@Override
	public <R extends Referencable> PrismReferenceWrapper<R> findReference(ItemPath path) throws SchemaException {
		return findItem(path, PrismReferenceWrapper.class);
	}

	@Override
	public ItemPath getPath() {
		return getNewValue().getPath();
	}
	
	@Override
	public boolean isSelected() {
		return selected;
	}

	@Override
	public boolean setSelected(boolean selected) {
		return this.selected = selected;
	}

	

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper#hasChanged()
	 */
	@Override
	public boolean hasChanged() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder(super.debugDump(indent));
		sb.append("Items:\n");
		for (ItemWrapper<?, ?, ?, ?> item: items) {
			sb.append(item).append("\n");
		}
			
		return sb.toString();
	}


	@Override
	public boolean isReadOnly() {
		return readOnly;
	}


	@Override
	public void setReadOnly(boolean readOnly, boolean recursive) {
		this.readOnly = readOnly;
	}


	@Override
	public boolean isShowEmpty() {
		return showEmpty;
	}


	@Override
	public void setShowEmpty(boolean showEmpty) {
		this.showEmpty = showEmpty;
		computeStripes();
	}
	
	
	@Override
	public void sort() {
		Locale locale = WebModelServiceUtils.getLocale();
		if (locale == null) {
			locale = Locale.getDefault();
		}
		Collator collator = Collator.getInstance(locale);
		collator.setStrength(Collator.SECONDARY);       // e.g. "a" should be different from "á"
		collator.setDecomposition(Collator.FULL_DECOMPOSITION); 
		
		Collections.sort(getNonContainers(), new ItemWrapperComparator<>(collator, sorted));
//		Collections.sort(getContainers(), new ItemWrapperComparator<>(collator));
		
		computeStripes();
	}
	
	private void computeStripes() {
		if (getNonContainers() == null) {
			return;
		}
		int visibleProperties = 0;

 		for (ItemWrapper<?,?,?,?> item : getNonContainers()) {
			if (item.isVisible(null)) {
				visibleProperties++;
			}
			
			if (visibleProperties % 2 == 0) {
				item.setStripe(true);
			} else {
				item.setStripe(false);
			}
			
		}
	}
}
