/*
 *  Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.web.component.prism;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.List;

/**
 * Extracts common functionality of PropertyWrapper and ReferenceWrapper.
 * TODO consider unifying with ContainerWrapper as well.
 *
 * @author mederly
 */
public abstract class PropertyOrReferenceWrapper<I extends Item<? extends PrismValue, ID>, ID extends ItemDefinition<I>> implements ItemWrapper<I, ID, ValueWrapper>, Serializable {

	private static final long serialVersionUID = -179218652752175177L;

	@Nullable protected ContainerValueWrapper container;
	protected I item;
	protected ValueStatus status;
	protected List<ValueWrapper> values;
	
	// processed and localized display name
	protected String displayName;
	
	protected boolean readonly;
	private boolean isStripe;
	private boolean showEmpty;
	private ItemPath path;

	public PropertyOrReferenceWrapper(@Nullable ContainerValueWrapper containerValue, I item, boolean readonly, ValueStatus status, ItemPath path) {
		Validate.notNull(item, "Item must not be null.");
		Validate.notNull(status, "Item status must not be null.");

		this.container = containerValue;
		this.item = item;
		this.status = status;
		this.readonly = readonly;
		this.path = path;
	}

	@Override
	public QName getName() {
		return getItem().getElementName();
	}

	public void revive(PrismContext prismContext) throws SchemaException {
		if (getItem() != null) {
			getItem().revive(prismContext);
		}
		if (getItemDefinition() != null) {
			getItemDefinition().revive(prismContext);
		}
	}

	@Override
	public ID getItemDefinition() {
		return item.getDefinition();
		
	}
	
	@Override
	@Nullable
	public ContainerWrapper getParent() {
		return container != null ? container.getContainer() : null;
	}

	public boolean isVisible() {
		
        if (getItemDefinition().isOperational() && !isMetadataContainer()) {			// TODO ...or use itemDefinition instead?
			return false;
		} 
        
        if (getItemDefinition().isDeprecated() && isEmpty()) {
        	return false;
        }

        if (container == null) {
        	return false;           // TODO: ok ?
        }
        switch (container.getObjectStatus()) {
        	case ADDING : 
        		return canAddDefault() || canAddAndShowEmpty();
        	case MODIFYING :
        		return canReadOrModifyAndNonEmpty() || canReadOrModifyAndShowEmpty();
        }
//        if (getItem().isEmpty() && isS)
//        else if (container != null) {
//			return container.isItemVisible(this);
//		} else {
			return true;
//		}
	}
	
	private boolean canAddAndShowEmpty() {
		return getItemDefinition().canAdd() && isShowEmpty();
	}
	
	private boolean canAddDefault() {
		return getItemDefinition().canAdd() && getItemDefinition().isEmphasized();
	}
	
	private boolean canReadOrModifyAndNonEmpty() {
		return getItemDefinition().canRead() && (!getItem().isEmpty() || getItemDefinition().isEmphasized()); //(getItemDefinition().canModify() || getItemDefinition().canRead()) && !getItem().isEmpty();
	}
	
	private boolean canReadOrModifyAndShowEmpty() {
		return getItemDefinition().canRead() && isShowEmpty(); //(getItemDefinition().canModify() || getItemDefinition().canRead()) && isShowEmpty();
	}

	public boolean isStripe() {
		return isStripe;
	}

	public void setStripe(boolean isStripe) {
		this.isStripe = isStripe;
	}

	public ContainerValueWrapper getContainerValue() {
	        return container;
	    }

	// TODO: unify with ContainerWrapper.getDisplayName()
	@Override
	public String getDisplayName() {
		if (displayName == null) {
			// Lazy loading of a localized name.
			// We really want to remember a processed name in the wrapper.
			// getDisplatName() method may be called many times, e.g. during sorting.
			displayName = ContainerWrapper.getDisplayNameFromItem(item);
		}
		return displayName;
	}
	
	@Override
	public void setDisplayName(String displayName) {
		this.displayName = ContainerWrapper.localizeName(displayName);
	}

	public ValueStatus getStatus() {
		return status;
	}

	public void setStatus(ValueStatus status) {
		this.status = status;
	}

	public List<ValueWrapper> getValues() {
		return values;
	}
	
	public boolean isShowEmpty() {
		return showEmpty;
	}
	
	public void setShowEmpty(boolean showEmpty, boolean recursive) {
		this.showEmpty = showEmpty;
	}

	public void addValue(boolean showEmpty) {
		this.showEmpty = showEmpty;
		getValues().add(createAddedValue());
	}

	public abstract ValueWrapper createAddedValue();

	@Override
	public I getItem() {
		return item;
	}

//	public ItemDefinition getDefinition() {
//		return item.getDefinition();
//	}

	public boolean hasChanged() {
		for (ValueWrapper value : getValues()) {
			switch (value.getStatus()) {
				case DELETED:
					return true;
				case ADDED:
				case NOT_CHANGED:
					if (value.hasValueChanged()) {
						return true;
					}
			}
		}

		return false;
	}
	private boolean isMetadataContainer() {
		ContainerWrapper parent = getParent();
		if (parent == null) {
			return false;
		}
		ItemDefinition<?> definition = parent.getItemDefinition();
		if (definition == null) {
			return false;
		}
		return definition.getTypeName().equals(MetadataType.COMPLEX_TYPE);
	}

	@Override
	public boolean isReadonly() {
		//TODO this is probably not good idea
		if (isMetadataContainer()) {
			return true;
		}
		
		return readonly;
	}

	public void setReadonly(boolean readonly) {
		this.readonly = readonly;
	}
	
	@Override
	public ItemPath getPath() {
		if (path == null) {
			path = item.getPath();
		}
		return path;
	}

	@Override
	public boolean isEmpty() {
		return getItem().isEmpty();
	}

	public I getUpdatedItem(PrismContext prismContext) throws SchemaException {
		final Item updatedItem = item.clone();
		updatedItem.clear();
		for (ValueWrapper valueWrapper : getValues()) {
			valueWrapper.normalize(prismContext);
			if (ValueStatus.DELETED.equals(valueWrapper.getStatus())) {
				updatedItem.remove(valueWrapper.getValue());
			} else if (!updatedItem.hasRealValue(valueWrapper.getValue())) {
				PrismValue cloned = ObjectWrapper.clone(valueWrapper.getValue());
				if (cloned != null) {
					updatedItem.add(cloned);
				}
			}
		}
		return (I) updatedItem;
	}

	@Override
	public boolean checkRequired(PageBase pageBase) {
		if (getItemDefinition() == null || !getItemDefinition().isMandatory()) {
			return true;
		}
		for (ValueWrapper valueWrapper : CollectionUtils.emptyIfNull(getValues())) {
			if (!valueWrapper.isEmpty()) {
				return true;
			}
		}
		pageBase.error("Item '" + getDisplayName() + "' must not be empty");
		return false;
	}
	
	@Override
	public boolean isDeprecated() {
		return getItemDefinition().isDeprecated();
	}
	
	@Override
	public String getDeprecatedSince() {
		return getItemDefinition().getDeprecatedSince();
	}
}
