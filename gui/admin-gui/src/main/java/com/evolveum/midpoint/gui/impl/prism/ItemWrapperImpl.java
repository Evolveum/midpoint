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
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.xml.namespace.QName;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.ItemProcessing;
import com.evolveum.midpoint.prism.MutableItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.prism.ItemVisibilityHandlerOld;
import com.evolveum.midpoint.web.component.prism.ValueWrapperOld;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FormItemServerValidationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FormItemValidationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemRefinedDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author katka
 *
 */
public abstract class ItemWrapperImpl<PV extends PrismValue, I extends Item<PV, ID>, ID extends ItemDefinition<I>, VW extends PrismValueWrapper> implements ItemWrapper<PV, I, ID, VW>, Serializable {

	private static final long serialVersionUID = 1L;
	
	private PrismContainerValueWrapper<?> parent;
	
	private ItemStatus status = null;
	private ItemPath fullPath = null;
//	private PrismContext prismContext = null;

	private String displayName;
	
	private List<VW> values = new ArrayList<>();
	
	private I oldItem;
	private I newItem;
	
	private boolean column;
	
	private boolean stripe;
	
	private boolean showEmpty;
	
	
	//consider
	private boolean readOnly;
	
	public ItemWrapperImpl(@Nullable PrismContainerValueWrapper<?> parent, I item, ItemStatus status) {
		Validate.notNull(item, "Item must not be null.");
		Validate.notNull(status, "Item status must not be null.");

		this.parent = parent;
		this.newItem = item;
		this.oldItem = (I) item.clone();
		this.status = status;
//		this.readonly = readonly;
		
	}
	
	protected <D extends ItemDelta<PV, ID>, O extends ObjectType> D getItemDelta(Class<O> objectClass, Class<D> deltaClass) throws SchemaException {
//		D delta = (D) getPrismContext().deltaFor(objectClass).asItemDelta();
		D delta = (D) createEmptyDelta(getPath());
		for (VW value : values) {
			value.addToDelta(delta);
		}
		
		if (delta.isEmpty()) {
			return null;
		}
		return delta;
	}
	
	@Override
	public <D extends ItemDelta<PV, ID>> D getDelta() {
		D delta = (D) createEmptyDelta(getPath());
		for (VW value : values) {
			value.addToDelta(delta);
		}
		
		if (delta.isEmpty()) {
			return null;
		}
		return delta;
	}
	
	@Override
	public String getDisplayName() {
		if (displayName == null) {
			displayName = getLocalizedDisplayName();
		}
		
		return displayName;
	}
	
	

	@Override
	public String getHelp() {
		return WebPrismUtil.getHelpText(getItemDefinition());
	}

		@Override
	public boolean isExperimental() {
		return getItemDefinition().isExperimental();
	}

	@Override
	public String getDeprecatedSince() {
		return getItemDefinition().getDeprecatedSince();
	}

	@Override
	public boolean isDeprecated() {
		return getItemDefinition().isDeprecated();
	}

	@Override
	public boolean isMandatory() {
		return getItemDefinition().isMandatory();
	}
	
	public ItemStatus getStatus() {
		return status;
	}

	@Override
	public I getItem() {
		return newItem;
	}
	
	@Override
	public void setColumn(boolean column) {
		this.column = column;
	}
	
	@Override
	public boolean isColumn() {
		return column;
	}
	
	public PrismContainerValueWrapper<?> getParent() {
		return parent;
	}
	
	@Override
	public boolean isMultiValue() {
		return getItemDefinition().isMultiValue();
	}
	
	@Override
	public boolean isReadOnly() {
		return readOnly;
	}
	
	@Override
	public ItemPath getPath() {
		return newItem.getPath();
	}
	
	@Override
	public ExpressionType getFormComponentValidator() {
		FormItemValidationType formItemValidation = getItemDefinition().getAnnotation(ItemRefinedDefinitionType.F_VALIDATION);
		if (formItemValidation == null) {
			return null;
		}
		
		List<FormItemServerValidationType> serverValidators = formItemValidation.getServer();
		if (CollectionUtils.isNotEmpty(serverValidators)) {
			return serverValidators.iterator().next().getExpression();
		}
		
		return null;
	}
	
