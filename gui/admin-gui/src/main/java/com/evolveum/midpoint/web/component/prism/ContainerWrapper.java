/*
 * Copyright (c) 2010-2013 Evolveum
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
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;

/**
 * @author lazyman
 */
public class ContainerWrapper<T extends PrismContainer> implements ItemWrapper, Serializable, DebugDumpable {

	private static final Trace LOGGER = TraceManager.getTrace(ContainerWrapper.class);

	private static final List<QName> INHERITED_OBJECT_ATTRIBUTES = Arrays.asList(ObjectType.F_NAME,
			ObjectType.F_DESCRIPTION, ObjectType.F_FETCH_RESULT, ObjectType.F_PARENT_ORG,
			ObjectType.F_PARENT_ORG_REF, FocusType.F_LINK, FocusType.F_LINK_REF);

	private static final String DOT_CLASS = ContainerWrapper.class.getName() + ".";
	private static final String CREATE_PROPERTIES = DOT_CLASS + "createProperties";

	private String displayName;
	private ObjectWrapper<? extends ObjectType> object;
	private T container;
	private ContainerStatus status;

	private boolean main;
	private ItemPath path;
	private List<ItemWrapper> properties;

	private boolean readonly;
	private boolean showInheritedObjectAttributes;

	private OperationResult result;

	private PrismContainerDefinition containerDefinition;

	public ContainerWrapper(ObjectWrapper object, T container, ContainerStatus status, ItemPath path,
			PageBase pageBase) {
		Validate.notNull(container, "Prism object must not be null.");
		Validate.notNull(status, "Container status must not be null.");
		Validate.notNull(pageBase, "pageBase must not be null.");

		this.object = object;
		this.container = container;
		this.status = status;
		this.path = path;
		main = path == null;
		readonly = object.isReadonly(); // [pm] this is quite questionable
		showInheritedObjectAttributes = object.isShowInheritedObjectAttributes();
		// have to be after setting "main" property
		containerDefinition = getItemDefinition();
		properties = createProperties(pageBase);
	}

	public void revive(PrismContext prismContext) throws SchemaException {
		if (container != null) {
			container.revive(prismContext);
		}
		if (containerDefinition != null) {
			containerDefinition.revive(prismContext);
		}
		if (properties != null) {
			for (ItemWrapper itemWrapper : properties) {
				itemWrapper.revive(prismContext);
			}
		}
	}

	@Override
	public PrismContainerDefinition getItemDefinition() {
		if (main) {
			return object.getDefinition();
		} else {
			return object.getDefinition().findContainerDefinition(path);
		}
	}

	OperationResult getResult() {
		return result;
	}

	void clearResult() {
		result = null;
	}

	ObjectWrapper getObject() {
		return object;
	}

	ContainerStatus getStatus() {
		return status;
	}

	public ItemPath getPath() {
		return path;
	}

	public T getItem() {
		return container;
	}

	public List<ItemWrapper> getItems() {
		return properties;
	}

	public ItemWrapper findPropertyWrapper(QName name) {
		Validate.notNull(name, "QName must not be null.");
		for (ItemWrapper wrapper : getItems()) {
			if (name.equals(wrapper.getItem().getElementName())) {
				return wrapper;
			}
		}
		return null;
	}

	private List<ItemWrapper> createProperties(PageBase pageBase) {
		result = new OperationResult(CREATE_PROPERTIES);

		List<ItemWrapper> properties = new ArrayList<ItemWrapper>();

		PrismContainerDefinition definition = null;
		PrismObject parent = getObject().getObject();
		Class clazz = parent.getCompileTimeClass();
		if (ShadowType.class.isAssignableFrom(clazz)) {
			QName name = containerDefinition.getName();

			if (ShadowType.F_ATTRIBUTES.equals(name)) {
				try {
					definition = object.getRefinedAttributeDefinition();

					if (definition == null) {
						PrismReference resourceRef = parent.findReference(ShadowType.F_RESOURCE_REF);
						PrismObject<ResourceType> resource = resourceRef.getValue().getObject();

						definition = pageBase
								.getModelInteractionService()
								.getEditObjectClassDefinition((PrismObject<ShadowType>) object.getObject(), resource,
										AuthorizationPhaseType.REQUEST)
								.toResourceAttributeContainerDefinition();

						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace("Refined account def:\n{}", definition.debugDump());
						}
					}
				} catch (Exception ex) {
					LoggingUtils.logException(LOGGER,
							"Couldn't load definitions from refined schema for shadow", ex);
					result.recordFatalError(
							"Couldn't load definitions from refined schema for shadow, reason: "
									+ ex.getMessage(), ex);

					return properties;
				}
			} else {
				definition = containerDefinition;
			}
		} else if (ResourceType.class.isAssignableFrom(clazz)) {
			if (containerDefinition != null) {
				definition = containerDefinition;
			} else {
				definition = container.getDefinition();
			}
		} else {
			definition = containerDefinition;
		}

