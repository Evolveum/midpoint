/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import org.apache.commons.collections4.CollectionUtils;

/**
 * @author katka
 *
 */
public class PrismContainerWrapperImpl<C extends Containerable> extends ItemWrapperImpl<PrismContainerValue<C>, PrismContainer<C>, PrismContainerDefinition<C>, PrismContainerValueWrapper<C>> implements PrismContainerWrapper<C>, Serializable{

    private static final long serialVersionUID = 1L;

    private static final transient Trace LOGGER = TraceManager.getTrace(PrismContainerWrapperImpl.class);

    private boolean expanded;

    private boolean virtual;


    public PrismContainerWrapperImpl(PrismContainerValueWrapper<?> parent, PrismContainer<C> item, ItemStatus status) {
        super(parent, item, status);
        //TODO move to factory
        this.expanded = !item.isEmpty();
    }

    @Override
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    @Override
    public boolean isExpanded() {
        return expanded;
    }

    @Override
    public Class<C> getCompileTimeClass() {
        return getItemDefinition().getCompileTimeClass();
    }

    @Override
    public ComplexTypeDefinition getComplexTypeDefinition() {
        return getItemDefinition().getComplexTypeDefinition();
    }

    @Override
    public String getDefaultNamespace() {
        return getItemDefinition().getDefaultNamespace();
    }

    @Override
    public List<String> getIgnoredNamespaces() {
        return getItemDefinition().getIgnoredNamespaces();
    }

    @Override
    public List<? extends ItemDefinition> getDefinitions() {
        return getItemDefinition().getDefinitions();
    }

    @Override
    public List<PrismPropertyDefinition> getPropertyDefinitions() {
        return getItemDefinition().getPropertyDefinitions();
    }

    @Override
    public ContainerDelta<C> createEmptyDelta(ItemPath path) {
        return getItemDefinition().createEmptyDelta(path);
    }

    @Override
    public PrismContainerDefinition<C> clone() {
        return getItemDefinition().clone();
    }

    @Override
    public PrismContainerDefinition<C> cloneWithReplacedDefinition(QName itemName, ItemDefinition newDefinition) {
        return getItemDefinition().cloneWithReplacedDefinition(itemName, newDefinition);
    }

    @Override
    public void replaceDefinition(QName itemName, ItemDefinition newDefinition) {
        getItemDefinition().replaceDefinition(itemName, newDefinition);
    }

    @Override
    public PrismContainerValue<C> createValue() {
        return getItemDefinition().createValue();
    }

    @Override
    public boolean canRepresent(QName type) {
        return getItemDefinition().canRepresent(type);
    }

    @Override
    public MutablePrismContainerDefinition<C> toMutable() {
        return getItemDefinition().toMutable();
    }

    @Override
    public <ID extends ItemDefinition> ID findLocalItemDefinition(QName name, Class<ID> clazz, boolean caseInsensitive) {
        return getItemDefinition().findLocalItemDefinition(name, clazz, caseInsensitive);
    }

    @Override
    public <ID extends ItemDefinition> ID findNamedItemDefinition(QName firstName, ItemPath rest, Class<ID> clazz) {
        return getItemDefinition().findNamedItemDefinition(firstName, rest, clazz);
    }


    //TODO : unify with PrismContainerImpl findContainer();
    @Override
    public <T extends Containerable> PrismContainerWrapper<T> findContainer(ItemPath path) throws SchemaException {
        return findItem(path, PrismContainerWrapper.class);
    }

    private PrismContainerValueWrapper<C> findValue(Long id) {
        if (isSingleValue()) {
            if (getValues() != null) {
                return getValues().iterator().next();
            }
        }

        for (PrismContainerValueWrapper<C> value : getValues()) {
            PrismContainerValue<C> newValue = value.getNewValue();
            if (id == null) {
                //TODO : what to do?? can be recently added
                return null;
            }
            if (id.equals(newValue.getId())) {
                return value;
            }
        }

        return null;
    }

    @Override
    public <X> PrismPropertyWrapper<X> findProperty(ItemPath propertyPath) throws SchemaException {
        return findItem(propertyPath, PrismPropertyWrapper.class);
    }

    @Override
    public PrismReferenceWrapper findReference(ItemPath path) throws SchemaException {
        return findItem(path, PrismReferenceWrapper.class);
    }

    @Override
    public <T extends Containerable> PrismContainerValueWrapper<T> findContainerValue(ItemPath path) {
        throw new UnsupportedOperationException("Too lazy to implement it. Please, implement by yourself :) ");
    }


    @Override
    public boolean isEmpty() {
        return getItem().isEmpty();
    }

    @Override
    public <IW extends ItemWrapper> IW findItem(ItemPath path, Class<IW> type) throws SchemaException {
        if (ItemPath.isEmpty(path)) {
            if (type.isAssignableFrom(this.getClass())) {
                return (IW) this;
            }
            return null;
        }

        Long id = path.firstToIdOrNull();
        PrismContainerValueWrapper<C> cval = findValue(id);
        if (cval == null) {
            return null;
        }
        // descent to the correct value
        ItemPath rest = path.startsWithId() ? path.rest() : path;
        return cval.findItem(rest, type);
    }

