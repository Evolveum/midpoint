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
import java.util.Map;
import java.util.function.Consumer;

import javax.xml.namespace.QName;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.ItemWrapperOld;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.ItemProcessing;
import com.evolveum.midpoint.prism.MutableItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.prism.ItemVisibilityHandler;
import com.evolveum.midpoint.web.component.prism.PropertyOrReferenceWrapper;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.prism.ValueWrapperOld;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FormItemServerValidationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FormItemValidationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemRefinedDefinitionType;

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
	
	private List<PrismValueWrapper<PV>> values;
	
	//consider
	private boolean readOnly;
	
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

//	@Override
//	public boolean hasOutboundMapping() {
//		ID def = getItemDefinition();
//		
//		if (def instanceof RefinedAttributeDefinition) {
//			return ((RefinedAttributeDefinition) def).hasOutboundMapping();
//		}
//		
//		return false;
//	}
	
	@Override
	public boolean isMultiValue() {
		return getItemDefinition().isMultiValue();
	}
	
	@Override
	public boolean isReadOnly() {
		return readOnly;
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
		return item.getDefinition();
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
		// TODO Auto-generated method stub
		return null;
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

	
	@Override
	public List<PrismValueWrapper<PV>> getValues() {
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
	
//	ID getItemDefinition() {
//		return item.getDefinition();
//	}

	boolean isEmpty() {
		return item.isEmpty();
	}
	
	ItemStatus getItemStatus() {
		return status;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.ItemDefinition#getName()
	 */
	@Override
	public ItemName getName() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.ItemDefinition#getNamespace()
	 */
	@Override
	public String getNamespace() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.ItemDefinition#getMinOccurs()
	 */
	@Override
	public int getMinOccurs() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.ItemDefinition#getMaxOccurs()
	 */
	@Override
	public int getMaxOccurs() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.ItemDefinition#isSingleValue()
	 */
	@Override
	public boolean isSingleValue() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.ItemDefinition#isOptional()
	 */
	@Override
	public boolean isOptional() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.ItemDefinition#isOperational()
	 */
	@Override
	public boolean isOperational() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.ItemDefinition#isInherited()
	 */
	@Override
	public boolean isInherited() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.ItemDefinition#isDynamic()
	 */
	@Override
	public boolean isDynamic() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.ItemDefinition#canRead()
	 */
	@Override
	public boolean canRead() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.ItemDefinition#canModify()
	 */
	@Override
	public boolean canModify() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.ItemDefinition#canAdd()
	 */
	@Override
	public boolean canAdd() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.ItemDefinition#getSubstitutionHead()
	 */
	@Override
	public QName getSubstitutionHead() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.ItemDefinition#isHeterogeneousListItem()
	 */
	@Override
	public boolean isHeterogeneousListItem() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.ItemDefinition#getValueEnumerationRef()
	 */
	@Override
	public PrismReferenceValue getValueEnumerationRef() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.ItemDefinition#isValidFor(javax.xml.namespace.QName, java.lang.Class)
	 */
	@Override
	public boolean isValidFor(QName elementQName, Class<? extends ItemDefinition> clazz) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.ItemDefinition#isValidFor(javax.xml.namespace.QName, java.lang.Class, boolean)
	 */
	@Override
	public boolean isValidFor(QName elementQName, Class<? extends ItemDefinition> clazz, boolean caseInsensitive) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.ItemDefinition#adoptElementDefinitionFrom(com.evolveum.midpoint.prism.ItemDefinition)
	 */
	@Override
	public void adoptElementDefinitionFrom(ItemDefinition otherDef) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.ItemDefinition#instantiate()
	 */
	@Override
	public I instantiate() throws SchemaException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.ItemDefinition#instantiate(javax.xml.namespace.QName)
	 */
	@Override
	public I instantiate(QName name) throws SchemaException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.ItemDefinition#findItemDefinition(com.evolveum.midpoint.prism.path.ItemPath, java.lang.Class)
	 */
	@Override
	public <T extends ItemDefinition> T findItemDefinition(ItemPath path, Class<T> clazz) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.ItemDefinition#createEmptyDelta(com.evolveum.midpoint.prism.path.ItemPath)
	 */
	@Override
	public ItemDelta createEmptyDelta(ItemPath path) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.ItemDefinition#clone()
	 */
	@Override
	public ItemDefinition<I> clone() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.ItemDefinition#deepClone(boolean, java.util.function.Consumer)
	 */
	@Override
	public ItemDefinition<I> deepClone(boolean ultraDeep, Consumer<ItemDefinition> postCloneAction) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.ItemDefinition#deepClone(java.util.Map, java.util.Map, java.util.function.Consumer)
	 */
	@Override
	public ItemDefinition<I> deepClone(Map<QName, ComplexTypeDefinition> ctdMap, Map<QName, ComplexTypeDefinition> onThisPath,
			Consumer<ItemDefinition> postCloneAction) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.ItemDefinition#revive(com.evolveum.midpoint.prism.PrismContext)
	 */
	@Override
	public void revive(PrismContext prismContext) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.ItemDefinition#debugDumpShortToString(java.lang.StringBuilder)
	 */
	@Override
	public void debugDumpShortToString(StringBuilder sb) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.ItemDefinition#canBeDefinitionOf(com.evolveum.midpoint.prism.Item)
	 */
	@Override
	public boolean canBeDefinitionOf(I item) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.ItemDefinition#canBeDefinitionOf(com.evolveum.midpoint.prism.PrismValue)
	 */
	@Override
	public boolean canBeDefinitionOf(PrismValue pvalue) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.ItemDefinition#toMutable()
	 */
	@Override
	public MutableItemDefinition<I> toMutable() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.Definition#getTypeName()
	 */
	@Override
	public QName getTypeName() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.Definition#isRuntimeSchema()
	 */
	@Override
	public boolean isRuntimeSchema() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.Definition#isIgnored()
	 */
	@Override
	public boolean isIgnored() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.Definition#getProcessing()
	 */
	@Override
	public ItemProcessing getProcessing() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.Definition#isAbstract()
	 */
	@Override
	public boolean isAbstract() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.Definition#getPlannedRemoval()
	 */
	@Override
	public String getPlannedRemoval() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.Definition#isElaborate()
	 */
	@Override
	public boolean isElaborate() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.Definition#isEmphasized()
	 */
	@Override
	public boolean isEmphasized() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.Definition#getDisplayOrder()
	 */
	@Override
	public Integer getDisplayOrder() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.Definition#getDocumentation()
	 */
	@Override
	public String getDocumentation() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.Definition#getDocumentationPreview()
	 */
	@Override
	public String getDocumentationPreview() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.Definition#getPrismContext()
	 */
	@Override
	public PrismContext getPrismContext() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.Definition#getTypeClassIfKnown()
	 */
	@Override
	public Class getTypeClassIfKnown() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.Definition#getTypeClass()
	 */
	@Override
	public Class getTypeClass() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.Definition#getAnnotation(javax.xml.namespace.QName)
	 */
	@Override
	public <A> A getAnnotation(QName qname) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.Definition#setAnnotation(javax.xml.namespace.QName, java.lang.Object)
	 */
	@Override
	public <A> void setAnnotation(QName qname, A value) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.prism.Visitable#accept(com.evolveum.midpoint.prism.Visitor)
	 */
	@Override
	public void accept(Visitor visitor) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.api.prism.ItemWrapper#removeValue(com.evolveum.midpoint.web.component.prism.ValueWrapperOld)
	 */
	@Override
	public void removeValue(ValueWrapperOld<PV> valueWrapper) throws SchemaException {
		// TODO Auto-generated method stub
		
	}
	
}
