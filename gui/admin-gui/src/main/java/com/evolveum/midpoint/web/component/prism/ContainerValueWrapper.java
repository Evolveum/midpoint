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

package com.evolveum.midpoint.web.component.prism;

import java.io.Serializable;
import java.text.Collator;
import java.util.*;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author lazyman
 * @author katkav
 */
public class ContainerValueWrapper<C extends Containerable> extends PrismWrapper implements Serializable, DebugDumpable {

	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(ContainerValueWrapper.class);

	private ContainerWrapper<C> containerWrapper;
	private PrismContainerValue<C> containerValue;
	private ValueStatus status;
	private ItemPath path;
	// WARNING!!! This field has to be called "properties" even if the right name should be "items".
	// But some Wicket reflection magic is looking for "properties" field. So, let it be properties for now.
	private List<ItemWrapper> properties;
	private boolean readonly;
	private boolean selected;

	private ContainerStatus objectStatus;
	
	ContainerValueWrapper(ContainerWrapper<C> containerWrapper, PrismContainerValue<C> containerValue, ContainerStatus objectStatus, ValueStatus status,
			ItemPath path) {
		Validate.notNull(status, "Container status must not be null.");
		Validate.notNull(containerValue.getParent().getDefinition(), "container definition must not be null.");
		Validate.notNull(status, "Container status must not be null.");

		this.containerWrapper = containerWrapper;
		this.containerValue = containerValue;
		this.objectStatus = objectStatus;
		this.status = status;
		this.path = path;
	}

	ContainerValueWrapper(PrismContainerValue<C> containerValue, ContainerStatus objectStatus, ValueStatus status, ItemPath path, boolean readOnly) {
		this(null, containerValue, objectStatus, status, path);
		this.readonly = readOnly;
	}

	public PrismContainerDefinition<C> getDefinition() {
		return containerValue.getParent().getDefinition();
	}

	public void revive(PrismContext prismContext) throws SchemaException {
		if (containerValue != null) {
			containerValue.revive(prismContext);
		}
		if (getDefinition() != null) {
			getDefinition().revive(prismContext);
		}
		if (properties != null) {
			for (ItemWrapper itemWrapper : properties) {
				itemWrapper.revive(prismContext);
			}
		}
	}

	@Nullable
	ContainerWrapper<C> getContainer() {
		return containerWrapper;
	}

	public ValueStatus getStatus() {
		return status;
	}

	public void setStatus(ValueStatus status){
		this.status = status;
	}

	public ItemPath getPath() {
		return path;
	}

	public PrismContainerValue<C> getContainerValue() {
		return containerValue;
	}

	public List<ItemWrapper> getItems() {
		if (properties == null) {
			properties = new ArrayList<>();
		}
		return properties;
	}

	public void addEmptyProperties(List<ItemWrapper> emptyProperties) {
		emptyProperties.forEach(empty -> {
			if (!properties.contains(empty))
				properties.add(empty);
		});
	}

	public void setProperties(List<ItemWrapper> properties) {
		this.properties = properties;
	}

	public void computeStripes() {
		if (properties == null) {
			return;
		}
		int visibleProperties = 0;

 		for (ItemWrapper item : properties) {
			if (item.isVisible()) {
				visibleProperties++;
			}
			
			if (visibleProperties % 2 == 0) {
				item.setStripe(true);
			} else {
				item.setStripe(false);
			}
			
		}
	}

	public boolean isMain() {
		return path == null || path.isEmpty();
	}

	static String getDisplayNameFromItem(Item item) {
		Validate.notNull(item, "Item must not be null.");

		String displayName = item.getDisplayName();
		if (StringUtils.isEmpty(displayName)) {
			QName name = item.getElementName();
			if (name != null) {
				displayName = name.getLocalPart();
			} else {
				displayName = item.getDefinition().getTypeName().getLocalPart();
			}
		}

		return displayName;
	}