    @Override
    public String debugDump(int indent) {
        return super.debugDump(indent);
    }

    protected <C extends Containerable> void cleanupEmptyContainers(PrismContainer<C> container) {
        List<PrismContainerValue<C>> values = container.getValues();
        Iterator<PrismContainerValue<C>> valueIterator = values.iterator();
        while (valueIterator.hasNext()) {
            PrismContainerValue<C> value = valueIterator.next();

            PrismContainerValue<C> valueAfter = cleanupEmptyContainerValue(value);
            if (valueAfter == null || valueAfter.isIdOnly() || valueAfter.isEmpty()) {
                valueIterator.remove();
            }
        }
    }

    protected <C extends Containerable> PrismContainerValue<C> cleanupEmptyContainerValue(PrismContainerValue<C> value) {
            Collection<Item<?, ?>> items = value.getItems();

            if (items != null) {
                Iterator<Item<?, ?>> iterator = items.iterator();
                while (iterator.hasNext()) {
                    Item<?, ?> item = iterator.next();

                    cleanupEmptyValues(item);
                    if (item.isEmpty()) {
                        iterator.remove();
                    }

                }


            }

            if (value.getItems() == null || value.getItems().isEmpty()) {
                return null;
            }

            return value;

    }

    private <T> void cleanupEmptyValues(Item item) {
        if (item instanceof PrismContainer) {
            cleanupEmptyContainers((PrismContainer) item);
        }

        if (item instanceof  PrismProperty) {
            PrismProperty<T> property = (PrismProperty) item;
            List<PrismPropertyValue<T>> pVals = property.getValues();
            if (pVals == null || pVals.isEmpty()) {
                return;
            }

            Iterator<PrismPropertyValue<T>> iterator = pVals.iterator();
            while (iterator.hasNext()) {
                PrismPropertyValue<T> pVal = iterator.next();
                if (pVal == null || pVal.isEmpty() || pVal.getRealValue() == null) {
                    iterator.remove();
                }
            }
        }

        if (item instanceof PrismReference) {
            PrismReference ref = (PrismReference) item;
            List<PrismReferenceValue> values = ref.getValues();
            if (values == null || values.isEmpty()) {
                return;
            }

            Iterator<PrismReferenceValue> iterator = values.iterator();
            while (iterator.hasNext()) {
                PrismReferenceValue rVal = iterator.next();
                if (rVal == null || rVal.isEmpty()) {
                    iterator.remove();
                }
            }
        }
    }

    @Override
    public <D extends ItemDelta<PrismContainerValue<C>, PrismContainerDefinition<C>>> Collection<D> getDelta() throws SchemaException {

        if (isOperational()) {
            return null;
        }

        Collection<D> deltas = new ArrayList<>();
        for (PrismContainerValueWrapper<C> pVal : getValues()) {
            LOGGER.trace("Processing delta for value:\n {}", pVal);
            ContainerDelta delta = createEmptyDelta(getPath());
            switch (pVal.getStatus()) {
                case ADDED:

                    PrismContainerValue<C> valueToAdd = pVal.getNewValue().clone();
                    if (valueToAdd.isEmpty() || valueToAdd.isIdOnly()) {
                        break;
                    }

                    valueToAdd = cleanupEmptyContainerValue(valueToAdd);
                    if (valueToAdd == null || valueToAdd.isEmpty() || valueToAdd.isIdOnly()) {
                        LOGGER.trace("Value is empty, skipping delta creation.");
                        break;
                    }

                    delta.addValueToAdd(valueToAdd);
                    deltas.add((D) delta);
                    LOGGER.trace("Computed delta: \n {}", delta.debugDump());
                    break;
                case NOT_CHANGED:
                    for (ItemWrapper iw : pVal.getItems()) {
                        LOGGER.trace("Start computing modifications for {}", iw);
                        Collection subDeltas = iw.getDelta();
                        if (CollectionUtils.isNotEmpty(subDeltas)) {
                            LOGGER.trace("No deltas computed for {}", iw);
                            deltas.addAll(subDeltas);
                        }
                        LOGGER.trace("Computed deltas:\n {}", subDeltas);
                    }

                    break;
                case DELETED:
                    delta.addValueToDelete(pVal.getOldValue().clone());
                    deltas.add((D) delta);
                    LOGGER.trace("Computed delta: \n {}", delta.debugDump());
                    break;
            }
        }
        return deltas;
    }


    protected ItemPath getDeltaPathForStatus(ItemStatus status) {
        if (ItemStatus.ADDED == status) {
            return getItemName();
        }

        return getPath();
    }

    @Override
    public boolean isVisible(PrismContainerValueWrapper parent, ItemVisibilityHandler visibilityHandler) {

        if (getComplexTypeDefinition().getTypeName().equals(MetadataType.COMPLEX_TYPE)) {
            return (getParent() != null && getParent().isShowMetadata());
        }

        return isVisibleByVisibilityHandler(visibilityHandler);
    }

    @Override
    public void setVirtual(boolean virtual) {
        this.virtual = virtual;
    }

    @Override
    public boolean isVirtual() {
        return virtual;
    }
}
