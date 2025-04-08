/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.schemaContext.SchemaContextDefinition;
import com.evolveum.midpoint.util.logging.LoggingUtils;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserInterfaceElementVisibilityType;

import org.jetbrains.annotations.Nullable;

/**
 * @author katka
 */
public class PrismContainerWrapperImpl<C extends Containerable>
        extends ItemWrapperImpl<PrismContainer<C>, PrismContainerValueWrapper<C>>
        implements PrismContainerWrapper<C>, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PrismContainerWrapperImpl.class);

    private boolean expanded;

    private boolean virtual;

    private String identifier;

    public PrismContainerWrapperImpl(PrismContainerValueWrapper<?> parent, PrismContainer<C> item, ItemStatus status) {
        super(parent, item, status);
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
    public void setShowEmpty(boolean isShowEmpty, boolean recursive) {
        super.setShowEmpty(isShowEmpty, recursive);
        if (recursive) {
            getValues().forEach(v -> v.setShowEmpty(isShowEmpty));
        }
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
    public @NotNull List<? extends ItemDefinition<?>> getDefinitions() {
        return getItemDefinition().getDefinitions();
    }

    @Override
    public <ID extends ItemDefinition<?>> ID findLocalItemDefinition(@NotNull QName name, @NotNull Class<ID> clazz, boolean caseInsensitive) {
        return getItemDefinition().findLocalItemDefinition(name, clazz, caseInsensitive);
    }

    @Override
    public List<PrismPropertyDefinition<?>> getPropertyDefinitions() {
        return getItemDefinition().getPropertyDefinitions();
    }

    @Override
    public @NotNull ContainerDelta<C> createEmptyDelta(ItemPath path) {
        return getItemDefinition().createEmptyDelta(path);
    }

    @NotNull
    @Override
    public PrismContainerDefinition<C> clone() {
        return getItemDefinition().clone();
    }

    @Override
    public PrismContainerDefinition<C> cloneWithNewDefinition(QName newItemName, ItemDefinition newDefinition) {
        return getItemDefinition().cloneWithNewDefinition(newItemName, newDefinition);
    }

    @Override
    public @NotNull ItemDefinition<PrismContainer<C>> cloneWithNewName(@NotNull ItemName itemName) {
        throw new UnsupportedOperationException("Implement if needed");
    }

    @Override
    public @NotNull PrismContainerDefinition<?> cloneWithNewType(@NotNull QName newTypeName, @NotNull ComplexTypeDefinition newCtd) {
        throw new UnsupportedOperationException("Implement if needed");
    }

    @Override
    public Boolean isIndexed() {
        return getItemDefinition().isIndexed();
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
    public boolean canRepresent(@NotNull QName type) {
        return getItemDefinition().canRepresent(type);
    }

    @Override
    public PrismContainerDefinitionMutator<C> mutator() {
        return getItemDefinition().mutator();
    }

    //TODO : unify with PrismContainerImpl findContainer();
    @Override
    public <T extends Containerable> PrismContainerWrapper<T> findContainer(ItemPath path) throws SchemaException {
        return findItem(path, PrismContainerWrapper.class);
    }

    public <T extends Containerable> PrismContainerWrapper<T> findContainer(String identifier) {
        List<PrismContainerValueWrapper<C>> values = getValues();
        for (PrismContainerValueWrapper<C> value : values) {
            PrismContainerWrapper<T> wrapper = value.findContainer(identifier);
            if (wrapper != null) {
                return wrapper;
            }
        }
        return null;
    }

    PrismContainerValueWrapper<C> findValue(Long id) {
        if (isSingleValue()) {
            List<PrismContainerValueWrapper<C>> values = getValues();
            if (values != null && !values.isEmpty()) {
                return values.iterator().next();
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
    public PrismContainerValueWrapper<C> findContainerValue(ItemPath path) {
        try {
            Object last = path.last();
            if (ItemPath.isId(last)) {
                path = path.allExceptLast();
            }
            PrismContainerWrapper containerWrapper = findContainer(path);

            if (!(containerWrapper instanceof PrismContainerWrapperImpl)) {
                throw new UnsupportedOperationException("Cannot find container wrapper for " + path + ". Unsupported parent found: " + containerWrapper);
            }

            PrismContainerWrapperImpl containerWrapperImpl = (PrismContainerWrapperImpl) containerWrapper;
            if (ItemPath.isId(last)) {
                return containerWrapperImpl.findValue((Long) last);
            } else {
                if (isSingleValue()) {
                    return containerWrapperImpl.findValue(null);
                }
                throw new UnsupportedOperationException("Cannot get value from multivalue container without specified container id.");
            }

        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Unexpected exception while trying to find container value with path {}, parentContainer {}", e, path, this);
            return null;
        }
//        if (isSingleValue()) {
//            return findValue(0L);
//        } else if (!path.startsWithId()) {
//            throw new UnsupportedOperationException("Cannot get value from multivalue container without specified container id.");
//        } else {
//            return findValue(path.firstToId());
//        }
    }

    public PrismContainerDefinition<C> getItemDefinition() {
        return super.getItemDefinition();
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
    public <D extends ItemDelta<? extends PrismValue, ? extends ItemDefinition>> Collection<D> getDelta() throws SchemaException {

        if (isOperational()) {
            return null;
        }
        return computeDeltasInternal();
    }

    protected <D extends ItemDelta<? extends PrismValue, ? extends ItemDefinition>> Collection<D> computeDeltasInternal() throws SchemaException {
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

                    valueToAdd = WebPrismUtil.cleanupEmptyContainerValue(valueToAdd);
                    if (valueToAdd == null || valueToAdd.isIdOnly()) {
                        LOGGER.trace("Value is empty, skipping delta creation.");
                        break;
                    }

                    if (!WebPrismUtil.isUseAsEmptyValue(valueToAdd) && valueToAdd.isEmpty()) {
                        LOGGER.trace("Value is empty, skipping delta creation.");
                        break;
                    }

                    //fix for #10624
                    //just to ensure that all deltas are applied to valueToAdd
                    Collection<ItemDelta> itemDeltas = new ArrayList<>();
                    for (ItemWrapper<?, ?> itemWrapper : pVal.getItems()) {
                        Collection<? extends ItemDelta> itemDelta = itemWrapper.getDelta();
                        if (itemDelta == null || itemDelta.isEmpty()) {
                            continue;
                        }
                        itemDeltas.addAll(itemDelta);
                    }
                    for (ItemDelta d : itemDeltas) {
                        d.applyTo(valueToAdd);
                    }
                    //end fix for #10624
                    delta.addValueToAdd(valueToAdd);
                    deltas.add((D) delta);
                    LOGGER.trace("Computed delta: \n {}", delta);
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
    public boolean isVisible(PrismContainerValueWrapper<?> parent, ItemVisibilityHandler visibilityHandler) {

        if (isVirtual() && getVisibleOverwrite() != null && UserInterfaceElementVisibilityType.HIDDEN == getVisibleOverwrite()) {
            return false;
        }

//        if (getComplexTypeDefinition().getTypeName().equals(MetadataType.COMPLEX_TYPE)) {
//            return (getParent() != null && getParent().isShowMetadata());
//        }

        // pretend that object is always expanded. it is becasue all other containers are children of it
        // and it can influence visibility behavior on different tabs.
        boolean parentExpanded = parent instanceof PrismObjectValueWrapper ? true : parent.isExpanded();
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

    @Override
    public boolean isImmutable() {
        // TODO
        return false;
    }

    @Override
    public void freeze() {
        // TODO
    }

    @Override
    public boolean accept(Visitor<Definition> visitor, SmartVisitation<Definition> visitation) {
        // TODO
        return false;
    }

    @Override
    public void accept(Visitor<Definition> visitor) {
        // TODO
    }

    @Override
    protected PrismContainerValue<C> createNewEmptyValue(ModelServiceLocator locator) {
        return createValue();
    }

    @Override
    public PrismContainerWrapper<? extends Containerable> getSelectedChild() {
        if (isShowMetadataDetails()) {
            return this;
        }
        List<PrismContainerValueWrapper<C>> values = getValues();
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }

        for (PrismContainerValueWrapper<C> metadataValue : values) {
            PrismContainerWrapper<? extends Containerable> selected = metadataValue.getSelectedChild();
            if (selected != null) {
                return selected;
            }
        }

        return null;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public Class<C> getTypeClass() {
        //noinspection unchecked
        return (Class<C>) super.getTypeClass();
    }

    @Override
    public @Nullable SchemaContextDefinition getSchemaContextDefinition() {
        return null;
    }

    @Override
    public PrismContainerWrapper<C> cloneVirtualContainerWithNewValue(
            PrismContainerValueWrapper<? extends Containerable> parent, ModelServiceLocator modelServiceLocator){
        if (!isVirtual()) {
            throw new UnsupportedOperationException();
        }
        PrismContainerWrapperImpl<C> virtualContainer = new PrismContainerWrapperImpl<>(parent, getOldItem().clone(), getStatus());
        virtualContainer.setExpanded(isExpanded());
        virtualContainer.setVirtual(isVirtual());
        virtualContainer.setIdentifier(getIdentifier());
        virtualContainer.setColumn(isColumn());
        virtualContainer.setShowEmpty(isShowEmpty(), false);
        virtualContainer.setShowInVirtualContainer(isShowInVirtualContainer());
        virtualContainer.setMetadata(isMetadata());
        virtualContainer.setShowMetadataDetails(isShowMetadataDetails());
        virtualContainer.setProcessProvenanceMetadata(isProcessProvenanceMetadata());
        virtualContainer.setReadOnly(isReadOnly());
        virtualContainer.setVisibleOverwrite(getVisibleOverwrite());

        PrismContainerValue<C> newValue = virtualContainer.getItem().createNewValue();

        try {
            PrismContainerValueWrapper<C> oldValueWrapper = getValue();
            PrismContainerValueWrapper<C> newValueWrapper =
                    WebPrismUtil.createNewValueWrapper(virtualContainer, newValue, modelServiceLocator);

            newValueWrapper.setExpanded(oldValueWrapper.isExpanded());
            newValueWrapper.setShowEmpty(oldValueWrapper.isShowEmpty());
            newValueWrapper.setSorted(oldValueWrapper.isSorted());
            newValueWrapper.setShowMetadata(oldValueWrapper.isShowMetadata());
            newValueWrapper.setReadOnly(oldValueWrapper.isReadOnly(), false);
            newValueWrapper.setSelected(oldValueWrapper.isSelected());
            newValueWrapper.setHeterogenous(oldValueWrapper.isHeterogenous());
            newValueWrapper.setMetadata(oldValueWrapper.isMetadata());
            newValueWrapper.setVirtualContainerItems(oldValueWrapper.getVirtualItems());
            newValueWrapper.setValueMetadata(oldValueWrapper.getValueMetadata());

            virtualContainer.getValues().add(newValueWrapper);
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't create new Wrapper for virtual container.", e);
        }

        return virtualContainer;
    }
}