	ID getItemDefinition() {
		return newItem.getDefinition();
	}
	//OLD

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.Revivable#revive(com.evolveum.midpoint.prism.PrismContext)
	 */
//	@Override
//	public void revive(PrismContext prismContext) throws SchemaException {
//		if (item != null) {
//			item.revive(prismContext);
//		}
//		if (item.getDefinition() != null) {
//			item.getDefinition().revive(prismContext);
//		}
//		this.prismContext = prismContext;
//
//	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.DebugDumpable#debugDump(int)
	 */
	@Override
	public String debugDump(int indent) {
		StringBuilder sb = DebugUtil.createIndentedStringBuilder(indent);
		sb.append(toString());
		sb.append("Original definition: ").append(newItem.getDefinition()).append("\n");
		sb.append("Display nam: ").append(displayName).append("\n");
		sb.append("Item status: ").append(status).append("\n");
		sb.append("Read only: ").append(isReadOnly()).append("\n");
		sb.append("New item: \n").append(newItem).append("\n");
		sb.append("Old item: \n").append(oldItem).append("\n");
		sb.append("Values: \n");
		for (VW value : values) {
			sb.append(value.debugDump());
		}
		return sb.toString();
		
	}
	
	private String getLocalizedDisplayName() {
		Validate.notNull(newItem, "Item must not be null.");

		String displayName = newItem.getDisplayName();
		if (!StringUtils.isEmpty(displayName)) {
			return localizeName(displayName);
		}

		QName name = newItem.getElementName();
		if (name != null) {
			displayName = name.getLocalPart();

			PrismValue val = newItem.getParent();
			if (val != null && val.getTypeName() != null) {
				displayName = val.getTypeName().getLocalPart() + "." + displayName;
			}
		} else {
			displayName = newItem.getDefinition().getTypeName().getLocalPart();
		}

		return localizeName(displayName);
	}
	
	private String localizeName(String nameKey) {
		Validate.notNull(nameKey, "Null localization key");
		return ColumnUtils.createStringResource(nameKey).getString();
	}


	private <IW extends ItemWrapper> ItemStatus findObjectStatus() {
		if (parent == null) {
			return status;
		}
		
		IW parentWrapper = parent.getParent();
		
		return findObjectStatus(parentWrapper);
	}
	
	private <IW extends ItemWrapper> ItemStatus findObjectStatus(IW parent) {
		if (parent != null) {
			if (parent instanceof PrismObjectWrapper) {
				return ((PrismObjectWrapper) parent).getStatus();
			}
			if (parent.getParent() != null) {
				return findObjectStatus(parent.getParent().getParent());
			}
		} 
		return status;
		
	}

	
	@Override
	public boolean hasChanged() {
		// TODO Auto-generated method stub
		return false;
	}

	
	@Override
	public List<VW> getValues() {
		return values;
	}
	
	
//	@Override
//	public void removeValue(ValueWrapperOld<PV> valueWrapper) throws SchemaException {
//		List<ValueWrapperOld<PV>> values = getValues();
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
//		int count = countUsableValues();
//		
//		if (count == 0 && !hasEmptyPlaceholder()) {
//			addValue(true);
//			
//		}
//	}
//	
//	private int countUsableValues() {
//		int count = 0;
//		for (ValueWrapperOld<PV> value : getValues()) {
//			value.normalize(prismContext);
//
//			if (ValueStatus.DELETED.equals(value.getStatus())) {
//				continue;
//			}
//
//			if (ValueStatus.ADDED.equals(value.getStatus()) && !value.hasValueChanged()) {
//				continue;
//			}
//
//			count++;
//		}
//		return count;
//	}
//	
//	private boolean hasEmptyPlaceholder() {
//		for (ValueWrapperOld<PV> value : getValues()) {
//			value.normalize(prismContext);
//			if (ValueStatus.ADDED.equals(value.getStatus()) && !value.hasValueChanged()) {
//				return true;
//			}
//		}
//
//		return false;
//	}


	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.api.prism.ItemWrapper#checkRequired(com.evolveum.midpoint.gui.api.page.PageBase)
	 */
	@Override
	public boolean checkRequired(PageBase pageBase) {
		return newItem.getDefinition().isMandatory();
	}

