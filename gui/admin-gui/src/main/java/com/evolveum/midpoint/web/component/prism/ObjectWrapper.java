/*
 * Copyright (c) 2010-2016 Evolveum
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

import com.evolveum.midpoint.common.InternalsConfig;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;

import java.io.Serializable;
import java.util.*;

/**
 * @author lazyman
 */
public class ObjectWrapper<O extends ObjectType> implements Serializable, Revivable, DebugDumpable {

    public static final String F_DISPLAY_NAME = "displayName";
    public static final String F_SELECTED = "selected";

    private static final Trace LOGGER = TraceManager.getTrace(ObjectWrapper.class);

    private PrismObject<O> object;
    private PrismObject<O> objectOld;
    private ObjectDelta<O> oldDelta;
    private ContainerStatus status;
    private HeaderStatus headerStatus;
    private String displayName;
    private String description;
    private List<ContainerWrapper<? extends Containerable>> containers;

    private boolean showEmpty;
    private boolean minimalized;
    private boolean selectable;
    private boolean selected;

    private boolean showAssignments = false;
    // whether to show name and description properties and metadata container
    private boolean showInheritedObjectAttributes = true;
    private boolean readonly = false;

    private OperationResult result;
    private boolean protectedAccount;

    private List<PrismContainerValue<ShadowAssociationType>> associations;
    private Collection<PrismObject<OrgType>> parentOrgs = new ArrayList<>();

    private OperationResult fetchResult;
    // a "static" (non-refined) definition that reflects editability of the object in terms of midPoint schema limitations and security
    private PrismContainerDefinition objectDefinitionForEditing;
    // a refined definition of an resource object class that reflects its editability; applicable for shadows only
    private RefinedObjectClassDefinition objectClassDefinitionForEditing;

    public ObjectWrapper(String displayName, String description, PrismObject object,
                         PrismContainerDefinition objectDefinitionForEditing, ContainerStatus status) {
        this(displayName, description, object, objectDefinitionForEditing, null, status, false);
    }

    // delayContainerCreation is used in cases where caller wants to configure
    // those aspects of the wrapper that must be set before container creation
    public ObjectWrapper(String displayName, String description, PrismObject object,
                         PrismContainerDefinition objectDefinitionForEditing,
                         RefinedObjectClassDefinition objectClassDefinitionForEditing, ContainerStatus status,
                         boolean delayContainerCreation) {
        Validate.notNull(object, "Object must not be null.");
        Validate.notNull(status, "Container status must not be null.");

        this.displayName = displayName;
        this.description = description;
        this.object = object;
        this.objectOld = object.clone();
        this.status = status;
        this.objectDefinitionForEditing = objectDefinitionForEditing;
        this.objectClassDefinitionForEditing = objectClassDefinitionForEditing;
    }

    public void initializeContainers(PageBase pageBase) {
        //todo remove
    }

    public void revive(PrismContext prismContext) throws SchemaException {
        if (object != null) {
            object.revive(prismContext);
        }
        if (oldDelta != null) {
            oldDelta.revive(prismContext);
        }
        if (containers != null) {
            for (ContainerWrapper containerWrapper : containers) {
                containerWrapper.revive(prismContext);
            }
        }
    }

    public List<PrismContainerValue<ShadowAssociationType>> getAssociations() {
        return associations;
    }

    public void setAssociations(List<PrismContainerValue<ShadowAssociationType>> associations) {
        this.associations = associations;
    }

    public Collection<PrismObject<OrgType>> getParentOrgs() {
        return parentOrgs;
    }

    public OperationResult getFetchResult() {
        return fetchResult;
    }

    public void setFetchResult(OperationResult fetchResult) {
        this.fetchResult = fetchResult;
    }

    void setResult(OperationResult result) {
        this.result = result;
    }

    public OperationResult getResult() {
        return result;
    }

    public void clearResult() {
        result = null;
    }

    public HeaderStatus getHeaderStatus() {
        if (headerStatus == null) {
            headerStatus = HeaderStatus.NORMAL;
        }
        return headerStatus;
    }

    public ObjectDelta<O> getOldDelta() {
        return oldDelta;
    }

    public void setOldDelta(ObjectDelta<O> oldDelta) {
        this.oldDelta = oldDelta;
    }

    public void setHeaderStatus(HeaderStatus headerStatus) {
        this.headerStatus = headerStatus;
    }

    public PrismObject<O> getObject() {
        return object;
    }

