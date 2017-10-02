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

import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

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

	ContainerWrapper(PrismContainer<C> container, ContainerStatus status, ItemPath path) {
		Validate.notNull(container, "container must not be null.");
		Validate.notNull(status, "Container status must not be null.");
		Validate.notNull(container.getDefinition(), "container definition must not be null.");

		this.container = container;
		this.status = status;
		this.path = path;

	}

	ContainerWrapper(PrismContainer<C> container, ContainerStatus status, ItemPath path, boolean readOnly) {
		this(container, status, path);
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

	@Override
	public String getDisplayName() {
		if (StringUtils.isNotEmpty(displayName)) {
			return displayName;
		}
		return getDisplayNameFromItem(container);
	}

	@Override
	public void setDisplayName(String name) {
		this.displayName = name;
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
		builder.append(getDisplayNameFromItem(container));
		builder.append(" (");
		builder.append(status);
		builder.append(") ");
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
		final List<QName> names = new ArrayList<QName>();
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

	public void addValue(boolean showEmpty) {
		getValues().add(createItem(showEmpty));
	}

	public ContainerValueWrapper<C> createItem(boolean showEmpty) {
		PrismContainerValue<C> pcv = container.createNewValue();
		ContainerValueWrapper<C> wrapper = new ContainerValueWrapper<C>(this, pcv, ValueStatus.ADDED, pcv.getPath());
		wrapper.setShowEmpty(showEmpty, true);
		return wrapper;
	}

	public void sort(final PageBase pageBase) {
		for (ContainerValueWrapper<C> valueWrapper : getValues()) {
			valueWrapper.sort(pageBase);
		}
	}

	@Override
	public String debugDump() {
		return debugDump(0);
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
		// Does not make much sense, but it is given by the interface
	}

	public PrismContainer createContainerAddDelta() throws SchemaException {

		PrismContainer containerAdd = container.clone();

		for (ContainerValueWrapper<C> itemWrapper : getValues()) {
			if (!itemWrapper.hasChanged()) {
				continue;
			}
			if (itemWrapper.isMain()) {
				containerAdd.setValue(itemWrapper.createContainerValueAddDelta());
				continue;
			}
			containerAdd.add(itemWrapper.createContainerValueAddDelta());
		}
		return containerAdd;
	}

	public <O extends ObjectType> void collectModifications(ObjectDelta<O> delta) throws SchemaException {

		if (!hasChanged()) {
			return;
		}

		for (ContainerValueWrapper<C> itemWrapper : getValues()) {
			if (!itemWrapper.hasChanged()) {
				continue;
			}

			switch (itemWrapper.getStatus()) {
				case ADDED:
					PrismContainerValue<C> valueToAdd = itemWrapper.createContainerValueAddDelta();
					if (getItemDefinition().isMultiValue()) {
						delta.addModificationAddContainer(getPath(), valueToAdd);
						break;
					}
					delta.addModificationReplaceContainer(getPath(), valueToAdd);
					break;
				case NOT_CHANGED:
					itemWrapper.collectModifications(delta);
					break;
				case DELETED:
					delta.addModificationDeleteContainer(itemWrapper.getPath(), itemWrapper.getContainerValue().clone());
					break;
			}
		}
	}

	public <O extends ObjectType> void collectDeleteDelta(ObjectDelta<O> delta,  PrismContext prismContext) throws SchemaException {

		for (ContainerValueWrapper<C> itemWrapper : getValues()) {
			if (ValueStatus.DELETED.equals(itemWrapper.getStatus())){
				ContainerDelta<C> containerDelta = new ContainerDelta(ItemPath.EMPTY_PATH, itemWrapper.getDefinition().getName(),
						itemWrapper.getDefinition(), prismContext);
				containerDelta.addValuesToDelete(itemWrapper.getContainerValue().clone());
				delta.addModification(containerDelta);
			}
		}
	}

	public <O extends ObjectType> void collectAddDelta(ObjectDelta<O> delta, PrismContext prismContext) throws SchemaException {

//		ContainerDelta containerDelta = new ContainerDelta(propertyPath, def.getName(), def, prismContext);

		for (ContainerValueWrapper<C> itemWrapper : getValues()) {
			if (ValueStatus.ADDED.equals(itemWrapper.getStatus())) {
				ContainerDelta<C> containerDelta = new ContainerDelta(ItemPath.EMPTY_PATH, itemWrapper.getDefinition().getName(),
						itemWrapper.getDefinition(), prismContext);
//				itemWrapper.getContainerValue().applyDefinition(def, false);
				containerDelta.addValueToAdd(itemWrapper.getContainerValue().clone());
				if (!containerDelta.isEmpty()){
					delta.addModification(containerDelta);
				}

			}
		}
//		ContainerDelta<C> containerDelta = delta.createContainerModification(propertyPath);
//
//		List<PrismContainerValue> containerValuesToAdd = new ArrayList<>();
//
//		for (ContainerValueWrapper<C> itemWrapper : getValues()) {
//			if (ValueStatus.ADDED.equals(itemWrapper.getStatus())){
//				containerValuesToAdd.add(itemWrapper.getContainerValue().clone());

//				ContainerDelta<C> containerDelta = new ContainerDelta(new ItemPath(FocusType.F_ASSIGNMENT), itemWrapper.getDefinition().getName(),
//						itemWrapper.getDefinition(), prismContext);
//				containerDelta.addValuesToAdd(itemWrapper.getContainerValue().clone());
//			}
//		}
//		containerDelta.addValuesToAdd(containerValuesToAdd.toArray(new PrismContainerValue[containerValuesToAdd.size()]));
	}

	@Override
	public boolean checkRequired(PageBase pageBase) {
		boolean rv = true;
		for (ContainerValueWrapper<C> itemWrapper : getValues()) {
			if (!itemWrapper.checkRequired(pageBase)) {
				rv = false;
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

		if (def.isIgnored() || (def.isOperational()) && (!def.getTypeName().equals(MetadataType.COMPLEX_TYPE))) {
			return false;
		}

		switch (status) {
			case MODIFYING:
				return isNotEmptyAndCanReadAndModify(def) || showEmptyCanReadAndModify(def);
			case ADDING:
				return emphasizedAndCanAdd(def) || showEmptyAndCanAdd(def);
		}

		return false;
	}

	private boolean isNotEmptyAndCanReadAndModify(PrismContainerDefinition<C> def) {
		return def.canRead() && def.canModify();
	}

	private boolean showEmptyCanReadAndModify(PrismContainerDefinition<C> def) {
		return def.canRead() && def.canModify() && isShowEmpty();
	}

	private boolean showEmptyAndCanAdd(PrismContainerDefinition<C> def) {
		return def.canAdd() && isShowEmpty();
	}

	private boolean emphasizedAndCanAdd(PrismContainerDefinition<C> def) {
		return def.canAdd() && def.isEmphasized();
	}

}