	public boolean hasChanged() {
		
		switch (getStatus()) {
			case DELETED : 
				return true;
			case ADDED:
			case NOT_CHANGED:
				for (ItemWrapper item : getItems()) {
					if (item.hasChanged()) {
						return true;
					}
				}
		}

		
		

		return false;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ContainerWrapper(");
		// builder.append(getDisplayNameFromItem(containerValue));
		builder.append(" (");
		builder.append(status);
		builder.append(") ");
		builder.append(getItems() == null ? null : getItems().size());
		builder.append(" items)");
		return builder.toString();
	}

	/**
	 * This methods check if we want to show property in form (e.g.
	 * failedLogins, fetchResult, lastFailedLoginTimestamp must be invisible)
	 *
	 * @return
	 * @deprecated will be implemented through annotations in schema
	 */
	@Deprecated
	private boolean skipProperty(PrismPropertyDefinition def) {
		final List<QName> names = new ArrayList<>();
		names.add(PasswordType.F_FAILED_LOGINS);
		names.add(PasswordType.F_LAST_FAILED_LOGIN);
		names.add(PasswordType.F_LAST_SUCCESSFUL_LOGIN);
		names.add(PasswordType.F_PREVIOUS_SUCCESSFUL_LOGIN);
		names.add(ObjectType.F_FETCH_RESULT);
		// activation
		names.add(ActivationType.F_EFFECTIVE_STATUS);
		names.add(ActivationType.F_VALIDITY_STATUS);
		// user
		names.add(UserType.F_RESULT);
		// org and roles
		names.add(OrgType.F_APPROVAL_PROCESS);
		names.add(OrgType.F_APPROVER_EXPRESSION);
		names.add(OrgType.F_AUTOMATICALLY_APPROVED);
		names.add(OrgType.F_CONDITION);

		for (QName name : names) {
			if (name.equals(def.getName())) {
				return true;
			}
		}

		return false;
	}

	public boolean isReadonly() {
		// readonly flag in container is an override. Do not get the value from
		// definition
		// otherwise it will be propagated to items and overrides the item
		// definition.
		return readonly;
	}

	public void setReadonly(boolean readonly) {
		this.readonly = readonly;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public void sort() {
		Locale locale = WebModelServiceUtils.getLocale();
		if (locale == null) {
			locale = Locale.getDefault();
		}
		Collator collator = Collator.getInstance(locale);
		if (isSorted()) {
			collator.setStrength(Collator.SECONDARY);       // e.g. "a" should be different from "á"
			collator.setDecomposition(Collator.FULL_DECOMPOSITION);     // slower but more precise
			
//			List<ItemWrapper> containerWrappers = new ArrayList<>();
//			List<ItemWrapper> propertyOrReferenceWrapper = new ArrayList<>();
//			for(ItemWrapper w : properties) {
//				if (w instanceof ContainerWrapper) {
//					containerWrappers.add(w);
//					continue;
//				}
//				
//				if (PropertyOrReferenceWrapper.class.isAssignableFrom(w.getClass())) {
//					propertyOrReferenceWrapper.add(w);
//				}
//			}
			
			Collections.sort(properties, new Comparator<ItemWrapper>() {
				@Override
				public int compare(ItemWrapper pw1, ItemWrapper pw2) {
					
					if (pw1 instanceof ContainerWrapper) {
						((ContainerWrapper) pw1).sort();
					}
					
					if (pw2 instanceof ContainerWrapper) {
						((ContainerWrapper) pw2).sort();
					}
					
					if (PropertyOrReferenceWrapper.class.isAssignableFrom(pw1.getClass()) && pw2 instanceof ContainerWrapper) {
						return -1;
					}
					
					if (PropertyOrReferenceWrapper.class.isAssignableFrom(pw2.getClass()) && pw1 instanceof ContainerWrapper) {
						return 1;
					}
//					
					return compareByDisplayNames(pw1, pw2, collator);
				}
			});
		} else {
			Collections.sort(properties, new Comparator<ItemWrapper>() {
				@Override
				public int compare(ItemWrapper pw1, ItemWrapper pw2) {
					
					if (pw1 instanceof ContainerWrapper) {
						((ContainerWrapper) pw1).sort();
					}
					
					if (pw2 instanceof ContainerWrapper) {
						((ContainerWrapper) pw2).sort();
					}
					
					if (PropertyOrReferenceWrapper.class.isAssignableFrom(pw1.getClass()) && pw2 instanceof ContainerWrapper) {
						return -1;
					}
					
					if (PropertyOrReferenceWrapper.class.isAssignableFrom(pw2.getClass()) && pw1 instanceof ContainerWrapper) {
						return 1;
					}
					
					ItemDefinition id1 = pw1.getItemDefinition();
					ItemDefinition id2 = pw2.getItemDefinition();

					int displayOrder1 = (id1 == null || id1.getDisplayOrder() == null) ? Integer.MAX_VALUE : id1.getDisplayOrder();
					int displayOrder2 = (id2 == null || id2.getDisplayOrder() == null) ? Integer.MAX_VALUE : id2.getDisplayOrder();
					if (displayOrder1 == displayOrder2){
						return compareByDisplayNames(pw1, pw2, collator);
					} else {
						return Integer.compare(displayOrder1, displayOrder2);
					}
				}
			});
		}

	}

	private int compareByDisplayNames(ItemWrapper pw1, ItemWrapper pw2, Collator collator) {
		return collator.compare(pw1.getDisplayName(), pw2.getDisplayName());
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("ContainerValueWrapper: ");// .append(PrettyPrinter.prettyPrint(getName()));
		sb.append("\n");
		DebugUtil.debugDumpWithLabelLn(sb, "displayName", getDisplayName(), indent + 1);
		DebugUtil.debugDumpWithLabel(sb, "status", status == null ? null : status.toString(), indent + 1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabelLn(sb, "main", isMain(), indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "readonly", readonly, indent + 1);
		// DebugUtil.debugDumpWithLabel(sb, "showInheritedObjectAttributes",
		// showInheritedObjectAttributes, indent + 1);
		// sb.append("\n");
		DebugUtil.debugDumpWithLabelLn(sb, "path", path == null ? null : path.toString(), indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "containerDefinition", getDefinition() == null ? null : getDefinition().toString(),
				indent + 1);
		DebugUtil.debugDumpWithLabelLn(sb, "containerValue", containerValue == null ? null : containerValue.toString(), indent + 1);
		DebugUtil.debugDumpLabelLn(sb, "items", indent + 1);
		DebugUtil.debugDump(sb, properties, indent + 2, false);
		return sb.toString();
	}

	public PrismContainerValue<C> createContainerValueAddDelta() throws SchemaException {
		if (!hasChanged()) {
			return null;
		}

		if (getStatus() != ValueStatus.ADDED) {
			return null;
		}

		PrismContainerValue<C> newValue = containerValue.clone();
		
		for (ItemWrapper item : getItems()) {
			if (!item.hasChanged()) {
				continue;
			}

			if (item instanceof ContainerWrapper) {
				
				PrismContainer containerToAdd = ((ContainerWrapper) item).createContainerAddDelta();
				newValue.addReplaceExisting(containerToAdd);
				
			} else {

				PropertyOrReferenceWrapper propOrRef = (PropertyOrReferenceWrapper) item;
				ItemPath path = propOrRef.getPath();
				Item updatedItem = propOrRef.getUpdatedItem(containerValue.getPrismContext());
				
				if (path.size() == 2 && path.startsWithName(ObjectType.F_EXTENSION)) {
					
					// HACK HACK HACK: MID-4705, TODO: MID-4706
					PrismContainer<Containerable> extensionContainer = newValue.findOrCreateContainer(ObjectType.F_EXTENSION);
					extensionContainer.getValue().addReplaceExisting(updatedItem);
					
				} else {
				
					newValue.addReplaceExisting(updatedItem);
				}

			}
		}
		return newValue;
	}

	public <O extends ObjectType> void collectModifications(ObjectDelta<O> delta) throws SchemaException {
		if (!hasChanged()) {
			return;
		}

		for (ItemWrapper itemWrapper : getItems()) {
			if (!itemWrapper.hasChanged()) {
				continue;
			}
			ItemPath containerPath = getPath() != null ? getPath() : ItemPath.EMPTY_PATH;
			if (itemWrapper instanceof PropertyWrapper) {
				ItemDelta pDelta = computePropertyDeltas((PropertyWrapper) itemWrapper, containerPath);
				if (!pDelta.isEmpty()) {
					// HACK to remove a password replace delta is to be created
					if (getContainer().getName().equals(CredentialsType.F_PASSWORD)) {
						if (pDelta.getValuesToDelete() != null) {
							pDelta.resetValuesToDelete();
							pDelta.setValuesToReplace(new ArrayList());
						}
					}
					delta.addModification(pDelta);
				}
			} else if (itemWrapper instanceof ReferenceWrapper) {
				ReferenceDelta pDelta = computeReferenceDeltas((ReferenceWrapper) itemWrapper, containerPath);
				if (!pDelta.isEmpty()) {
					delta.addModification(pDelta);
				}
			} else if (itemWrapper instanceof ContainerWrapper) {
				((ContainerWrapper) itemWrapper).collectModifications(delta);
			} else {
				LOGGER.trace("Delta from wrapper: ignoring {}", itemWrapper);
			}
		}
	}

	private ItemDelta computePropertyDeltas(PropertyWrapper propertyWrapper, ItemPath containerPath) {
		ItemDefinition itemDef = propertyWrapper.getItemDefinition();
		ItemDelta pDelta = itemDef.createEmptyDelta(propertyWrapper.getPath());
		addItemDelta(propertyWrapper, pDelta, itemDef, containerPath);
		return pDelta;
	}

	private ReferenceDelta computeReferenceDeltas(ReferenceWrapper referenceWrapper, ItemPath containerPath) {
		PrismReferenceDefinition propertyDef = referenceWrapper.getItemDefinition();
		ReferenceDelta pDelta = new ReferenceDelta(containerPath, propertyDef.getName(), propertyDef,
				propertyDef.getPrismContext());
		addItemDelta(referenceWrapper, pDelta, propertyDef, containerPath.subPath(propertyDef.getName()));
		return pDelta;
	}

	private void addItemDelta(PropertyOrReferenceWrapper itemWrapper, ItemDelta pDelta, ItemDefinition propertyDef,
			ItemPath containerPath) {
		for (Object vWrapper : itemWrapper.getValues()) {
			if (!(vWrapper instanceof ValueWrapper)) {
				continue;
			}

			ValueWrapper valueWrapper = (ValueWrapper) vWrapper;

			valueWrapper.normalize(propertyDef.getPrismContext());
			ValueStatus valueStatus = valueWrapper.getStatus();
			if (!valueWrapper.hasValueChanged()
					&& (ValueStatus.NOT_CHANGED.equals(valueStatus) || ValueStatus.ADDED.equals(valueStatus))) {
				continue;
			}

			PrismValue newValCloned = ObjectWrapper.clone(valueWrapper.getValue());
			PrismValue oldValCloned = ObjectWrapper.clone(valueWrapper.getOldValue());
			switch (valueWrapper.getStatus()) {
				case ADDED:
					if (newValCloned != null) {
						if (SchemaConstants.PATH_PASSWORD.equivalent(containerPath)) {
							// password change will always look like add,
							// therefore we push replace
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace("Delta from wrapper: {} (password) ADD -> replace {}", pDelta.getPath(),
										newValCloned);
							}
							pDelta.setValuesToReplace(Arrays.asList(newValCloned));
						} else if (propertyDef.isSingleValue()) {
							// values for single-valued properties
							// should be pushed via replace
							// in order to prevent problems e.g. with
							// summarizing deltas for
							// unreachable resources
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace("Delta from wrapper: {} (single,new) ADD -> replace {}", pDelta.getPath(),
										newValCloned);
							}
							pDelta.setValueToReplace(newValCloned);
						} else {
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace("Delta from wrapper: {} (multi,new) ADD -> add {}", pDelta.getPath(), newValCloned);
							}
							pDelta.addValueToAdd(newValCloned);
						}
					}
					break;
				case DELETED:
					if (newValCloned != null) {
						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace("Delta from wrapper: {} (new) DELETE -> delete {}", pDelta.getPath(), newValCloned);
						}
						pDelta.addValueToDelete(newValCloned);
					}
					if (oldValCloned != null) {
						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace("Delta from wrapper: {} (old) DELETE -> delete {}", pDelta.getPath(), oldValCloned);
						}
						pDelta.addValueToDelete(oldValCloned);
					}
					break;
				case NOT_CHANGED:
					// this is modify...
					if (propertyDef.isSingleValue()) {
						// newValCloned.isEmpty()
						if (newValCloned != null && !newValCloned.isEmpty()) {
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace("Delta from wrapper: {} (single,new) NOT_CHANGED -> replace {}", pDelta.getPath(),
										newValCloned);
							}
							pDelta.setValuesToReplace(Arrays.asList(newValCloned));
						} else {
							if (oldValCloned != null) {
								if (LOGGER.isTraceEnabled()) {
									LOGGER.trace("Delta from wrapper: {} (single,old) NOT_CHANGED -> delete {}", pDelta.getPath(),
											oldValCloned);
								}
								pDelta.addValueToDelete(oldValCloned);
							}
						}
					} else {
						if (newValCloned != null && !newValCloned.isEmpty()) {
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace("Delta from wrapper: {} (multi,new) NOT_CHANGED -> add {}", pDelta.getPath(),
										newValCloned);
							}
							pDelta.addValueToAdd(newValCloned);
						}
						if (oldValCloned != null) {
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace("Delta from wrapper: {} (multi,old) NOT_CHANGED -> delete {}", pDelta.getPath(),
										oldValCloned);
							}
							pDelta.addValueToDelete(oldValCloned);
						}
					}
					break;
			}
		}
	}

	
	private Item createItem(PropertyOrReferenceWrapper itemWrapper, ItemDefinition propertyDef) {
		List<PrismValue> prismValues = new ArrayList<>();
		for (Object vWrapper : itemWrapper.getValues()) {
			if (!(vWrapper instanceof ValueWrapper)) {
				continue;
			}

			ValueWrapper valueWrapper = (ValueWrapper) vWrapper;

			valueWrapper.normalize(propertyDef.getPrismContext());
			ValueStatus valueStatus = valueWrapper.getStatus();
			if (!valueWrapper.hasValueChanged()
					&& (ValueStatus.NOT_CHANGED.equals(valueStatus) || ValueStatus.ADDED.equals(valueStatus))) {
				continue;
			}

			prismValues.add(valueWrapper.getValue().clone());

		}

		Item item = itemWrapper.getItem().clone();
		try {
			item.addAll(prismValues);
		} catch (SchemaException e) {
			LoggingUtils.logException(LOGGER, "could not crate delta for " + itemWrapper.getItem(), e);
			return null;
		}
		return item;

	}
	
	public ContainerStatus getObjectStatus() {
		return objectStatus;
	}

	// TODO: unify with other isVisibleMethods
	public boolean isVisible() {
		PrismContainerDefinition<C> def = getDefinition();

		if (def.isIgnored() || (def.isOperational() && !def.getTypeName().equals(MetadataType.COMPLEX_TYPE))) {
			return false;
		}

		if (def.isDeprecated() && containerValue.isEmpty()) {
			return false;
		}
		
		if (def.getTypeName().equals(MetadataType.COMPLEX_TYPE)) {
			return isShowMetadata();
		}

		if (ValueStatus.DELETED.equals(status)){
			return false;
		}
		
			// TODO: emphasized
		switch (objectStatus) {
			case MODIFYING:
				return canReadAndModify(def) || showEmptyCanReadAndModify(def);
			case ADDING:
				return emphasizedAndCanAdd(def) || showEmptyAndCanAdd(def);
		}

		return false;
	}

	private boolean canReadAndModify(PrismContainerDefinition<C> def) {
		return def.canRead(); // && def.canModify();
	}

	private boolean showEmptyCanReadAndModify(PrismContainerDefinition<C> def) {
		return def.canRead() && isShowEmpty(); //def.canModify() && isShowEmpty();
	}

	private boolean showEmptyAndCanAdd(PrismContainerDefinition<C> def) {
		return def.canAdd();
	}

	private boolean emphasizedAndCanAdd(PrismContainerDefinition<C> def) {
		return def.canAdd() && def.isEmphasized();
	}

	@Override
	public void setShowEmpty(boolean showEmpty, boolean recursive) {
		super.setShowEmpty(showEmpty, recursive);
		getItems().forEach(item -> {
			item.setShowEmpty(showEmpty, recursive);
		});

	}

	@Override
	public void setShowMetadata(boolean showMetadata) {
		super.setShowMetadata(showMetadata);
		getItems().forEach(value -> {
			if (value instanceof ContainerWrapper) {
				((ContainerWrapper<C>) value).setShowMetadata(showMetadata);
			}
		});
	}

	// @Override
	public boolean checkRequired(PageBase pageBase) {
		boolean rv = true;
		for (ItemWrapper itemWrapper : getItems()) {
			if (!itemWrapper.checkRequired(pageBase)) {
				rv = false;     // not returning directly as we want to go through all the values
			}
		}
		return rv;
	}

	public PropertyOrReferenceWrapper findPropertyWrapper(QName name) {
		Validate.notNull(name, "QName must not be null.");
		for (ItemWrapper wrapper : getItems()) {
			if (wrapper instanceof ContainerWrapper) {
				continue;
			}
			if (QNameUtil.match(name, wrapper.getItem().getElementName())) {
				return (PropertyOrReferenceWrapper) wrapper;
			}
		}
		return null;
	}
	
	public ItemWrapper findPropertyWrapper(ItemPath itemPath) {
		Validate.notNull(itemPath, "Item path must not be null.");
		for (ItemWrapper wrapper : getItems()) {
			if (wrapper instanceof ContainerWrapper) {
				continue;
			}
			if (itemPath.equivalent(wrapper.getPath())) {
				return (PropertyOrReferenceWrapper) wrapper;
			}
		}
		return null;
	}


	public <T extends Containerable> ContainerWrapper<T> findContainerWrapper(QName qname) {
		return findContainerWrapper(new ItemPath(qname));
	}
	
	public <T extends Containerable> ContainerWrapper<T> findContainerWrapper(ItemPath path) {
		Validate.notNull(path, "QName must not be null.");
		for (ItemWrapper wrapper : getItems()) {
			if (!(wrapper instanceof ContainerWrapper)) {
				continue;
			}
			ContainerWrapper<T> containerWrapper = (ContainerWrapper<T>) wrapper;
			if (containerWrapper.getPath().equivalent(path)) {
				return containerWrapper;
			} else {
				ContainerWrapper<T> childrenContainer = containerWrapper.findContainerWrapper(path);
				if (childrenContainer != null){
					return childrenContainer;
				}
			}
		}
		return null;
	}

	public ContainerValueWrapper<C> findContainerValueWrapper(ItemPath path) {
		Validate.notNull(path, "QName must not be null.");
		for (ItemWrapper wrapper : getItems()) {
			if (!(wrapper instanceof ContainerValueWrapper)) {
				continue;
			}
			return ((ContainerValueWrapper<C>) wrapper).findContainerValueWrapper(path);

		}
		return null;
	}

	public boolean containsMultivalueContainer(){
		for (ItemWrapper wrapper : getItems()) {
			if (!(wrapper instanceof ContainerWrapper)) {
				continue;
			}
			if (!((ContainerWrapper<C>) wrapper).getItemDefinition().isSingleValue()){
				return true;
			}
		}
		return false;
	}

	public List<QName> getChildMultivalueContainersToBeAdded(){
		List<QName> pathList = new ArrayList<>();
		for (ItemWrapper wrapper : getItems()) {
			if (!(wrapper instanceof ContainerWrapper)) {
				continue;
			}
			if (!((ContainerWrapper<C>)wrapper).getItemDefinition().canAdd() ||
					!((ContainerWrapper<C>)wrapper).getItemDefinition().canModify()){
				continue;
			}
			if (!((ContainerWrapper<C>) wrapper).getItemDefinition().isSingleValue()){
				pathList.add(((ContainerWrapper<C>) wrapper).getName());
			}
		}
		return pathList;
	}

	public String getDisplayName() {
		if (getContainer().isMain()) {
			return "prismContainer.mainPanelDisplayName";
		}

		if (getDefinition() == null) {
			return WebComponentUtil.getDisplayName(containerValue);
		}

		if (getDefinition().isSingleValue()) {

			return ContainerWrapper.getDisplayNameFromItem(getContainerValue().getContainer());
		}
		return WebComponentUtil.getDisplayName(containerValue);
	}

}
