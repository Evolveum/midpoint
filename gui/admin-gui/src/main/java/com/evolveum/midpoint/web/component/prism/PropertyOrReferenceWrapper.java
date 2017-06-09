/*
 *  Copyright (c) 2010-2016 Evolveum
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
import com.evolveum.midpoint.util.exception.SchemaException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
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
public abstract class PropertyOrReferenceWrapper<I extends Item<? extends PrismValue, ID>, ID extends ItemDefinition> implements ItemWrapper<I, ID>, Serializable {

	private static final long serialVersionUID = -179218652752175177L;

	protected ContainerWrapper container;
	protected I item;
	protected ID itemDefinition;
	protected ValueStatus status;
	protected List<ValueWrapper> values;
	protected String displayName;
	protected boolean readonly;
	private boolean isStripe;

	public PropertyOrReferenceWrapper(@Nullable ContainerWrapper container, I item, boolean readonly, ValueStatus status) {
		Validate.notNull(item, "Item must not be null.");
		Validate.notNull(status, "Item status must not be null.");

		this.container = container;
		this.item = item;
		this.itemDefinition = getItemDefinition();
		this.status = status;
		this.readonly = readonly;
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
			itemDefinition.revive(prismContext);
		}
	}

	@Override
	public ID getItemDefinition() {
		ID definition = null;
		if (container != null && container.getItemDefinition() != null) {
			definition = (ID) container.getItemDefinition().findItemDefinition(item.getDefinition().getName());
		}
		if (definition == null) {
			definition = item.getDefinition();
		}
		return definition;
	}

	public boolean isVisible() {
        if (item.getDefinition().isOperational()) {			// TODO ...or use itemDefinition instead?
			return false;
		} else if (container != null) {
			return container.isItemVisible(this);
		} else {
			return true;
		}
	}

	public boolean isStripe() {
		return isStripe;
	}

	public void setStripe(boolean isStripe) {
		this.isStripe = isStripe;
	}

	public ContainerWrapper getContainer() {
	        return container;
	    }

	@Override
	public String getDisplayName() {
		if (StringUtils.isNotEmpty(displayName)) {
			return displayName;
		}
		return ContainerWrapper.getDisplayNameFromItem(item);
	}

	@Override
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
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

	public void addValue() {
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

	@Override
	public boolean isReadonly() {
		return readonly;
	}

	public void setReadonly(boolean readonly) {
		this.readonly = readonly;
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
		if (itemDefinition == null || !itemDefinition.isMandatory()) {
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
}
