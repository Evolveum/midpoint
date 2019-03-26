/*
 *  Copyright (c) 2010-2018 Evolveum
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

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.Validate;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemWrapperOld;
import com.evolveum.midpoint.gui.impl.component.prism.PrismPropertyPanel;
import com.evolveum.midpoint.gui.impl.prism.ContainerWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.ObjectWrapperOld;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.ItemProcessing;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.form.ValueChoosePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FormItemServerValidationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FormItemValidationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemRefinedDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;

/**
 * Extracts common functionality of PropertyWrapper and ReferenceWrapper.
 * TODO consider unifying with ContainerWrapper as well.
 *
 * @author mederly
 */
public abstract class PropertyOrReferenceWrapper<I extends Item<? extends PrismValue, ID>, ID extends ItemDefinition<I>> implements ItemWrapperOld, Serializable {

	private static final long serialVersionUID = -179218652752175177L;

	@Nullable protected ContainerValueWrapper container;
	protected I item;
	protected ValueStatus status;
	protected List<ValueWrapperOld> values;
	
	// processed and localized display name
	protected String displayName;
	
	protected boolean readonly;
	private boolean isStripe;
	private boolean showEmpty;
	private ItemPath path;

	protected transient PrismContext prismContext;

	public PropertyOrReferenceWrapper(@Nullable ContainerValueWrapper containerValue, I item, boolean readonly,
			ValueStatus status, ItemPath path, PrismContext prismContext) {
		Validate.notNull(item, "Item must not be null.");
		Validate.notNull(status, "Item status must not be null.");

		this.container = containerValue;
		this.item = item;
		this.status = status;
		this.readonly = readonly;
		this.path = path;
		this.prismContext = prismContext;
	}

	/**
	 * @return the status
	 */
	public ValueStatus getStatus() {
		return status;
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
		this.prismContext = prismContext;
	}

	@Override
	public ID getItemDefinition() {
		return item.getDefinition();
		
	}
	
//	@Override
//	@Nullable
//	public ContainerWrapperImpl getParent() {
//		return container != null ? container.getContainer() : null;
//	}
//
//	public boolean isVisible() {
//		
//        if (!isVisibleOnBasisOfModel()) {
//			return false;
//		} 
//
//        if (container == null) {
//        	return false;           // TODO: ok ?
//        }
//        switch (container.getObjectStatus()) {
//        	case ADDING : 
//        		return canAddDefault() || canAddAndShowEmpty();
//        	case MODIFYING :
//        		return canReadOrModifyAndNonEmpty() || canReadOrModifyAndShowEmpty();
//        }
////        if (getItem().isEmpty() && isS)
////        else if (container != null) {
////			return container.isItemVisible(this);
////		} else {
//			return true;
////		}
//	}
	
	private boolean isVisibleOnBasisOfModel() {
//		
//		if (getItemDefinition().isOperational() && !isMetadataContainer()) {			// TODO ...or use itemDefinition instead?
//			return false;
//		} 
        
        if (getItemDefinition().isDeprecated() && isEmpty()) {
        	return false;
        }
        return true;
	}
	
	public boolean isOnlyHide() {
		
		if (!isVisibleOnBasisOfModel()) {
			return false;
		} 
		
		if (container == null) {
        	return false;
        }
		
		switch (container.getObjectStatus()) {
    		case ADDING : 
    			return getItemDefinition().canAdd() && !isShowEmpty();
    		case MODIFYING :
    			return getItemDefinition().canRead() && !isShowEmpty();
    		default : return false;
		}
	}
	
	@Override
	public ItemProcessing getProcessing() {
		return getItemDefinition().getProcessing();
	}
	
	private boolean canAddAndShowEmpty() {
		return getItemDefinition().canAdd() && isShowEmpty();
	}
	
	private boolean canAddDefault() {
		return getItemDefinition().canAdd() && (getItemDefinition().isEmphasized() || getItemDefinition().getMinOccurs() == 1);
	}
	
