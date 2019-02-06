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
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.ItemWrapperOld;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.ItemProcessing;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.prism.ItemVisibilityHandler;
import com.evolveum.midpoint.web.component.prism.PropertyOrReferenceWrapper;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.prism.ValueWrapper;

/**
 * @author katka
 *
 */
public abstract class ItemWrapperImpl<PV extends PrismValue, I extends Item<PV, ID>, ID extends ItemDefinition<I>> implements ItemWrapper<PV, I, ID>, Serializable {

	private static final long serialVersionUID = 1L;
	
	private ContainerValueWrapper<?> parent;
	
	private I item = null;
	private ItemStatus status = null;
	private ItemPath fullPath = null;
	private PrismContext prismContext = null;

	private String displayName;
	
	private List<ValueWrapper<PV>> values;
	
	public ItemWrapperImpl(@Nullable ContainerValueWrapper<?> parent, I item, ItemStatus status, ItemPath fullPath, PrismContext prismContext) {
		Validate.notNull(item, "Item must not be null.");
		Validate.notNull(status, "Item status must not be null.");

		this.parent = parent;
		this.item = item;
		this.status = status;
//		this.readonly = readonly;
		this.fullPath = fullPath;
		this.prismContext = prismContext;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.Revivable#revive(com.evolveum.midpoint.prism.PrismContext)
	 */
	@Override
	public void revive(PrismContext prismContext) throws SchemaException {
		if (item != null) {
			item.revive(prismContext);
		}
		if (item.getDefinition() != null) {
			item.getDefinition().revive(prismContext);
		}
		this.prismContext = prismContext;

	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.DebugDumpable#debugDump(int)
	 */
	@Override
	public String debugDump(int indent) {
		// TODO Auto-generated method stub
		return null;
	}

	private String getDisplayName() {
		if (displayName == null) {
			displayName = getLocalizedDisplayName();
		}
		
		return displayName;
	}
	
	private String getLocalizedDisplayName() {
		Validate.notNull(item, "Item must not be null.");

		String displayName = item.getDisplayName();
		if (!StringUtils.isEmpty(displayName)) {
			return localizeName(displayName);
		}

		QName name = item.getElementName();
		if (name != null) {
			displayName = name.getLocalPart();

			PrismValue val = item.getParent();
			if (val != null && val.getTypeName() != null) {
				displayName = val.getTypeName().getLocalPart() + "." + displayName;
			}
		} else {
			displayName = item.getDefinition().getTypeName().getLocalPart();
		}

		return localizeName(displayName);
	}
	
	private String localizeName(String nameKey) {
		Validate.notNull(nameKey, "Null localization key");
		return ColumnUtils.createStringResource(nameKey).getString();
	}



	
	@Override
	public boolean hasChanged() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.api.prism.ItemWrapper#getValues()
	 */
	@Override
	public List<ValueWrapper<PV>> getValues() {
		// TODO Auto-generated method stub
		return values;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.api.prism.ItemWrapper#setStripe(boolean)
	 */
//	@Override
//	public void setStripe(boolean isStripe) {
//		// TODO Auto-generated method stub
//		
//	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.api.prism.ItemWrapper#addValue(boolean)
	 */
	@Override
	public void addValue(boolean showEmpty) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.api.prism.ItemWrapper#removeValue(com.evolveum.midpoint.web.component.prism.ValueWrapper)
	 */
	@Override
	public void removeValue(ValueWrapper<PV> valueWrapper) throws SchemaException {
		List<ValueWrapper<PV>> values = getValues();
		
		switch (valueWrapper.getStatus()) {
			case ADDED:
				values.remove(valueWrapper);
				break;
			case DELETED:
				throw new SchemaException();
			case NOT_CHANGED:
				valueWrapper.setStatus(ValueStatus.DELETED);
				break;
		}
		
		int count = countUsableValues();
		
		if (count == 0 && !hasEmptyPlaceholder()) {
			addValue(true);
			
		}
	}
	
	private int countUsableValues() {
		int count = 0;
		for (ValueWrapper<PV> value : getValues()) {
			value.normalize(prismContext);

			if (ValueStatus.DELETED.equals(value.getStatus())) {
				continue;
			}

			if (ValueStatus.ADDED.equals(value.getStatus()) && !value.hasValueChanged()) {
				continue;
			}

			count++;
		}
		return count;
	}
	
	private boolean hasEmptyPlaceholder() {
		for (ValueWrapper<PV> value : getValues()) {
			value.normalize(prismContext);
			if (ValueStatus.ADDED.equals(value.getStatus()) && !value.hasValueChanged()) {
				return true;
			}
		}

		return false;
	}


	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.api.prism.ItemWrapper#checkRequired(com.evolveum.midpoint.gui.api.page.PageBase)
	 */
	@Override
	public boolean checkRequired(PageBase pageBase) {
		return item.getDefinition().isMandatory();
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.api.prism.ItemWrapper#getParent()
	 */
	@Override
	public ContainerValueWrapper<?> getParent() {
		return parent;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.api.prism.ItemWrapper#isShowEmpty()
	 */
	@Override
	public boolean isShowEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.api.prism.ItemWrapper#setShowEmpty(boolean, boolean)
	 */
	@Override
	public void setShowEmpty(boolean isShowEmpty, boolean recursive) {
		// TODO Auto-generated method stub
		
	}
	
	protected boolean isVisible(ItemVisibilityHandler visibilityHandler) {
		if (visibilityHandler != null) {
			ItemVisibility visible = visibilityHandler.isVisible(this);
			if (visible != null) {
				switch (visible) {
					case VISIBLE:
						return true;
					case HIDDEN:
						return false;
					default:
						// automatic, go on ...
				}
			}
		}
	    return isVisible();
	}
	
	@Override
	public boolean isVisible() {
		
		ID def = getItemDefinition();
		switch (getItemStatus()) {
			case NOT_CHANGED:
				return isNotEmptyAndCanReadAndModify(def) || showEmptyCanReadAndModify(def);
			case ADDED:
				return emphasizedAndCanAdd(def) || showEmptyAndCanAdd(def);
			case DELETED:
				return false;
		}
		
		return false;
	}
	
	private boolean isNotEmptyAndCanReadAndModify(ID def) {
		return def.canRead(); // && def.canModify();
	}

	private boolean showEmptyCanReadAndModify(ID def) {
		return def.canRead() && isShowEmpty();// && def.canModify() && isShowEmpty();
	}

	private boolean showEmptyAndCanAdd(ID def) {
		return def.canAdd() && isShowEmpty();
	}

	private boolean emphasizedAndCanAdd(ID def) {
		return def.canAdd() && def.isEmphasized();
	}
	
	ID getItemDefinition() {
		return item.getDefinition();
	}

	boolean isEmpty() {
		return item.isEmpty();
	}
	
	ItemStatus getItemStatus() {
		return status;
	}
	
}
