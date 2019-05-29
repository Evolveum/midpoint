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
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
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
	
	private static final transient Trace LOGGER = TraceManager.getTrace(ItemWrapperImpl.class);
	
	private PrismContainerValueWrapper<?> parent;
	
	private ItemStatus status = null;
	
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
		
	}
	
	protected <D extends ItemDelta<PV, ID>, O extends ObjectType> D getItemDelta(Class<O> objectClass, Class<D> deltaClass) throws SchemaException {
		D delta = (D) createEmptyDelta(null);
		for (VW value : values) {
			value.addToDelta(delta);
		}
		
		if (delta.isEmpty()) {
			return null;
		}
		return delta;
	}
	
	@Override
	public <D extends ItemDelta<PV, ID>> Collection<D> getDelta() throws SchemaException {
		LOGGER.trace("Start computing delta for {}", newItem);
		D delta = null;
		if (parent != null && ValueStatus.ADDED == parent.getStatus()) {
			delta = (D) createEmptyDelta(getName()); 
		} else {
			delta = (D) createEmptyDelta(getPath());
		}
		
		for (VW value : values) {
			value.addToDelta(delta);
		}
		
		if (delta.isEmpty()) {
			LOGGER.trace("There is no delta for {}", newItem);
			return null;
		}
		
		LOGGER.trace("Returning delta {}", delta);
		return MiscUtil.createCollection(delta);
	}

	
	@Override
	public <D extends ItemDelta<PV, ID>> void applyDelta(D delta) throws SchemaException {
		if (delta == null) {
			return;
		}
		
		LOGGER.trace("Applying {} to {}", delta, newItem);
		delta.applyTo(newItem);
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


	@Override
	public <O extends ObjectType> ItemStatus findObjectStatus() {
		if (parent == null) {
			return status;
		}
		
		ItemWrapper parentWrapper = parent.getParent();
		
		PrismObjectWrapper<O> objectWrapper = findObjectWrapper(parentWrapper);
		if (objectWrapper == null) {
			return status;
		}
		
		return objectWrapper.getStatus();
	}
	
	@Override
	public <OW extends PrismObjectWrapper<O>, O extends ObjectType> OW findObjectWrapper() {
		if (parent == null) {
			return null;
		}
		
		ItemWrapper parentWrapper = parent.getParent();
		
		return findObjectWrapper(parentWrapper);
		
	}
	
	private <OW extends PrismObjectWrapper<O>, O extends ObjectType> OW findObjectWrapper(ItemWrapper parent) {
		if (parent != null) {
			if (parent instanceof PrismObjectWrapper) {
				return (OW) parent;
			}
			if (parent.getParent() != null) {
				return findObjectWrapper(parent.getParent().getParent());
			}
		} 
		return null;
		
	}
	
	
	@Override
	public List<VW> getValues() {
		return values;
	}
	
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
	
	@Override
	public boolean isVisible(ItemVisibilityHandler visibilityHandler) {
		
		if (!isVisibleByVisibilityHandler(visibilityHandler)) {
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
	
	protected boolean isVisibleByVisibilityHandler(ItemVisibilityHandler visibilityHandler) {
		if (getParent() != null && !getParent().isExpanded()) {
			return false;
		}
		
		
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
		
		return true;
	    
	}
	
	private boolean isVisibleForModify(ID def) {
		if (parent != null && parent.isShowEmpty()) {
			return def.canRead();
		}
		
		return def.canRead() && (def.isEmphasized() || !isEmpty());
	}
	
	private boolean isVisibleForAdd(ID def) {
		if (parent != null && parent.isShowEmpty()) {
			return def.canAdd();
		}
		
		return def.isEmphasized() && def.canAdd();
	}
	
	protected boolean isEmpty() {
		if (newItem.isEmpty()) {
			return true;
		}
		
		return false;
	}
	
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
	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	@Override
	public boolean isStripe() {
		return stripe;
	}
	
	@Override
	public void setStripe(boolean stripe) {
		this.stripe = stripe;
	}
	
	protected I getOldItem() {
		return oldItem;
	}
	
}
