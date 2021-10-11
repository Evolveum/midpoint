/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.delta;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.*;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import org.apache.commons.collections4.CollectionUtils;

import javax.xml.namespace.QName;
import java.util.*;

public class ContainerDeltaImpl<V extends Containerable> extends ItemDeltaImpl<PrismContainerValue<V>,PrismContainerDefinition<V>> implements
        ContainerDelta<V> {

    public ContainerDeltaImpl(PrismContainerDefinition itemDefinition, PrismContext prismContext) {
        super(itemDefinition, prismContext);
    }

    public ContainerDeltaImpl(ItemPath propertyPath, PrismContainerDefinition itemDefinition, PrismContext prismContext) {
        super(propertyPath, itemDefinition, prismContext);
    }

    public ContainerDeltaImpl(ItemPath parentPath, QName name, PrismContainerDefinition itemDefinition, PrismContext prismContext) {
        super(parentPath, name, itemDefinition, prismContext);
        // Extra check. It makes no sense to create container delta with object definition
        if (itemDefinition instanceof PrismObjectDefinition<?>) {
            throw new IllegalArgumentException("Cannot apply "+definition+" to container delta");
        }
    }

//    public ContainerDeltaImpl(QName name, PrismContainerDefinition itemDefinition, PrismContext prismContext) {
//        super(name, itemDefinition, prismContext);
//        // Extra check. It makes no sense to create container delta with object definition
//        if (itemDefinition instanceof PrismObjectDefinition<?>) {
//            throw new IllegalArgumentException("Cannot apply "+definition+" to container delta");
//        }
//    }

    @Override
    public Class<PrismContainer> getItemClass() {
        return PrismContainer.class;
    }

    /**
     * Returns all values regardless of whether they are added or removed or replaced.
     * Useful for iterating over all the changed values.
     */
    public <T extends Containerable> Collection<PrismContainerValue<T>> getValues(Class<T> type) {
        checkConsistence();
        if (valuesToReplace != null) {
            return (Collection) valuesToReplace;
        }
        return (Collection) MiscUtil.union(valuesToAdd, valuesToDelete);
    }

    @Override
    public void setDefinition(PrismContainerDefinition<V> definition) {
        if (!(definition instanceof PrismContainerDefinition)) {
            throw new IllegalArgumentException("Cannot apply "+definition+" to container delta");
        }
        // Extra check. It makes no sense to create container delta with object definition
        if (definition instanceof PrismObjectDefinition<?>) {
            throw new IllegalArgumentException("Cannot apply "+definition+" to container delta");
        }
        super.setDefinition(definition);
    }

    @Override
    public void applyDefinition(PrismContainerDefinition<V> definition) throws SchemaException {
        if (!(definition instanceof PrismContainerDefinition)) {
            throw new IllegalArgumentException("Cannot apply definition "+definition+" to container delta "+this);
        }
        super.applyDefinition(definition);
    }

    @Override
    public boolean hasCompleteDefinition() {
        if (!super.hasCompleteDefinition()) {
            return false;
        }
        if (!hasCompleteDefinition(getValuesToAdd())) {
            return false;
        }
        if (!hasCompleteDefinition(getValuesToDelete())) {
            return false;
        }
        if (!hasCompleteDefinition(getValuesToReplace())) {
            return false;
        }
        return true;
    }

    private boolean hasCompleteDefinition(Collection<PrismContainerValue<V>> values) {
        if (values == null) {
            return true;
        }
        for (PrismContainerValue<V> value: values) {
            if (!value.hasCompleteDefinition()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Class<V> getCompileTimeClass() {
        if (getDefinition() != null) {
            return getDefinition().getCompileTimeClass();
        }
        return null;
    }

    @Override
    public boolean isApplicableToType(Item item) {
        return item instanceof PrismContainer;
    }

    @Override
    public ItemDelta<?,?> getSubDelta(ItemPath path) {
        if (path.isEmpty()) {
            return this;
        }
        Long id = null;
        if (path.startsWithId()) {
            id = path.firstToIdOrNull();
            path = path.rest();
        }
        ItemDefinition itemDefinition = getDefinition().findItemDefinition(path);
        if (itemDefinition == null) {
            throw new IllegalStateException("No definition of " + path + " in " + getDefinition());
        }
        ItemDelta<?,?> itemDelta = itemDefinition.createEmptyDelta(getPath().append(path));
        itemDelta.addValuesToAdd(findItemValues(id, path, getValuesToAdd()));
        itemDelta.addValuesToDelete(findItemValues(id, path, getValuesToDelete()));
        itemDelta.setValuesToReplace(findItemValues(id, path, getValuesToReplace()));
        if (itemDelta.isEmpty()) {
            return null;
        }
        return itemDelta;
    }

    private Collection findItemValues(Long id, ItemPath path, Collection<PrismContainerValue<V>> cvalues) {
        if (cvalues == null) {
            return null;
        }
        boolean foundValuesOnPath = false;
        Collection<PrismValue> subValues = new ArrayList<>();
        for (PrismContainerValue<V> cvalue: cvalues) {
            if (id == null || id.equals(cvalue.getId())) {
                Item<?,?> item = cvalue.findItem(path);
                if (item != null) {
                    subValues.addAll(PrismValueCollectionsUtil.cloneCollection(item.getValues()));
                    foundValuesOnPath = true;
                }
            }
        }
        return foundValuesOnPath ? subValues : null;
    }

    /**
     * Post processing of delta to expand missing values from the object. E.g. a delete deltas may
     * be "id-only" so they contain only id of the value to delete. In such case locate the full value
     * in the object and fill it into the delta.
     * This method may even delete id-only values that are no longer present in the object.
     */
    public <O extends Objectable> void expand(PrismObject<O> object, Trace logger) throws SchemaException {
        if (valuesToDelete != null) {
            ItemPath path = this.getPath();
            PrismContainer<Containerable> container = null;
            if (object != null) {
                container = object.findContainer(path);
            }
            Iterator<PrismContainerValue<V>> iterator = valuesToDelete.iterator();
            while (iterator.hasNext()) {
                PrismContainerValue<V> deltaCVal = iterator.next();
                if (CollectionUtils.isEmpty(deltaCVal.getItems())) {
                    Long id = deltaCVal.getId();
                    if (id == null) {
                        throw new IllegalArgumentException("No id and no items in value "+deltaCVal+" in delete set in "+this);
                    }
                    if (container != null) {
                        PrismContainerValue<Containerable> containerCVal = container.findValue(id);
                        if (containerCVal != null) {
                            for (Item<?,?> containerItem: containerCVal.getItems()) {
                                deltaCVal.add(containerItem.clone());
                            }
                            continue;
                        }
                    }
                    // id-only value with ID that is not in the object any more: delete the value from delta
                    iterator.remove();
                }
            }
        }
    }

    @Override
    protected boolean isValueEquivalent(PrismContainerValue<V> a, PrismContainerValue<V> b) {
        if (!super.isValueEquivalent(a, b)) {
            return false;
        }
        if (a.getId() == null || b.getId() == null) {
            return true;
        } else {
            return a.getId().equals(b.getId());
        }
    }

    @Override
    public void checkConsistence(boolean requireDefinition, boolean prohibitRaw, ConsistencyCheckScope scope) {
        super.checkConsistence(requireDefinition, prohibitRaw, scope);
        checkDuplicateId(valuesToAdd);
    }

    private void checkDuplicateId(Collection<PrismContainerValue<V>> valuesToAdd) {
        if (valuesToAdd == null || valuesToAdd.isEmpty()) {
            return;
        }
        Set<Long> idsToAdd = new HashSet<>();
        for (PrismContainerValue<V> valueToAdd : valuesToAdd) {
            Long id = valueToAdd.getId();
            if (id != null) {
                if (idsToAdd.contains(id)) {
                    throw new IllegalArgumentException("Trying to add prism container value with id " + id + " multiple times");
                } else {
                    idsToAdd.add(id);
                }
            }
        }
    }

    @Override
    public ContainerDeltaImpl<V> clone() {
        ContainerDeltaImpl<V> clone = new ContainerDeltaImpl<>(getElementName(), getDefinition(), getPrismContext());
        copyValues(clone);
        return clone;
    }

    protected void copyValues(ContainerDeltaImpl<V> clone) {
        super.copyValues(clone);
    }

    public static <T extends Containerable,O extends Objectable> ContainerDeltaImpl<T> createDelta(ItemPath containerPath,
            Class<O> type, PrismContext prismContext) {
        PrismObjectDefinition<O> objectDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
        return createDelta(containerPath, objectDefinition);
    }

    public static <T extends Containerable,O extends Objectable> ContainerDeltaImpl<T> createDelta(ItemPath containerPath,
            PrismObjectDefinition<O> objectDefinition) {
        PrismContainerDefinition<T> containerDefinition = objectDefinition.findContainerDefinition(containerPath);
        if (containerDefinition == null) {
            throw new IllegalArgumentException("No definition for "+containerPath+" in "+objectDefinition);
        }
        ContainerDeltaImpl<T> delta = new ContainerDeltaImpl<>(containerPath, containerDefinition, objectDefinition.getPrismContext());
        return delta;
    }

    public static <T extends Containerable,O extends Objectable> ContainerDeltaImpl<T> createDelta(ItemPath containerPath,
            PrismContainerDefinition<O> objectDefinition) {
        PrismContainerDefinition<T> containerDefinition = objectDefinition.findContainerDefinition(containerPath);
        if (containerDefinition == null) {
            throw new IllegalArgumentException("No definition for "+containerPath+" in "+objectDefinition);
        }
        ContainerDeltaImpl<T> delta = new ContainerDeltaImpl<>(containerPath, containerDefinition, objectDefinition.getPrismContext());
        return delta;
    }

    public static <T extends Containerable,O extends Objectable> ContainerDeltaImpl<T> createModificationAdd(
            ItemPath containerPath,
            Class<O> type, PrismContext prismContext, T containerable) throws SchemaException {
        return createModificationAdd(containerPath, type, prismContext, containerable.asPrismContainerValue());
    }

    public static <T extends Containerable,O extends Objectable> ContainerDeltaImpl<T> createModificationAdd(
            ItemPath containerPath,
            Class<O> type, PrismContext prismContext, PrismContainerValue<T> cValue) throws SchemaException {
        ContainerDeltaImpl<T> delta = createDelta(containerPath, type, prismContext);
        prismContext.adopt(cValue, type, containerPath);
        delta.addValuesToAdd(cValue);
        return delta;
    }

    public static <T extends Containerable,O extends Objectable> ContainerDeltaImpl<T> createModificationDelete(
            ItemPath containerPath,
            Class<O> type, PrismContext prismContext, T containerable) throws SchemaException {
        return createModificationDelete(containerPath, type, prismContext, containerable.asPrismContainerValue());
    }

    public static <T extends Containerable,O extends Objectable> ContainerDeltaImpl<T> createModificationDelete(
            ItemPath containerPath,
            Class<O> type, PrismContext prismContext, PrismContainerValue<T> cValue) throws SchemaException {
        ContainerDeltaImpl<T> delta = createDelta(containerPath, type, prismContext);
        prismContext.adopt(cValue, type, containerPath);
        delta.addValuesToDelete(cValue);
        return delta;
    }

    public static <T extends Containerable,O extends Objectable> ContainerDeltaImpl<T> createModificationReplace(
            ItemPath containerPath,
            Class<O> type, PrismContext prismContext, T containerable) throws SchemaException {
        return createModificationReplace(containerPath, type, prismContext, containerable != null ? containerable.asPrismContainerValue() : null);
    }

    public static <T extends Containerable,O extends Objectable> ContainerDeltaImpl<T> createModificationReplace(ItemPath containerPath,
            Class<O> type, PrismContext prismContext, Collection<T> containerables) throws SchemaException {
        ContainerDeltaImpl<T> delta = createDelta(containerPath, type, prismContext);
        List<PrismContainerValue<T>> pcvs = new ArrayList<>();
        for (Containerable c: containerables) {
            pcvs.add(c.asPrismContainerValue());
        }
        delta.setValuesToReplace(pcvs);
        return delta;
    }

    public static <T extends Containerable,O extends Objectable> ContainerDeltaImpl<T> createModificationReplace(
            ItemPath containerPath, Class<O> type, PrismContext prismContext, PrismContainerValue<T> cValue)
            throws SchemaException {
        ContainerDeltaImpl<T> delta = createDelta(containerPath, type, prismContext);
        if (cValue != null) {
            prismContext.adopt(cValue, type, containerPath);
            delta.setValuesToReplace(cValue);
        } else {
            delta.setValuesToReplace();
        }
        return delta;
    }

//    // cValue should be parent-less
//    public static <T extends Containerable> ContainerDeltaImpl<T> createModificationReplace(QName containerName, PrismContainerDefinition containerDefinition, PrismContainerValue<T> cValue) throws SchemaException {
//        ContainerDeltaImpl<T> delta = createDelta(ItemName.fromQName(containerName), containerDefinition);
//        delta.setValuesToReplace(cValue);
//        return delta;
//    }

    // cValues should be parent-less
    @Deprecated // Remove in 4.2
    public static Collection<? extends ItemDelta> createModificationReplaceContainerCollection(ItemName containerName,
                                                                                               PrismObjectDefinition<?> objectDefinition, PrismContainerValue... cValues) {
        Collection<? extends ItemDelta> modifications = new ArrayList<>(1);
        ContainerDeltaImpl delta = createDelta(ItemName.fromQName(containerName), objectDefinition);
        delta.setValuesToReplace(cValues);
        ((Collection)modifications).add(delta);
        return modifications;
    }

    // cValues should be parent-less
    @Deprecated // Remove in 4.2
    public static <T extends Containerable> ContainerDeltaImpl<T> createModificationReplace(ItemName containerName, PrismObjectDefinition<?> objectDefinition, PrismContainerValue... cValues) {
        ContainerDeltaImpl delta = createDelta(ItemName.fromQName(containerName), objectDefinition);
        delta.setValuesToReplace(cValues);
        return delta;
    }

    @Override
    protected void dumpValues(StringBuilder sb, String label, Collection<PrismContainerValue<V>> values, int indent) {
        DebugUtil.debugDumpLabel(sb, label, indent);
        if (values == null) {
            sb.append(" (null)");
        } else if (values.isEmpty()) {
            sb.append(" (empty)");
        } else {
            for (PrismContainerValue<V> val: values) {
                sb.append("\n");
                sb.append(val.debugDump(indent+1));
            }
        }
    }

    @Override
    public void acceptParentVisitor(Visitor visitor) {
        visitor.visit(this);
    }
}