    public PrismObject<O> getObjectOld() {
        return objectOld;
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

    public List<ContainerWrapper<? extends Containerable>> getContainers() {
        if (containers == null) {
            containers = new ArrayList<>();
        }
        return containers;
    }

    public void setContainers(List<ContainerWrapper<? extends Containerable>> containers) {
        this.containers = containers;
    }

    public ContainerWrapper findContainerWrapper(ItemPath path) {
        for (ContainerWrapper wrapper : getContainers()) {
            if (path != null) {
                if (path.equivalent(wrapper.getPath())) {
                    return wrapper;
                }
            } else {
                if (wrapper.getPath() == null) {
                    return wrapper;
                }
            }
        }

        return null;
    }
    
    public ContainerWrapper findMainContainerWrapper() {
        for (ContainerWrapper wrapper : getContainers()) {
        	if (wrapper.isMain()) {
        		return wrapper;
        	}
        }
        return null;
    }
    
    public <IW extends ItemWrapper> IW findPropertyWrapper(ItemPath path) {
    	ContainerWrapper containerWrapper;
    	ItemPath propertyPath;
    	if (path.size() == 1) {
    		containerWrapper = findMainContainerWrapper();
    		propertyPath = path;
    	} else {
    		containerWrapper = findContainerWrapper(path.head());
    		propertyPath = path.tail();
    	}
    	if (containerWrapper == null) {
    		return null;
    	}
    	return (IW) containerWrapper.findPropertyWrapper(ItemPath.getFirstName(propertyPath));
    }

    public void normalize() throws SchemaException {
        ObjectDelta delta = getObjectDelta();
        if (ChangeType.ADD.equals(delta.getChangeType())) {
            object = delta.getObjectToAdd();
        } else {
            delta.applyTo(object);
        }
    }

    public ObjectDelta<O> getObjectDelta() throws SchemaException {
        if (ContainerStatus.ADDING.equals(getStatus())) {
            return createAddingObjectDelta();
        }

        ObjectDelta<O> delta = new ObjectDelta<O>(object.getCompileTimeClass(), ChangeType.MODIFY,
                object.getPrismContext());
        delta.setOid(object.getOid());

        List<ContainerWrapper<? extends Containerable>> containers = getContainers();
        // sort containers by path size
        Collections.sort(containers, new PathSizeComparator());

        for (ContainerWrapper containerWrapper : getContainers()) {
            //create ContainerDelta for association container
            //HACK HACK HACK create correct procession for association container data
            //according to its structure
            if (containerWrapper.getItemDefinition().getName().equals(ShadowType.F_ASSOCIATION)) {
                ContainerDelta<ShadowAssociationType> associationDelta = ContainerDelta.createDelta(ShadowType.F_ASSOCIATION, containerWrapper.getItemDefinition());
                List<AssociationWrapper> associationItemWrappers = (List<AssociationWrapper>) containerWrapper.getItems();
                for (AssociationWrapper associationItemWrapper : associationItemWrappers) {
                    List<ValueWrapper> assocValueWrappers = associationItemWrapper.getValues();
                    for (ValueWrapper assocValueWrapper : assocValueWrappers) {
                        PrismContainerValue<ShadowAssociationType> assocValue = (PrismContainerValue<ShadowAssociationType>) assocValueWrapper.getValue();
                        if (assocValueWrapper.getStatus() == ValueStatus.DELETED) {
                            associationDelta.addValueToDelete(assocValue.clone());
                        } else if (assocValueWrapper.getStatus().equals(ValueStatus.ADDED)) {
                            associationDelta.addValueToAdd(assocValue.clone());
                        }
                    }
                }
                delta.addModification(associationDelta);
            } else {
                if (!containerWrapper.hasChanged()) {
                    continue;
                }

                for (ItemWrapper itemWrapper : (List<ItemWrapper>) containerWrapper.getItems()) {
                    if (!itemWrapper.hasChanged()) {
                        continue;
                    }
                    ItemPath containerPath = containerWrapper.getPath() != null ? containerWrapper.getPath() : new ItemPath();
                    if (itemWrapper instanceof PropertyWrapper) {
                        ItemDelta pDelta = computePropertyDeltas((PropertyWrapper) itemWrapper, containerPath);
                        if (!pDelta.isEmpty()) {
                            delta.addModification(pDelta);
                        }
                    }

                    if (itemWrapper instanceof ReferenceWrapper) {
                        ReferenceDelta pDelta = computeReferenceDeltas((ReferenceWrapper) itemWrapper, containerPath);
                        if (!pDelta.isEmpty()) {
                            delta.addModification(pDelta);
                        }
                    }

                }
            }
        }
        // returning container to previous order
        Collections.sort(containers, new ItemWrapperComparator());

        // Make sure we have all the definitions
        object.getPrismContext().adopt(delta);
        return delta;
    }

    private ItemDelta computePropertyDeltas(PropertyWrapper propertyWrapper, ItemPath containerPath) {
        ItemDefinition itemDef = propertyWrapper.getItem().getDefinition();
        ItemDelta pDelta = itemDef.createEmptyDelta(containerPath.subPath(itemDef.getName()));
        addItemDelta(propertyWrapper, pDelta, itemDef, containerPath);
        return pDelta;

    }

    private ReferenceDelta computeReferenceDeltas(ReferenceWrapper referenceWrapper, ItemPath containerPath) {
        PrismReferenceDefinition propertyDef = referenceWrapper.getItem().getDefinition();
        ReferenceDelta pDelta = new ReferenceDelta(containerPath, propertyDef.getName(), propertyDef,
                propertyDef.getPrismContext());
        addItemDelta(referenceWrapper, pDelta, propertyDef, containerPath.subPath(propertyDef.getName()));
        return pDelta;

    }

    private void addItemDelta(ItemWrapper itemWrapper, ItemDelta pDelta, ItemDefinition propertyDef,
                              ItemPath containerPath) {
        for (ValueWrapper valueWrapper : itemWrapper.getValues()) {
            valueWrapper.normalize(propertyDef.getPrismContext());
            ValueStatus valueStatus = valueWrapper.getStatus();
            if (!valueWrapper.hasValueChanged()
                    && (ValueStatus.NOT_CHANGED.equals(valueStatus) || ValueStatus.ADDED.equals(valueStatus))) {
                continue;
            }

            // TODO: need to check if the resource has defined
            // capabilities
            // todo this is bad hack because now we have not tri-state
            // checkbox
            if (SchemaConstants.PATH_ACTIVATION.equivalent(containerPath)) {

                if (object.asObjectable() instanceof ShadowType
                        && (((ShadowType) object.asObjectable()).getActivation() == null || ((ShadowType) object
                        .asObjectable()).getActivation().getAdministrativeStatus() == null)) {

                    if (!hasResourceCapability(((ShadowType) object.asObjectable()).getResource(),
                            ActivationCapabilityType.class)) {
                        continue;
                    }
                }
            }

            PrismValue newValCloned = clone(valueWrapper.getValue());
            PrismValue oldValCloned = clone(valueWrapper.getOldValue());
            switch (valueWrapper.getStatus()) {
                case ADDED:
                    if (newValCloned != null) {
                        if (SchemaConstants.PATH_PASSWORD.equivalent(containerPath)) {
                            // password change will always look like
                            // add,
                            // therefore we push replace
                            pDelta.setValuesToReplace(Arrays.asList(newValCloned));
                        } else if (propertyDef.isSingleValue()) {
                            // values for single-valued properties
                            // should be pushed via replace
                            // in order to prevent problems e.g. with
                            // summarizing deltas for
                            // unreachable resources
                            pDelta.setValueToReplace(newValCloned);
                        } else {
                            pDelta.addValueToAdd(newValCloned);
                        }
                    }
                    break;
                case DELETED:
                    if (newValCloned != null) {
                        pDelta.addValueToDelete(newValCloned);
                    }
                    if (oldValCloned != null) {
                        pDelta.addValueToDelete(oldValCloned);
                    }
                    break;
                case NOT_CHANGED:
                    // this is modify...
                    if (propertyDef.isSingleValue()) {
                        // newValCloned.isEmpty()
                        if (newValCloned != null && !newValCloned.isEmpty()) {
                            pDelta.setValuesToReplace(Arrays.asList(newValCloned));
                        } else {
                            if (oldValCloned != null) {
                                pDelta.addValueToDelete(oldValCloned);
                            }
                        }
                    } else {
                        if (newValCloned != null && !newValCloned.isEmpty()) {
                            pDelta.addValueToAdd(newValCloned);
                        }
                        if (oldValCloned != null) {
                            pDelta.addValueToDelete(oldValCloned);
                        }
                    }
                    break;
            }
        }
    }

    private PrismValue clone(PrismValue value) {
        if (value == null) {
            return null;
        }
        PrismValue cloned = value.clone();
        cloned.setOriginType(OriginType.USER_ACTION);
        if (value instanceof PrismPropertyValue) {
            PrismPropertyValue ppValue = (PrismPropertyValue) value;
            if (ppValue.getValue() instanceof ProtectedStringType) {
                ((PrismPropertyValue) cloned).setValue(((ProtectedStringType) ppValue.getValue()).clone());
            }
            if (ppValue.getValue() instanceof PolyString) {
                PolyString poly = (PolyString) ppValue.getValue();
                if (StringUtils.isEmpty(poly.getOrig())) {
                    return null;
                }
                ((PrismPropertyValue) cloned).setValue(new PolyString(poly.getOrig(), poly.getNorm()));
            }
        } else if (value instanceof PrismReferenceValue) {
            if (cloned == null) {
                return null;
            }
            if (cloned.isEmpty()) {
                return null;
            }
        }

        return cloned;
    }

    private boolean hasResourceCapability(ResourceType resource,
                                          Class<? extends CapabilityType> capabilityClass) {
        if (resource == null) {
            return false;
        }
        return ResourceTypeUtil.hasEffectiveCapability(resource, capabilityClass);
    }

    private ObjectDelta createAddingObjectDelta() throws SchemaException {
        PrismObject object = this.object.clone();

        List<ContainerWrapper<? extends Containerable>> containers = getContainers();
        // sort containers by path size
        Collections.sort(containers, new PathSizeComparator());

        for (ContainerWrapper containerWrapper : getContainers()) {

            if (containerWrapper.getItemDefinition().getName().equals(ShadowType.F_ASSOCIATION)) {
                PrismContainer associationContainer = object.findOrCreateContainer(ShadowType.F_ASSOCIATION);
                List<AssociationWrapper> associationItemWrappers = (List<AssociationWrapper>) containerWrapper.getItems();
                for (AssociationWrapper associationItemWrapper : associationItemWrappers) {
                    List<ValueWrapper> assocValueWrappers = associationItemWrapper.getValues();
                    for (ValueWrapper assocValueWrapper : assocValueWrappers) {
                        PrismContainerValue<ShadowAssociationType> assocValue = (PrismContainerValue<ShadowAssociationType>) assocValueWrapper.getValue();
                        associationContainer.add(assocValue.clone());
                    }
                }
                continue;
            }

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
                    PrismContainer existing = object.findContainer(container.getElementName());
                    if (existing == null) {
                        object.add(container);
                    } else {
                        continue;
                    }
                }
            } else {
                container = object;
            }

            for (ItemWrapper propertyWrapper : (List<ItemWrapper>) containerWrapper.getItems()) {
                if (!propertyWrapper.hasChanged()) {
                    continue;
                }

                Item property = propertyWrapper.getItem().clone();
                if (container.findProperty(property.getElementName()) != null) {
                    continue;
                }
                for (ValueWrapper valueWrapper : propertyWrapper.getValues()) {
                    valueWrapper.normalize(object.getPrismContext());
                    if (!valueWrapper.hasValueChanged()
                            || ValueStatus.DELETED.equals(valueWrapper.getStatus())) {
                        continue;
                    }

                    if (property.hasRealValue(valueWrapper.getValue())) {
                        continue;
                    }

                    PrismValue cloned = clone(valueWrapper.getValue());
                    if (cloned != null) {
                        property.add(cloned);
                    }
                }

                if (!property.isEmpty()) {
                    container.add(property);
                }
            }
        }