		if (definition == null) {
			LOGGER.error("Couldn't get property list from null definition {}",
					new Object[] { container.getElementName() });
			return properties;
		}

		// assignments are treated in a special way -- we display names of
		// org.units and roles
		// (but only if ObjectWrapper.isShowAssignments() is true; otherwise
		// they are filtered out by ObjectWrapper)
		if (container.getCompileTimeClass() != null
				&& AssignmentType.class.isAssignableFrom(container.getCompileTimeClass())) {

			for (Object o : container.getValues()) {
				PrismContainerValue<AssignmentType> pcv = (PrismContainerValue<AssignmentType>) o;

				AssignmentType assignmentType = pcv.asContainerable();

				if (assignmentType.getTargetRef() == null) {
					continue;
				}

				// hack... we want to create a definition for Name
				// PrismPropertyDefinition def = ((PrismContainerValue)
				// pcv.getContainer().getParent()).getContainer().findProperty(ObjectType.F_NAME).getDefinition();
				PrismPropertyDefinition def = new PrismPropertyDefinition(ObjectType.F_NAME,
						DOMUtil.XSD_STRING, pcv.getPrismContext());

				if (OrgType.COMPLEX_TYPE.equals(assignmentType.getTargetRef().getType())) {
					def.setDisplayName("Org.Unit");
					def.setDisplayOrder(100);
				} else if (RoleType.COMPLEX_TYPE.equals(assignmentType.getTargetRef().getType())) {
					def.setDisplayName("Role");
					def.setDisplayOrder(200);
				} else {
					continue;
				}

				PrismProperty<Object> temp = def.instantiate();

				String value = formatAssignmentBrief(assignmentType);

				temp.setValue(new PrismPropertyValue<Object>(value));
				// TODO: do this.isReadOnly() - is that OK? (originally it was the default behavior for all cases)
				properties.add(new PropertyWrapper(this, temp, this.isReadonly(), ValueStatus.NOT_CHANGED)); 
			}

		} else if (isShadowAssociation()) {
			if (object.getAssociations() != null) {
				for (PrismProperty property : object.getAssociations()) {
					// TODO: fix this -> for now, read only is supported..
					PropertyWrapper propertyWrapper = new PropertyWrapper(this, property, true,
							ValueStatus.NOT_CHANGED);
					properties.add(propertyWrapper);
				}
			}

		} else { // if not an assignment

			if ((container.getValues().size() == 1 || container.getValues().isEmpty()) 
					&& (containerDefinition == null || containerDefinition.isSingleValue())) {

				// there's no point in showing properties for non-single-valued
				// parent containers,
				// so we continue only if the parent is single-valued

				Collection<ItemDefinition> propertyDefinitions = definition.getDefinitions();
				for (ItemDefinition itemDef : propertyDefinitions) {
					if (itemDef instanceof PrismPropertyDefinition) {

						PrismPropertyDefinition def = (PrismPropertyDefinition) itemDef;
						if (def.isIgnored() || skipProperty(def)) {
							continue;
						}
						if (!showInheritedObjectAttributes
								&& INHERITED_OBJECT_ATTRIBUTES.contains(def.getName())) {
							continue;
						}

						// capability handling for activation properties
						if (isShadowActivation() && !hasCapability(def)) {
							continue;
						}

						if (isShadowAssociation()) {
							continue;
						}

						PrismProperty property = container.findProperty(def.getName());
						boolean propertyIsReadOnly;
						// decision is based on parent object status, not this
						// container's one (because container can be added also
						// to an existing object)
						if (object.getStatus() == ContainerStatus.MODIFYING) {

							propertyIsReadOnly = !def.canModify();
						} else {
							propertyIsReadOnly = !def.canAdd();
						}
						if (property == null) {
							properties.add(new PropertyWrapper(this, def.instantiate(), propertyIsReadOnly,
									ValueStatus.ADDED));
						} else {
							properties.add(new PropertyWrapper(this, property, propertyIsReadOnly,
									ValueStatus.NOT_CHANGED));
						}
					} else if (itemDef instanceof PrismReferenceDefinition){
						PrismReferenceDefinition def = (PrismReferenceDefinition) itemDef;
						
						if (INHERITED_OBJECT_ATTRIBUTES.contains(def.getName())){
							continue;
						}
						
						PrismReference reference = container.findReference(def.getName());
						boolean propertyIsReadOnly;
						// decision is based on parent object status, not this
						// container's one (because container can be added also
						// to an existing object)
						if (object.getStatus() == ContainerStatus.MODIFYING) {

							propertyIsReadOnly = !def.canModify();
						} else {
							propertyIsReadOnly = !def.canAdd();
						}
						if (reference == null) {
							properties.add(new ReferenceWrapper(this, def.instantiate(), propertyIsReadOnly,
									ValueStatus.ADDED));
						} else {
							properties.add(new ReferenceWrapper(this, reference, propertyIsReadOnly,
									ValueStatus.NOT_CHANGED));
						}

					}
				}
			}
		}