	private boolean canReadOrModifyAndNonEmpty() {
		return getItemDefinition().canRead() && (!getItem().isEmpty() || getItemDefinition().isEmphasized() || getItemDefinition().getMinOccurs() == 1); //(getItemDefinition().canModify() || getItemDefinition().canRead()) && !getItem().isEmpty();
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
//	public String getDisplayName() {
//		if (displayName == null) {
//			// Lazy loading of a localized name.
//			// We really want to remember a processed name in the wrapper.
//			// getDisplatName() method may be called many times, e.g. during sorting.
//			displayName = ContainerWrapperImpl.getDisplayNameFromItem(item);
//		}
//		return displayName;
//	}
	
//	@Override
//	public void setDisplayName(String displayName) {
//		this.displayName = ContainerWrapperImpl.localizeName(displayName);
//	}

//	public ValueStatus getStatus() {
//		return status;
//	}
//
//	public void setStatus(ValueStatus status) {
//		this.status = status;
//	}

	public List<ValueWrapperOld> getValues() {
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
	
	@Override
	public boolean isRequired() {
		return getItemDefinition().isMandatory() && isEnforceRequiredFields();
	}
	
//	@Override
//	public void removeValue(ValueWrapperOld<ValueWrapperOld> valueWrapper) throws SchemaException {
//		List<ValueWrapperOld> values = getValues();
//		
//		switch (valueWrapper.getStatus()) {
//			case ADDED:
//				values.remove(valueWrapper);
//				break;
//			case DELETED:
//				throw new SchemaException();
//			case NOT_CHANGED:
//				valueWrapper.setStatus(ValueStatus.DELETED);
//				break;
//		}
//		
//		int count = countUsableValues(this);
//		
//		if (count == 0 && !hasEmptyPlaceholder(this)) {
//			addValue(true);
//			
//		}
//	}
	
	
	
	public abstract ValueWrapperOld createAddedValue();

	@Override
	public I getItem() {
		return item;
	}

//	public ItemDefinition getDefinition() {
//		return item.getDefinition();
//	}

	public boolean hasChanged() {
		for (ValueWrapperOld value : getValues()) {
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
//	private boolean isMetadataContainer() {
//		ContainerWrapperImpl parent = getParent();
//		if (parent == null) {
//			return false;
//		}
//		ItemDefinition<?> definition = parent.getItemDefinition();
//		if (definition == null) {
//			return false;
//		}
//		return definition.getTypeName().equals(MetadataType.COMPLEX_TYPE);
//	}

	@Override
	public boolean isReadonly() {
		//TODO this is probably not good idea
//		if (isMetadataContainer()) {
//			return true;
//		}
//		
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
		for (ValueWrapperOld valueWrapper : getValues()) {
			valueWrapper.normalize(prismContext);
			if (ValueStatus.DELETED.equals(valueWrapper.getStatus())) {
				updatedItem.remove(valueWrapper.getValue());
			} else if (!updatedItem.containsEquivalentValue(valueWrapper.getValue())) {
				PrismValue cloned = ObjectWrapperOld.clone(valueWrapper.getValue());
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
		for (ValueWrapperOld valueWrapper : CollectionUtils.emptyIfNull(getValues())) {
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
	public boolean isExperimental() {
		return getItemDefinition().isExperimental();
	}
	
	@Override
	public String getDeprecatedSince() {
		return getItemDefinition().getDeprecatedSince();
	}
	
//	@Override
//	public ExpressionType getFormItemValidator() {
//		FormItemValidationType formItemValidation = item.getDefinition().getAnnotation(ItemRefinedDefinitionType.F_VALIDATION);
//		if (formItemValidation == null) {
//			return null;
//		}
//		
//		List<FormItemServerValidationType> serverValidators = formItemValidation.getServer();
//		if (CollectionUtils.isNotEmpty(serverValidators)) {
//			return serverValidators.iterator().next().getExpression();
//		}
//		
//		return null;
//	}
	
	public Panel createPanel(String id, Form form, ItemVisibilityHandlerOld visibilityHandler) {
		PrismPropertyPanel<?> propertyPanel = new PrismPropertyPanel<>(id, this, form, visibilityHandler);
		propertyPanel.setOutputMarkupId(true);
		propertyPanel.add(new VisibleEnableBehaviour() {
			
			private static final long serialVersionUID = 1L;
			
			@Override
			public boolean isVisible() {
				if(!container.isExpanded()) {
					return false;
				}
				return propertyPanel.isVisible();
			}
		});
		
		return propertyPanel;
	}
}