        // cleanup empty containers
        cleanupEmptyContainers(object);

        ObjectDelta delta = ObjectDelta.createAddDelta(object);

        // returning container to previous order
        Collections.sort(containers, new ItemWrapperComparator());

        if (InternalsConfig.consistencyChecks) {
            delta.checkConsistence(true, true, true, ConsistencyCheckScope.THOROUGH);
        }

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
        builder.append("ObjectWrapper(");
        builder.append(ContainerWrapper.getDisplayNameFromItem(object));
        builder.append(" (");
        builder.append(status);
        builder.append(") ");
        builder.append(getContainers() == null ? null : getContainers().size());
        builder.append(" containers)");
        return builder.toString();
    }

    public boolean isProtectedAccount() {
        if (object == null || !(ShadowType.class.isAssignableFrom(object.getCompileTimeClass()))) {
            return false;
        }

        PrismProperty<Boolean> protectedObject = object.findProperty(ShadowType.F_PROTECTED_OBJECT);
        if (protectedObject == null) {
            return false;
        }

        return protectedObject.getRealValue() != null ? protectedObject.getRealValue() : false;
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
        if (isProtectedAccount()) {
            return true;
        }
        return readonly;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }

    public boolean isShowInheritedObjectAttributes() {
        return showInheritedObjectAttributes;
    }

    public void setShowInheritedObjectAttributes(boolean showInheritedObjectAttributes) {
        this.showInheritedObjectAttributes = showInheritedObjectAttributes;
    }

    public PrismContainerDefinition getDefinition() {
        if (objectDefinitionForEditing != null) {
            return objectDefinitionForEditing;
        }
        return object.getDefinition();
    }

    public PrismContainerDefinition getRefinedAttributeDefinition() {
        if (objectClassDefinitionForEditing != null) {
            return objectClassDefinitionForEditing.toResourceAttributeContainerDefinition();
        }
        return null;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("ObjectWrapper(\n");
        DebugUtil.debugDumpWithLabel(sb, "displayName", displayName, indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "description", description, indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "object", object == null ? null : object.toString(), indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "objectOld", objectOld == null ? null : objectOld.toString(), indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "oldDelta", oldDelta, indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "status", status == null ? null : status.toString(), indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "headerStatus", headerStatus == null ? null : headerStatus.toString(), indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "containers", containers, indent + 1);
        sb.append("\n");
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(")");
        return sb.toString();
    }
}