	@Override
	public boolean isShowEmpty() {
		return showEmpty;
	}

	@Override
	public void setShowEmpty(boolean isShowEmpty, boolean recursive) {
		this.showEmpty = isShowEmpty;
	}
	
//	protected boolean isVisible(ItemVisibilityHandler visibilityHandler) {
//		if (visibilityHandler != null) {
//			ItemVisibility visible = visibilityHandler.isVisible(this);
//			if (visible != null) {
//				switch (visible) {
//					case VISIBLE:
//						return true;
//					case HIDDEN:
//						return false;
//					default:
//						// automatic, go on ...
//				}
//			}
//		}
//	    return isVisible();
//	}
	
	@Override
	public boolean isVisible() {
		
		if (!getParent().isExpanded()) {
			return false;
		}
		
		ID def = getItemDefinition();
		switch (findObjectStatus()) {
			case NOT_CHANGED:
				return isVisibleForModify(def);
			case ADDED:
				return isVisibleForAdd(def);
			case DELETED:
				return false;
		}
		
		return false;
	}
	
	private boolean isVisibleForModify(ID def) {
		if (parent.isShowEmpty()) {
			return def.canRead();
		}
		
		return def.canRead() && (def.isEmphasized() || !newItem.isEmpty());
	}
	
	private boolean isVisibleForAdd(ID def) {
		if (parent.isShowEmpty()) {
			return def.canAdd();
		}
		
		return def.isEmphasized() && def.canAdd();
	}

//	private boolean showEmptyCanReadAndModify(ID def) {
//		return def.canRead() && isShowEmpty();// && def.canModify() && isShowEmpty();
//	}
//
//	private boolean showEmptyAndCanAdd(ID def) {
//		return def.canAdd() && isShowEmpty();
//	}
//
//	private boolean emphasizedAndCanAdd(ID def) {
//		return def.canAdd() && def.isEmphasized();
//	}
	
	ItemStatus getItemStatus() {
		return status;
	}
	

	@Override
	public ItemName getName() {
		return getItemDefinition().getName();
	}

	@Override
	public String getNamespace() {
		return getItemDefinition().getNamespace();
	}

	@Override
	public int getMinOccurs() {
		return getItemDefinition().getMinOccurs();
	}

	@Override
	public int getMaxOccurs() {
		return getItemDefinition().getMaxOccurs();
	}

	@Override
	public boolean isSingleValue() {
		return getItemDefinition().isSingleValue();
	}

	@Override
	public boolean isOptional() {
		return getItemDefinition().isOptional();
	}

	@Override
	public boolean isOperational() {
		return getItemDefinition().isOperational();
	}

	@Override
	public boolean isInherited() {
		return getItemDefinition().isInherited();
	}

	@Override
	public boolean isDynamic() {
		return getItemDefinition().isDynamic();
	}

	@Override
	public boolean canRead() {
		return getItemDefinition().canRead();
	}

	@Override
	public boolean canModify() {
		return getItemDefinition().canModify();
	}

	@Override
	public boolean canAdd() {
		return getItemDefinition().canAdd();
	}

	@Override
	public QName getSubstitutionHead() {
		return getItemDefinition().getSubstitutionHead();
	}

	@Override
	public boolean isHeterogeneousListItem() {
		return getItemDefinition().isHeterogeneousListItem();
	}

	@Override
	public PrismReferenceValue getValueEnumerationRef() {
		return getItemDefinition().getValueEnumerationRef();
	}

	@Override
	public boolean isValidFor(QName elementQName, Class<? extends ItemDefinition> clazz) {
		return getItemDefinition().isValidFor(elementQName, clazz);
	}

	@Override
	public boolean isValidFor(QName elementQName, Class<? extends ItemDefinition> clazz, boolean caseInsensitive) {
		return getItemDefinition().isValidFor(elementQName, clazz, caseInsensitive);
	}

