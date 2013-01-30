/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.prism;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ConnectorTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ActivationCapabilityType;

import org.apache.commons.lang.Validate;
import org.apache.wicket.RestartResponseException;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.*;

/**
 * @author lazyman
 */
public class ObjectWrapper implements Serializable {

	private static final Trace LOGGER = TraceManager.getTrace(ObjectWrapper.class);
	private PrismObject object;
	private ObjectDelta oldDelta;
	private ContainerStatus status;
	private HeaderStatus headerStatus;
	private String displayName;
	private String description;
	private List<ContainerWrapper> containers;

	private boolean showEmpty;
	private boolean minimalized;
	private boolean selectable;
	private boolean selected;

    private boolean showAssignments = false;
    private boolean readonly = false;

	public ObjectWrapper(String displayName, String description, PrismObject object, ContainerStatus status) {
		Validate.notNull(object, "Object must not be null.");
		Validate.notNull(status, "Container status must not be null.");

		this.displayName = displayName;
		this.description = description;
		this.object = object;
		this.status = status;
	}

	public HeaderStatus getHeaderStatus() {
		if (headerStatus == null) {
			headerStatus = HeaderStatus.NORMAL;
		}
		return headerStatus;
	}
	
	public ObjectDelta getOldDelta() {
		return oldDelta;
	}
	
	public void setOldDelta(ObjectDelta oldDelta) {
		this.oldDelta = oldDelta;
	}
	
	public Boolean getEnableStatus() {
		ContainerWrapper activation = null;
		String containerName = "";
		ItemPath resourceActivationPath = new ItemPath(ResourceObjectShadowType.F_ACTIVATION);
		for (ContainerWrapper container : getContainers()) {
			containerName = container.getObject().getDisplayName();
			Class clazz = container.getItem().getCompileTimeClass();
			if(clazz != null) {
				if(clazz.equals(RoleType.class) || clazz.equals(ObjectType.class)) {
					return true;
				}
			}
			if (container.getPath() == null || !container.getPath().equals(resourceActivationPath)) {
				continue;
			} else if(container.getPath().equals(resourceActivationPath)) {
				activation = container;
				break;
			}
		}
        if (activation == null) {
        	return true;
        }
        
        PropertyWrapper enabledProperty = activation.findPropertyWrapper(ActivationType.F_ENABLED);
        if (enabledProperty.getValues().size() != 1) {
        	LOGGER.warn("No enabled property found for account " + getDisplayName() + ".");
        	return false;
        }
        ValueWrapper value = enabledProperty.getValues().get(0);
        return (Boolean) value.getValue().getValue();
	}

	public void setHeaderStatus(HeaderStatus headerStatus) {
		this.headerStatus = headerStatus;
	}

	public PrismObject getObject() {
		return object;
	}

	public String getDisplayName() {
		if (displayName == null) {
			return WebMiscUtil.getName(object);
		}
		return displayName;
	}

	public ContainerStatus getStatus() {
		return status;
	}

	public String getDescription() {
		return description;
	}

	public boolean isMinimalized() {
		return minimalized;
	}

	public void setMinimalized(boolean minimalized) {
		this.minimalized = minimalized;
	}

	public boolean isShowEmpty() {
		return showEmpty;
	}

	public void setShowEmpty(boolean showEmpty) {
		this.showEmpty = showEmpty;
	}

	public boolean isSelectable() {
		return selectable;
	}