		Collections.sort(properties, new ItemWrapperComparator());

		result.recomputeStatus();

		return properties;
	}

	private boolean isPassword(PrismPropertyDefinition def) {
		// in the future, this option could apply as well
		return CredentialsType.F_PASSWORD.equals(container.getElementName())
				|| CredentialsType.F_PASSWORD.equals(def.getName()); 
	}

	private boolean isShadowAssociation() {
		if (!ShadowType.class.isAssignableFrom(getObject().getObject().getCompileTimeClass())) {
			return false;
		}

		if (!ShadowType.F_ASSOCIATION.equals(container.getElementName())) {
			return false;
		}

		return true;
	}

	private boolean isShadowActivation() {
		if (!ShadowType.class.isAssignableFrom(getObject().getObject().getCompileTimeClass())) {
			return false;
		}

		if (!ShadowType.F_ACTIVATION.equals(container.getElementName())) {
			return false;
		}

		return true;
	}

	private boolean hasCapability(PrismPropertyDefinition def) {
		ShadowType shadow = (ShadowType) getObject().getObject().asObjectable();

		ActivationCapabilityType cap = ResourceTypeUtil.getEffectiveCapability(shadow.getResource(),
				ActivationCapabilityType.class);

		if (ActivationType.F_VALID_FROM.equals(def.getName()) && cap.getValidFrom() == null) {
			return false;
		}

		if (ActivationType.F_VALID_TO.equals(def.getName()) && cap.getValidTo() == null) {
			return false;
		}

		if (ActivationType.F_ADMINISTRATIVE_STATUS.equals(def.getName()) && cap.getStatus() == null) {
			return false;
		}

		return true;
	}

	// FIXME temporary - brutal hack - the following three methods are copied from
	// AddRoleAssignmentAspect - Pavol M.

	private String formatAssignmentBrief(AssignmentType assignment) {
		StringBuilder sb = new StringBuilder();
		if (assignment.getTarget() != null) {
			sb.append(assignment.getTarget().getName());
		} else {
			sb.append(assignment.getTargetRef().getOid());
		}
		if (assignment.getActivation() != null
				&& (assignment.getActivation().getValidFrom() != null || assignment.getActivation()
						.getValidTo() != null)) {
			sb.append(" ");
			sb.append("(");
			sb.append(formatTimeIntervalBrief(assignment));
			sb.append(")");
		}
		if (assignment.getActivation() != null) {
			switch (assignment.getActivation().getAdministrativeStatus()) {
				case ARCHIVED:
					sb.append(", archived");
					break; // TODO i18n
				case ENABLED:
					sb.append(", enabled");
					break;
				case DISABLED:
					sb.append(", disabled");
					break;
			}
		}
		return sb.toString();
	}

	public static String formatTimeIntervalBrief(AssignmentType assignment) {
		StringBuilder sb = new StringBuilder();
		if (assignment != null
				&& assignment.getActivation() != null
				&& (assignment.getActivation().getValidFrom() != null || assignment.getActivation()
						.getValidTo() != null)) {
			if (assignment.getActivation().getValidFrom() != null
					&& assignment.getActivation().getValidTo() != null) {
				sb.append(formatTime(assignment.getActivation().getValidFrom()));
				sb.append("-");
				sb.append(formatTime(assignment.getActivation().getValidTo()));
			} else if (assignment.getActivation().getValidFrom() != null) {
				sb.append("from ");
				sb.append(formatTime(assignment.getActivation().getValidFrom()));
			} else {
				sb.append("to ");
				sb.append(formatTime(assignment.getActivation().getValidTo()));
			}
		}
		return sb.toString();
	}

	private static String formatTime(XMLGregorianCalendar time) {
		DateFormat formatter = DateFormat.getDateInstance();
		return formatter.format(time.toGregorianCalendar().getTime());
	}


	boolean isItemVisible(ItemWrapper item) {
		ItemDefinition def = item.getItemDefinition();
		if (def.isIgnored() || def.isOperational()) {
			return false;
		}

		if (def instanceof PrismPropertyDefinition && skipProperty((PrismPropertyDefinition) def)) {
			return false;
		}

		// we decide not according to status of this container, but according to
		// the status of the whole object
		if (object.getStatus() == ContainerStatus.ADDING) {
			return def.canAdd();
		}

		// otherwise, object.getStatus() is MODIFYING

		if (def.canModify()) {
			return showEmpty(item);
		} else {
			if (def.canRead()) {
				return showEmpty(item);
			}
			return false;
		}
	}

	private boolean showEmpty(ItemWrapper item) {
		ObjectWrapper object = getObject();
			List<ValueWrapper> values = item.getValues();
			boolean isEmpty = values.isEmpty();
			if (values.size() == 1) {
				ValueWrapper value = values.get(0);
				if (ValueStatus.ADDED.equals(value.getStatus())) {
					isEmpty = true;
				}
			}
			return object.isShowEmpty() || !isEmpty;
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

	public boolean isMain() {
		return main;
	}

	public void setMain(boolean main) {
		this.main = main;
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
		for (ItemWrapper item : getItems()) {
			if (item.hasChanged()) {
				return true;
			}
		}

		return false;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getDisplayNameFromItem(container));
		builder.append(", ");
		builder.append(status);
		builder.append("\n");
		for (ItemWrapper wrapper : getItems()) {
			builder.append("\t");
			builder.append(wrapper.toString());
			builder.append("\n");
		}
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
		PrismContainerDefinition def = getItemDefinition();
		if (def != null) {
			// todo take into account the containing object status (adding vs. modifying)
			return (def.canRead() && !def.canAdd() && !def.canModify()); 
		}
		return readonly;
	}

	public void setReadonly(boolean readonly) {
		this.readonly = readonly;
	}

	@Override
	public List<ValueWrapper> getValues() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isVisible() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ContainerWrapper getContainer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addValue() {
		// TODO Auto-generated method stub

	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("ContainerWrapper(\n");
		DebugUtil.debugDumpWithLabel(sb, "displayName", displayName, indent+1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "status", status == null?null:status.toString(), indent+1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "main", main, indent+1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "readonly", readonly, indent+1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "showInheritedObjectAttributes", showInheritedObjectAttributes, indent+1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "path", path == null?null:path.toString(), indent+1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "containerDefinition", containerDefinition == null?null:containerDefinition.toString(), indent+1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "container", container==null?null:container.toString(), indent+1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "properties", properties, indent+1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "result", result, indent+1);
		sb.append("\n");
		DebugUtil.indentDebugDump(sb, indent);
		sb.append(")");
		return sb.toString();
	}
}