	@Override
	public void adoptElementDefinitionFrom(ItemDefinition otherDef) {
		getItemDefinition().adoptElementDefinitionFrom(otherDef);
	}

	@Override
	public I instantiate() throws SchemaException {
		return getItemDefinition().instantiate();
	}

	@Override
	public I instantiate(QName name) throws SchemaException {
		return getItemDefinition().instantiate();
	}

	@Override
	public <T extends ItemDefinition> T findItemDefinition(ItemPath path, Class<T> clazz) {
		return getItemDefinition().findItemDefinition(path, clazz);
	}

	@Override
	public ItemDelta createEmptyDelta(ItemPath path) {
		return getItemDefinition().createEmptyDelta(path);
	}

	@Override
	public ItemDefinition<I> clone() {
		return getItemDefinition().clone();
	}

	@Override
	public ItemDefinition<I> deepClone(boolean ultraDeep, Consumer<ItemDefinition> postCloneAction) {
		return getItemDefinition().deepClone(ultraDeep, postCloneAction);
	}

	@Override
	public ItemDefinition<I> deepClone(Map<QName, ComplexTypeDefinition> ctdMap, Map<QName, ComplexTypeDefinition> onThisPath,
			Consumer<ItemDefinition> postCloneAction) {
		return getItemDefinition().deepClone(ctdMap, onThisPath, postCloneAction);
	}

	@Override
	public void revive(PrismContext prismContext) {
		getItemDefinition().revive(prismContext);
	}

	@Override
	public void debugDumpShortToString(StringBuilder sb) {
		//TODO implement for wrappers
		getItemDefinition().debugDumpShortToString(sb);
	}

	@Override
	public boolean canBeDefinitionOf(I item) {
		return getItemDefinition().canBeDefinitionOf(item);
	}

	@Override
	public boolean canBeDefinitionOf(PrismValue pvalue) {
		return getItemDefinition().canBeDefinitionOf(pvalue);
	}

	@Override
	public MutableItemDefinition<I> toMutable() {
		return getItemDefinition().toMutable();
	}

	@Override
	public QName getTypeName() {
		return getItemDefinition().getTypeName();
	}

	@Override
	public boolean isRuntimeSchema() {
		return getItemDefinition().isRuntimeSchema();
	}

	@Override
	@Deprecated
	public boolean isIgnored() {
		return getItemDefinition().isIgnored();
	}

	@Override
	public ItemProcessing getProcessing() {
		return getItemDefinition().getProcessing();
	}

	@Override
	public boolean isAbstract() {
		return getItemDefinition().isAbstract();
	}

	@Override
	public String getPlannedRemoval() {
		return getItemDefinition().getPlannedRemoval();
	}

	@Override
	public boolean isElaborate() {
		return getItemDefinition().isElaborate();
	}

	@Override
	public boolean isEmphasized() {
		return getItemDefinition().isEmphasized();
	}

	@Override
	public Integer getDisplayOrder() {
		return getItemDefinition().getDisplayOrder();
	}

	@Override
	public String getDocumentation() {
		return getItemDefinition().getDocumentation();
	}

	@Override
	public String getDocumentationPreview() {
		return getItemDefinition().getDocumentationPreview();
	}

	@Override
	public PrismContext getPrismContext() {
		return getItemDefinition().getPrismContext();
	}

	@Override
	public Class getTypeClassIfKnown() {
		return getItemDefinition().getTypeClassIfKnown();
	}

	@Override
	public Class getTypeClass() {
		return getItemDefinition().getTypeClass();
	}

	@Override
	public <A> A getAnnotation(QName qname) {
		return getItemDefinition().getAnnotation(qname);
	}

	@Override
	public <A> void setAnnotation(QName qname, A value) {
		getItemDefinition().setAnnotation(qname, value);
	}

	@Override
	public void accept(Visitor visitor) {
		getItemDefinition().accept(visitor);
	}

	
	@Override
	public void setReadOnly() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isStripe() {
		return stripe;
	}
	
	@Override
	public void setStripe(boolean stripe) {
		this.stripe = stripe;
	}
	
}