	public void setSelectable(boolean selectable) {
		this.selectable = selectable;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public List<ContainerWrapper> getContainers() {
		if (containers == null) {
			containers = createContainers();
		}
		return containers;
	}

	public ContainerWrapper findContainerWrapper(ItemPath path) {
		for (ContainerWrapper wrapper : getContainers()) {
			if (path != null && path.equals(wrapper.getPath())) {
				return wrapper;
			} else {
				if (wrapper.getPath() == null) {
					return wrapper;
				}
			}
		}

		return null;
	}

	private List<ContainerWrapper> createCustomContainerWrapper(PrismObject object, QName name) {
		PrismContainer container = object.findContainer(name);
		ContainerStatus status = container == null ? ContainerStatus.ADDING : ContainerStatus.MODIFYING;
		List<ContainerWrapper> list = new ArrayList<ContainerWrapper>();
		if (container == null) {
			PrismContainerDefinition definition = object.getDefinition().findContainerDefinition(name);
			container = definition.instantiate();
		}

		list.add(new ContainerWrapper(this, container, status, new ItemPath(name)));
		list.addAll(createContainerWrapper(container, new ItemPath(name)));

		return list;
	}

	private List<ContainerWrapper> createContainers() {
		List<ContainerWrapper> containers = new ArrayList<ContainerWrapper>();

		try {
			Class clazz = object.getCompileTimeClass();
			if (ResourceObjectShadowType.class.isAssignableFrom(clazz)) {
				PrismContainer attributes = object.findContainer(ResourceObjectShadowType.F_ATTRIBUTES);
				ContainerStatus status = attributes != null ? getStatus() : ContainerStatus.ADDING;
				if (attributes == null) {
					PrismContainerDefinition definition = object.getDefinition().findContainerDefinition(
							ResourceObjectShadowType.F_ATTRIBUTES);
					attributes = definition.instantiate();
				}

				ContainerWrapper container = new ContainerWrapper(this, attributes, status, new ItemPath(
						ResourceObjectShadowType.F_ATTRIBUTES));
				
				container.setMain(true);
				containers.add(container);
				
				if (hasResourceCapability(((AccountShadowType) object.asObjectable()).getResource())){
					containers.addAll(createCustomContainerWrapper(object, ResourceObjectShadowType.F_ACTIVATION));
					if (AccountShadowType.class.isAssignableFrom(clazz)) {
						containers.addAll(createCustomContainerWrapper(object, AccountShadowType.F_CREDENTIALS));
					}
				}
            } else if (ResourceType.class.isAssignableFrom(clazz)) {
                containers =  createResourceContainers();
			} else {
				ContainerWrapper container = new ContainerWrapper(this, object, getStatus(), null);
				containers.add(container);

				containers.addAll(createContainerWrapper(object, null));
			}
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Error occurred during container wrapping", ex);
			throw new SystemException(ex.getMessage(), ex);
		}

		Collections.sort(containers, new ItemWrapperComparator());

		return containers;
	}

    private List<ContainerWrapper> createResourceContainers() throws SchemaException {
        List<ContainerWrapper> containers = new ArrayList<ContainerWrapper>();
        PrismObject<ConnectorType> connector = loadConnector();

        containers.add(createResourceContainerWrapper(SchemaConstants.ICF_CONFIGURATION_PROPERTIES, connector));
        containers.add(createResourceContainerWrapper(SchemaConstants.ICF_CONNECTOR_POOL_CONFIGURATION, connector));
        containers.add(createResourceContainerWrapper(SchemaConstants.ICF_TIMEOUTS, connector));

        return containers;
    }

    private PrismObject<ConnectorType> loadConnector() {
        PrismReference connectorRef = object.findReference(ResourceType.F_CONNECTOR_REF);
        return connectorRef.getValue().getObject();
        //todo reimplement
    }

    private ContainerWrapper createResourceContainerWrapper(QName name, PrismObject<ConnectorType> connector)
        throws SchemaException {

        PrismContainer container = object.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
        if (container != null && container.size() == 1 &&  container.getValue() != null) {
            PrismContainerValue value = container.getValue();
            container = value.findContainer(name);
        }

        ContainerStatus status = container != null ? ContainerStatus.MODIFYING : ContainerStatus.ADDING;
        if (container == null) {
            ConnectorType connectorType = connector.asObjectable();
            PrismSchema schema = ConnectorTypeUtil.getConnectorSchema(connectorType, connector.getPrismContext());
            PrismContainerDefinition definition = ConnectorTypeUtil.findConfigurationContainerDefintion(connectorType, schema);

            definition = definition.findContainerDefinition(new ItemPath(name));
            container =  definition.instantiate();
        }

        return new ContainerWrapper(this, container, status,
                new ItemPath(ResourceType.F_CONNECTOR_CONFIGURATION, name));
    }

	private List<ContainerWrapper> createContainerWrapper(PrismContainer parent, ItemPath path) {

		PrismContainerDefinition definition = parent.getDefinition();
		List<ContainerWrapper> wrappers = new ArrayList<ContainerWrapper>();

		List<ItemPathSegment> segments = new ArrayList<ItemPathSegment>();
		if (path != null) {
			segments.addAll(path.getSegments());
		}
		ItemPath parentPath = new ItemPath(segments);
		for (ItemDefinition def : (Collection<ItemDefinition>) definition.getDefinitions()) {
			if (!(def instanceof PrismContainerDefinition)) {
				continue;
			}

			PrismContainerDefinition containerDef = (PrismContainerDefinition) def;
			if (!showAssignments && AssignmentType.COMPLEX_TYPE.equals(containerDef.getTypeName())) {
				continue;
			}

			ItemPath newPath = createPropertyPath(parentPath, containerDef.getName());
			PrismContainer prismContainer = object.findContainer(def.getName());
			if (prismContainer != null) {
				wrappers.add(new ContainerWrapper(this, prismContainer, ContainerStatus.MODIFYING, newPath));
			} else {
				prismContainer = containerDef.instantiate();
				wrappers.add(new ContainerWrapper(this, prismContainer, ContainerStatus.ADDING, newPath));
			}

            if (!AssignmentType.COMPLEX_TYPE.equals(containerDef.getTypeName())) {      // do not show internals of Assignments (e.g. activation)
			    wrappers.addAll(createContainerWrapper(prismContainer, newPath));
            }
		}

		return wrappers;
	}

	private ItemPath createPropertyPath(ItemPath path, QName element) {
		List<ItemPathSegment> segments = new ArrayList<ItemPathSegment>();
		segments.addAll(path.getSegments());
		segments.add(new NameItemPathSegment(element));

		return new ItemPath(segments);
	}

	public void normalize() throws SchemaException {
		ObjectDelta delta = getObjectDelta();
		if (ChangeType.ADD.equals(delta.getChangeType())) {
			object = delta.getObjectToAdd();
		} else {
			delta.applyTo(object);
		}
	}

	public ObjectDelta getObjectDelta() throws SchemaException {
		if (ContainerStatus.ADDING.equals(getStatus())) {
			return createAddingObjectDelta();
		}

		ObjectDelta delta = new ObjectDelta(object.getCompileTimeClass(), ChangeType.MODIFY, object.getPrismContext());
		delta.setOid(object.getOid());

		List<ContainerWrapper> containers = getContainers();
		// sort containers by path size
		Collections.sort(containers, new PathSizeComparator());

		for (ContainerWrapper containerWrapper : getContainers()) {
			if (!containerWrapper.hasChanged()) {
				continue;
			}

			for (PropertyWrapper propertyWrapper : (List<PropertyWrapper>) containerWrapper.getProperties()) {
				if (!propertyWrapper.hasChanged()) {
					continue;
				}

				PrismPropertyDefinition propertyDef = propertyWrapper.getItem().getDefinition();

				ItemPath path = containerWrapper.getPath() != null ? containerWrapper.getPath()
						: new ItemPath();
				PropertyDelta pDelta = new PropertyDelta(path, propertyDef.getName(), propertyDef);
				for (ValueWrapper valueWrapper : propertyWrapper.getValues()) {
					ValueStatus valueStatus = valueWrapper.getStatus();
					if (!valueWrapper.hasValueChanged()
							&& (ValueStatus.NOT_CHANGED.equals(valueStatus) || ValueStatus.ADDED.equals(valueStatus))) {
						continue;
					}

					//TODO: need to check if the resource has defined capabilities
                    //todo this is bad hack because now we have not tri-state checkbox
					if (SchemaConstants.PATH_ACTIVATION.equals(path)) {
						
						if (object.asObjectable() instanceof AccountShadowType	&& (((ResourceObjectShadowType) object.asObjectable()).getActivation() == null || ((ResourceObjectShadowType) object
										.asObjectable()).getActivation().isEnabled() == null)) {
							
							if (!hasResourceCapability(((AccountShadowType) object.asObjectable()).getResource())){
								continue;
							}
						}
					}

					PrismPropertyValue val = valueWrapper.getValue();
					val.setOriginType(OriginType.USER_ACTION);
					switch (valueWrapper.getStatus()) {
					case ADDED:
						if (SchemaConstants.PATH_PASSWORD.equals(path)) {
							// password change will always look like add,
							// therefore we push replace
							pDelta.setValuesToReplace(Arrays.asList(val.clone()));
						} else {
							pDelta.addValueToAdd(val.clone());
						}
						break;
					case DELETED:
						pDelta.addValueToDelete(val.clone());
						break;
					case NOT_CHANGED:
						// this is modify...
						if (propertyDef.isSingleValue()) {
							if (val.getValue() != null) {
								pDelta.setValuesToReplace(Arrays.asList(val.clone()));
							} else {
								pDelta.addValueToDelete(valueWrapper.getOldValue().clone());
							}
						} else {
							if (val.getValue() != null) {
								pDelta.addValueToAdd(val.clone());
							}
							pDelta.addValueToDelete(valueWrapper.getOldValue().clone());
						}
						break;
					}
				}
				if (!pDelta.isEmpty()) {
					delta.addModification(pDelta);
				}
			}
		}

		return delta;
	}
	
	private boolean hasResourceCapability(ResourceType resource){
		if (resource == null){
			return false;
		}
		if (!ResourceTypeUtil.hasResourceNativeActivationCapability(resource)){
			return (ResourceTypeUtil.getEffectiveCapability(resource,  ActivationCapabilityType.class) == null) ? false : true; 
		} 
		
		return true;
	}

	private ObjectDelta createAddingObjectDelta() throws SchemaException {
		PrismObject object = this.object.clone();

		List<ContainerWrapper> containers = getContainers();
		// sort containers by path size
		Collections.sort(containers, new PathSizeComparator());

		for (ContainerWrapper containerWrapper : getContainers()) {
			if (!containerWrapper.hasChanged()) {
				continue;
			}

			PrismContainer container = containerWrapper.getItem();
			ItemPath path = containerWrapper.getPath();
			if (containerWrapper.getPath() != null) {
				container = container.clone();
				if (path.size() > 1) {
					ItemPath parentPath = path.allExceptLast();
					PrismContainer parent = object.findOrCreateContainer(parentPath);
					parent.add(container);
				} else {
					PrismContainer existing = object.findContainer(container.getName());
					if (existing == null) {
						object.add(container);
					} else {
						continue;
					}
				}
			} else {
				container = object;
			}

			for (PropertyWrapper propertyWrapper : (List<PropertyWrapper>) containerWrapper.getProperties()) {
				if (!propertyWrapper.hasChanged()) {
					continue;
				}

				PrismProperty property = propertyWrapper.getItem().clone();
				if (container.findProperty(property.getName()) != null) {
					continue;
				}
				container.add(property);
				for (ValueWrapper valueWrapper : propertyWrapper.getValues()) {
					if (!valueWrapper.hasValueChanged() || ValueStatus.DELETED.equals(valueWrapper.getStatus())) {
						continue;
					}

					if (property.hasRealValue(valueWrapper.getValue())) {
						continue;
					}
					property.addValue(valueWrapper.getValue().clone());
				}
			}
		}

		// cleanup empty containers
		cleanupEmptyContainers(object);

		ObjectDelta delta = ObjectDelta.createAddDelta(object);

		return delta;
	}

	private void cleanupEmptyContainers(PrismContainer container) {
		List<PrismContainerValue> values = container.getValues();

		List<PrismContainerValue> valuesToBeRemoved = new ArrayList<PrismContainerValue>();
		for (PrismContainerValue value : values) {
			List<? extends Item> items = value.getItems();
			if (items != null) {
				Iterator<? extends Item> iterator = items.iterator();
				while (iterator.hasNext()) {
					Item item = iterator.next();

					if (item instanceof PrismContainer) {
						cleanupEmptyContainers((PrismContainer) item);

						if (item.isEmpty()) {
							iterator.remove();
						}
					}
				}
			}

			if (items == null || value.isEmpty()) {
				valuesToBeRemoved.add(value);
			}
		}

		container.removeAll(valuesToBeRemoved);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(ContainerWrapper.getDisplayNameFromItem(object));
		builder.append(", ");
		builder.append(status);
		builder.append("\n");
		for (ContainerWrapper wrapper : getContainers()) {
			builder.append("\t");
			builder.append(wrapper.toString());
			builder.append("\n");
		}
		return builder.toString();
	}

	private static class PathSizeComparator implements Comparator<ContainerWrapper> {

		@Override
		public int compare(ContainerWrapper c1, ContainerWrapper c2) {
			int size1 = c1.getPath() != null ? c1.getPath().size() : 0;
			int size2 = c2.getPath() != null ? c2.getPath().size() : 0;

			return size1 - size2;
		}
	}

    public boolean isShowAssignments() {
        return showAssignments;
    }

    public void setShowAssignments(boolean showAssignments) {
        this.showAssignments = showAssignments;
    }

    public boolean isReadonly() {
        return readonly;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }
}
