/*
 * Copyright (c) 2010-2017 Evolveum
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
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;

/**
 * @author lazyman
 * @author katkav
 */
public class ContainerWrapper<C extends Containerable> extends PrismWrapper implements
		ItemWrapper<PrismContainer<C>, PrismContainerDefinition<C>, ContainerValueWrapper<C>>, Serializable, DebugDumpable {

	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(ContainerWrapper.class);

	private String displayName;
	private PrismContainer<C> container;
	private ContainerStatus status;

	private ItemPath path;
	private List<ContainerValueWrapper<C>> values;

	private boolean readonly;
	private boolean removeContainerButtonVisible;
	private boolean addContainerButtonVisible;
	
	private ContainerStatus objectStatus;

	//TODO: HACK to have custom filter for association contianer here becasue of creating new association:
	private ObjectFilter filter;

	ContainerWrapper(PrismContainer<C> container, ContainerStatus objectStatus, ContainerStatus status, ItemPath path) {
		Validate.notNull(container, "container must not be null.");
		Validate.notNull(status, "Container status must not be null.");
		Validate.notNull(container.getDefinition(), "container definition must not be null.");

		this.container = container;
		this.objectStatus = objectStatus;
		this.status = status;
		this.path = path;

	}

	ContainerWrapper(PrismContainer<C> container, ContainerStatus objectStatus, ContainerStatus status, ItemPath path, boolean readOnly) {
		this(container, objectStatus, status, path);
		this.readonly = readOnly;
	}

	public void revive(PrismContext prismContext) throws SchemaException {
		if (container != null) {
			container.revive(prismContext);
		}
		if (getItemDefinition() != null) {
			getItemDefinition().revive(prismContext);
		}
		if (values != null) {
			for (ContainerValueWrapper itemWrapper : values) {
				itemWrapper.revive(prismContext);
			}
		}
	}
	
	public ObjectFilter getFilter() {
		return filter;
	}
	
	public void setFilter(ObjectFilter filter) {
		this.filter = filter;
	}

	@Override
	public PrismContainerDefinition<C> getItemDefinition() {
		return container.getDefinition();
	}
	
	public ContainerStatus getStatus() {
		return status;
	}

	public ItemPath getPath() {
		return path;
	}

	@Override
	public PrismContainer<C> getItem() {
		return container;
	}

	@Override
	public List<ContainerValueWrapper<C>> getValues() {
		if (values == null) {
			values = new ArrayList<>();
		}
		return values;

	}

	public void setProperties(List<ContainerValueWrapper<C>> properties) {
		this.values = properties;
	}
	
	public ContainerStatus getObjectStatus() {
		return objectStatus;
	}

	public PropertyOrReferenceWrapper findPropertyWrapper(QName name) {
		Validate.notNull(name, "QName must not be null.");
		for (ContainerValueWrapper wrapper : getValues()) {
			PropertyOrReferenceWrapper propertyWrapper = wrapper.findPropertyWrapper(name);
			if (propertyWrapper != null) {
				return propertyWrapper;
			}
		}
		return null;
	}

	public ContainerWrapper<C> findContainerWrapper(ItemPath path) {
		Validate.notNull(path, "QName must not be null.");
		for (ContainerValueWrapper<C> wrapper : getValues()) {
			ContainerWrapper<C> containerWrapper = wrapper.findContainerWrapper(path);
			if (containerWrapper != null) {
				return containerWrapper;
			}
		}
		return null;
	}

	public ContainerValueWrapper<C> findContainerValueWrapper(ItemPath path) {
		Validate.notNull(path, "QName must not be null.");
		for (ContainerValueWrapper<C> wrapper : getValues()) {

			if (path.equivalent(wrapper.getPath())) {
				return wrapper;
			}

			ContainerValueWrapper<C> containerWrapper = wrapper.findContainerValueWrapper(path);
			if (containerWrapper != null) {
				return containerWrapper;
			}
		}
		return null;
	}

	public void computeStripes() {
		int visibleProperties = 0;
		for (ContainerValueWrapper<C> item : values) {
			item.computeStripes();
			}
	}

	// TODO: unify with PropertyOrReferenceWrapper.getDisplayName()
	@Override
	public String getDisplayName() {
		if (displayName == null) {
			// Lazy loading of a localized name.
			// We really want to remember a processed name in the wrapper.
			// getDisplatName() method may be called many times, e.g. during sorting.
			displayName = getDisplayNameFromItem(container);
		}
		return displayName;
	}
	
	@Override
	public void setDisplayName(String displayName) {
		this.displayName = localizeName(displayName);
	}

	@Override
	public QName getName() {
		return getItem().getElementName();
	}

	public boolean isMain() {
		return path == null || path.isEmpty();
	}

	static String getDisplayNameFromItem(Item item) {
		Validate.notNull(item, "Item must not be null.");

		String displayName = item.getDisplayName();
		if (!StringUtils.isEmpty(displayName)) {
			return localizeName(displayName);
		}
		
		QName name = item.getElementName();
		if (name != null) {
			displayName = name.getLocalPart();
		} else {
			displayName = item.getDefinition().getTypeName().getLocalPart();
		}
		
		return localizeName(displayName);
	}

	static String localizeName(String nameKey) {
		Validate.notNull(nameKey, "Null localization key");
		return ColumnUtils.createStringResource(nameKey).getString();
	}

	public boolean hasChanged() {
		for (ContainerValueWrapper item : getValues()) {
			if (item.hasChanged()) {
				return true;
			}
		}

		return false;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ContainerWrapper(");
		if (displayName == null) {
			if (container == null) {
				builder.append("null");
			} else {
				builder.append(container.getElementName());
			}
		} else {
			builder.append(displayName);
		}
		builder.append(" [");
		builder.append(status);
		builder.append("] ");
		builder.append(getValues() == null ? null : getValues().size());
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

	@Override
	public boolean isEmpty() {
		return getItem().isEmpty();
	}

	public void addValue(ContainerValueWrapper<C> newValue) {
		getValues().add(newValue);
	}
	
	@Override
	public void addValue(boolean showEmpty) {
		// TODO: but fist need to clean up relaition between wrapper and factory.
		throw new UnsupportedOperationException("Not implemented yet");
	}

	public void sort() {
		for (ContainerValueWrapper<C> valueWrapper : getValues()) {
			valueWrapper.sort();
		}
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("ContainerWrapper: ").append(PrettyPrinter.prettyPrint(getName())).append("\n");
		DebugUtil.debugDumpWithLabel(sb, "displayName", displayName, indent + 1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "status", status == null ? null : status.toString(), indent + 1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "main", isMain(), indent + 1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "readonly", readonly, indent + 1);
		sb.append("\n");
		// DebugUtil.debugDumpWithLabel(sb, "showInheritedObjectAttributes",
		// showInheritedObjectAttributes, indent + 1);
		// sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "path", path == null ? null : path.toString(), indent + 1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "containerDefinition",
				getItemDefinition() == null ? null : getItemDefinition().toString(), indent + 1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "container", container == null ? null : container.toString(), indent + 1);
		sb.append("\n");
		DebugUtil.debugDumpLabel(sb, "properties", indent + 1);
		sb.append("\n");
		DebugUtil.debugDump(sb, values, indent + 2, false);
		return sb.toString();
	}

	@Override
	public boolean isStripe() {
		// Does not make much sense, but it is given by the interface
		return false;
	}

	@Override
	public void setStripe(boolean isStripe) {
		for (ContainerValueWrapper value : values) {
			value.computeStripes();
		}
	}

	public PrismContainer<C> createContainerAddDelta() throws SchemaException {

		PrismContainer<C> containerAdd = container.clone();

		for (ContainerValueWrapper<C> itemWrapper : getValues()) {
			if (!itemWrapper.hasChanged()) {
				continue;
			}
			
			PrismContainerValue<C> newContainerValue = itemWrapper.createContainerValueAddDelta();
			
			if (newContainerValue == null) {
				continue;
			}
			
			if (itemWrapper.isMain()) {
				containerAdd.setValue(newContainerValue);
				continue;
			}
			
			if (containerAdd.isSingleValue() || containerAdd.isEmpty()) {
				containerAdd.replace(newContainerValue);
			} else {
				containerAdd.add(newContainerValue);
			}
		}
		return containerAdd;
	}

	public <O extends ObjectType> void collectModifications(ObjectDelta<O> delta) throws SchemaException {

		for (ContainerValueWrapper<C> containerValueWrapper : getValues()) {
			if (!containerValueWrapper.hasChanged()) {
				continue;
			}

			switch (containerValueWrapper.getStatus()) {
				case ADDED:
					if (!isMain()) {
						PrismContainerValue<C> valueToAdd = containerValueWrapper.createContainerValueAddDelta();
						if (getItemDefinition().isMultiValue()) {
							delta.addModificationAddContainer(getPath(), valueToAdd);
							break;
						}
						delta.addModificationReplaceContainer(getPath(), valueToAdd);
						break;
					}
				case NOT_CHANGED:
					containerValueWrapper.collectModifications(delta);
					break;
				case DELETED:
					PrismContainerValue clonedValue = containerValueWrapper.getContainerValue().clone();
					clonedValue.clear();
					delta.addModificationDeleteContainer(getPath(), clonedValue);
					break;
			}
		}
	}

	@Override
	public boolean checkRequired(PageBase pageBase) {
		boolean rv = true;
		for (ContainerValueWrapper<C> valueWrapper : getValues()) {
			if (!valueWrapper.checkRequired(pageBase)) {
				rv = false;     // not returning directly as we want to go through all the values
			}
		}
		return rv;
	}

	@Override
	public void setShowEmpty(boolean showEmpty, boolean recursive) {
		super.setShowEmpty(showEmpty, recursive);
		if (recursive) {
			getValues().forEach(value -> value.setShowEmpty(showEmpty, recursive));
		}

	}

	@Override
	public void setShowMetadata(boolean showMetadata) {
		super.setShowMetadata(showMetadata);
		getValues().forEach(value -> value.setShowMetadata(showMetadata));
	}

	@Override
	public boolean isEnforceRequiredFields() {
		return true;// objectWrapper != null &&
					// objectWrapper.isEnforceRequiredFields();
	}

	@Override
	public ContainerWrapper getParent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isVisible() {
		PrismContainerDefinition<C> def = getItemDefinition();

		if (def.getProcessing() != null && def.getProcessing() != ItemProcessing.AUTO) {
			return false;
			
		}
		
		if (def.isOperational() && (!def.getTypeName().equals(MetadataType.COMPLEX_TYPE))) {
			return false;
		}
		
		if (def.isDeprecated() && isEmpty()) {
			return false;
		}

		//TODO: is this correct place? shouldn't we restrict creation for multivalue containers
		//dirrectly in factory? this can plausible cause problems while computing deltas.
		if (!getItem().isSingleValue() && (getValues() == null || getValues().size() == 0)){
			return false;
		}

		switch (objectStatus) {
			case MODIFYING:
				return isNotEmptyAndCanReadAndModify(def) || showEmptyCanReadAndModify(def);
			case ADDING:
				return emphasizedAndCanAdd(def) || showEmptyAndCanAdd(def);
		}

		return false;
	}
	
	@Override
	public ItemProcessing getProcessing() {
		return getItemDefinition().getProcessing();
	}

	private boolean isNotEmptyAndCanReadAndModify(PrismContainerDefinition<C> def) {
		return def.canRead();// && def.canModify();
	}

	private boolean showEmptyCanReadAndModify(PrismContainerDefinition<C> def) {
		return def.canRead() && isShowEmpty();// && def.canModify() && isShowEmpty();
	}

	private boolean showEmptyAndCanAdd(PrismContainerDefinition<C> def) {
		return def.canAdd() && isShowEmpty();
	}

	private boolean emphasizedAndCanAdd(PrismContainerDefinition<C> def) {
		return def.canAdd() && def.isEmphasized();
	}

	@Override
	public boolean isDeprecated() {
		return getItemDefinition().isDeprecated();
	}

	@Override
	public String getDeprecatedSince() {
		return getItemDefinition().getDeprecatedSince();
	}

	public boolean isRemoveContainerButtonVisible() {
		return removeContainerButtonVisible;
	}

	public void setRemoveContainerButtonVisible(boolean removeContainerButtonVisible) {
		this.removeContainerButtonVisible = removeContainerButtonVisible;
	}

	public boolean isAddContainerButtonVisible() {
		return addContainerButtonVisible;
	}

	public void setAddContainerButtonVisible(boolean addContainerButtonVisible) {
		this.addContainerButtonVisible = addContainerButtonVisible;
	}

	@Override
	public boolean isExperimental() {
		return getItemDefinition().isExperimental();
	}
}
