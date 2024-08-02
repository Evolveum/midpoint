/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author lskublik
 */
public class ObjectTypeAttributeMappingWrapper extends PrismContainerWrapperImpl<ResourceAttributeDefinitionType> {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectTypeAttributeMappingWrapper.class);

    public ObjectTypeAttributeMappingWrapper(
            @Nullable PrismContainerValueWrapper parent,
            PrismContainer<ResourceAttributeDefinitionType> container,
            ItemStatus status) {
        super(parent, container, status);
    }

    @Override
    public <D extends ItemDelta<? extends PrismValue, ? extends ItemDefinition>> Collection<D> getDelta() throws SchemaException {
        if (isOperational()) {
            return null;
        }

        List<AttributeMappingValueWrapper> valuesToAdd = new ArrayList<>();
        List<AttributeMappingValueWrapper> valuesNotChanged = new ArrayList<>();
        List<AttributeMappingValueWrapper> valuesToDelete = new ArrayList<>();

        Collection<D> deltas = new ArrayList<>();
        for (PrismContainerValueWrapper<ResourceAttributeDefinitionType> pVal : getValues()) {
            LOGGER.trace("Distribution delta for value:\n {}", pVal);
            ContainerDelta delta = createEmptyDelta(getPath());
            switch (pVal.getStatus()) {
                case ADDED:
                    PrismContainerValue<ResourceAttributeDefinitionType> valueToAdd = pVal.getNewValue().clone();
                    if (valueToAdd.isEmpty() || valueToAdd.isIdOnly()) {
                        break;
                    }

                    valueToAdd = WebPrismUtil.cleanupEmptyContainerValue(valueToAdd);
                    if (valueToAdd == null || valueToAdd.isEmpty() || valueToAdd.isIdOnly()) {
                        LOGGER.trace("Value is empty, skipping delta creation.");
                        break;
                    }
                    if (pVal instanceof AttributeMappingValueWrapper
                            && !((AttributeMappingValueWrapper) pVal).getAttributeMappingTypes().isEmpty()
                            && ((AttributeMappingValueWrapper) pVal).getAttributeMappingTypes().size() == 1) {
                        valuesToAdd.add((AttributeMappingValueWrapper) pVal);
                    } else {
                        delta.addValueToAdd(valueToAdd);
                        deltas.add((D) delta);
                        LOGGER.trace("Computed delta: \n {}", delta);
                    }
                    break;
                case NOT_CHANGED:
                    if (pVal instanceof AttributeMappingValueWrapper
                            && !((AttributeMappingValueWrapper) pVal).getAttributeMappingTypes().isEmpty()) {
                        valuesNotChanged.add((AttributeMappingValueWrapper) pVal);
                    } else {
                        for (ItemWrapper iw : pVal.getItems()) {
                            LOGGER.trace("Start computing modifications for {}", iw);
                            Collection subDeltas = iw.getDelta();
                            if (CollectionUtils.isNotEmpty(subDeltas)) {
                                LOGGER.trace("No deltas computed for {}", iw);
                                deltas.addAll(subDeltas);
                            }
                            LOGGER.trace("Computed deltas:\n {}", subDeltas);
                        }
                    }

                    break;
                case DELETED:
                    if (pVal instanceof AttributeMappingValueWrapper
                            && !((AttributeMappingValueWrapper) pVal).getAttributeMappingTypes().isEmpty()) {
                        valuesToDelete.add((AttributeMappingValueWrapper) pVal);
                    } else {
                        delta.addValueToDelete(pVal.getOldValue().clone());
                        deltas.add((D) delta);
                        LOGGER.trace("Computed delta: \n {}", delta.debugDump());
                    }
                    break;
            }
        }

        List<DeltaWrapper> deltaWrappers = new ArrayList<>();

        valuesToAdd.stream()
                .filter(v -> MappingDirection.OVERRIDE.equals(v.getAttributeMappingTypes().get(0)))
                .forEach(v -> deltaWrappers.add(new DeltaWrapper(v)));

        valuesNotChanged.stream()
                .filter(v -> v.getAttributeMappingTypes().contains(MappingDirection.OVERRIDE))
                .forEach(v -> deltaWrappers.add(new DeltaWrapper(v)));

        for (AttributeMappingValueWrapper<?> v : valuesNotChanged) {
            if (v.getAttributeMappingTypes().contains(MappingDirection.INBOUND)
                    || v.getAttributeMappingTypes().contains(MappingDirection.OUTBOUND)) {
                List<ItemPath> pathsForDelete = new ArrayList<>();

                if (v.getAttributeMappingTypes().contains(MappingDirection.INBOUND)) {
                    pathsForDelete.addAll(
                            processAlreadyExistValue(
                                    v,
                                    deltas,
                                    deltaWrappers,
                                    ResourceAttributeDefinitionType.F_INBOUND,
                                    true));
                }
                if (v.getAttributeMappingTypes().contains(MappingDirection.OUTBOUND)) {
                    pathsForDelete.addAll(
                            processAlreadyExistValue(
                                    v,
                                    deltas,
                                    deltaWrappers,
                                    ResourceAttributeDefinitionType.F_OUTBOUND,
                                    true));
                }

                for (ItemWrapper iw : v.getItems()) {
                    LOGGER.trace("Start computing modifications for {}", iw);
                    Collection<D> subDeltas = iw.getDelta();
                    if (CollectionUtils.isNotEmpty(subDeltas)) {
                        subDeltas.removeIf(d -> {
                            if (d.getPath().namedSegmentsOnly().equivalent(ResourceAttributeDefinitionType.F_REF)) {
                                return true;
                            }
                            for (ItemPath pathForDelete : pathsForDelete) {
                                if (pathForDelete.isSubPath(d.getPath())) {
                                    return true;
                                }
                            }
                            return false;
                        });
                        if (CollectionUtils.isNotEmpty(subDeltas)) {
                            LOGGER.trace("Computed deltas:\n {}", subDeltas);
                            deltas.addAll(subDeltas);
                        }
                    } else {
                        LOGGER.trace("No deltas computed for {}", iw);
                    }
                }
            }
        }

        for (AttributeMappingValueWrapper v : valuesToDelete) {
            if (v.getAttributeMappingTypes().contains(MappingDirection.INBOUND)
                    || v.getAttributeMappingTypes().contains(MappingDirection.OUTBOUND)) {
                if (v.getAttributeMappingTypes().contains(MappingDirection.INBOUND)) {
                    processAlreadyExistValue(v, deltas, deltaWrappers, ResourceAttributeDefinitionType.F_INBOUND, false);
                }
                if (v.getAttributeMappingTypes().contains(MappingDirection.OUTBOUND)) {
                    processAlreadyExistValue(v, deltas, deltaWrappers, ResourceAttributeDefinitionType.F_OUTBOUND, false);
                }

                ContainerDelta delta = createEmptyDelta(getPath());

                delta.addValueToDelete(v.getOldValue().clone());
                deltas.add((D) delta);
                LOGGER.trace("Computed delta: \n {}", delta.debugDump());
            }
        }

        for (AttributeMappingValueWrapper v : valuesToAdd) {
            if (v.getAttributeMappingTypes().contains(MappingDirection.INBOUND)) {
                processAddValues(v, deltas, deltaWrappers, ResourceAttributeDefinitionType.F_INBOUND);
            }

            if (v.getAttributeMappingTypes().contains(MappingDirection.OUTBOUND)) {
                processAddValues(v, deltas, deltaWrappers, ResourceAttributeDefinitionType.F_OUTBOUND);
            }
        }

        for (DeltaWrapper deltaWrapper : deltaWrappers) {
            if (deltaWrapper.value == null) {
                if (deltaWrapper.inbounds.isEmpty() && deltaWrapper.outbounds.isEmpty()) {
                    continue;
                }

                PrismContainerValue<ResourceAttributeDefinitionType> newValue =
                        new ResourceAttributeDefinitionType().asPrismContainerValue();

                newValue.asContainerable().setRef(deltaWrapper.attributeRef);

                processAddDeltaWrapper(newValue, deltaWrapper, deltas);
            } else if (deltaWrapper.value.getStatus().equals(ValueStatus.ADDED)) {

                processAddDeltaWrapper(deltaWrapper.value.getNewValue().clone(), deltaWrapper, deltas);

            }
        }

        return deltas;
    }

    private <D extends ItemDelta<? extends PrismValue, ? extends ItemDefinition>> void processAddValues(
            AttributeMappingValueWrapper v,
            Collection<D> deltas,
            List<DeltaWrapper> deltaWrappers,
            ItemName containerPath) throws SchemaException {

        PrismContainerWrapper<InboundMappingType> inboundContainer =
                v.findContainer(containerPath);

        if (inboundContainer != null) {

            for (PrismContainerValueWrapper<InboundMappingType> inboundValue : inboundContainer.getValues()) {

                PrismContainerValue<InboundMappingType> prismInboundValue =
                        WebPrismUtil.cleanupEmptyContainerValue(inboundValue.getNewValue().clone());
                if (prismInboundValue == null || prismInboundValue.isEmpty() || prismInboundValue.isIdOnly()) {
                    LOGGER.trace("Value is empty, skipping delta processing.");
                    continue;
                }

                PrismPropertyWrapper<ItemPathType> virtualRef =
                        inboundValue.findProperty(ResourceAttributeDefinitionType.F_REF);

                if (virtualRef == null) {
                    LOGGER.debug("Skip processing inbound container because, couldn't find virtual attribute property");
                    continue;
                }

                if (virtualRef.getItem().getRealValue() == null) {

                    createAttributeMappingWithoutRef(
                            inboundValue,
                            inboundContainer,
                            null,
                            containerPath,
                            deltas,
                            null,
                            false);

                    continue;
                }

                searchInDeltaWrappers(
                        inboundContainer,
                        inboundValue,
                        virtualRef,
                        containerPath,
                        deltas,
                        deltaWrappers,
                        null,
                        false);
            }
        }
    }

    private <D extends ItemDelta<? extends PrismValue, ? extends ItemDefinition>> void processAddDeltaWrapper(
            PrismContainerValue<ResourceAttributeDefinitionType> newValue,
            DeltaWrapper deltaWrapper,
            Collection<D> deltas) throws SchemaException {

        PrismContainer inboundContainer = newValue.findOrCreateContainer(ResourceAttributeDefinitionType.F_INBOUND);
        PrismContainer outboundContainer = newValue.findOrCreateContainer(ResourceAttributeDefinitionType.F_OUTBOUND);

        for (PrismContainerValue<InboundMappingType> inbound : deltaWrapper.inbounds) {

            PrismContainerValue<InboundMappingType> newInbound = inbound.clone();
            newInbound = WebPrismUtil.cleanupEmptyContainerValue(newInbound);
            if (newInbound == null || newInbound.isEmpty() || newInbound.isIdOnly()) {
                LOGGER.trace("Value is empty, skipping delta processing.");
                continue;
            }

            newInbound.clearParent();
            newInbound.setParent(inboundContainer);
            inboundContainer.add(newInbound);
        }

        for (PrismContainerValue<MappingType> outbound : deltaWrapper.outbounds) {

            PrismContainerValue<MappingType> newOutbound = outbound.clone();
            newOutbound = WebPrismUtil.cleanupEmptyContainerValue(newOutbound);
            if (newOutbound == null || newOutbound.isEmpty() || newOutbound.isIdOnly()) {
                LOGGER.trace("Value is empty, skipping delta processing.");
                continue;
            }

            newOutbound.clearParent();
            newOutbound.setParent(outboundContainer);
            outboundContainer.add(newOutbound);
        }

        createAddDelta(newValue, this, deltas);
    }

    private <D extends ItemDelta> void searchInDeltaWrappers(
            PrismContainerWrapper inboundContainer,
            PrismContainerValueWrapper inboundValue,
            PrismPropertyWrapper<ItemPathType> virtualRef,
            ItemPath path,
            Collection<D> deltas,
            List<DeltaWrapper> deltaWrappers,
            List<ItemPath> pathsForDelete,
            boolean isNotChangedState) throws SchemaException {

        DeltaWrapper foundDeltaWrapper = null;
        for (DeltaWrapper deltaWrapper : deltaWrappers) {

            if (deltaWrapper.attributeRef.equivalent(virtualRef.getValue().getRealValue())) {
                foundDeltaWrapper = deltaWrapper;
                break;
            }
        }
        if (foundDeltaWrapper != null) {

            if (foundDeltaWrapper.value == null
                    || foundDeltaWrapper.value.getStatus().equals(ValueStatus.ADDED)) {

                if (path.equivalent(ResourceAttributeDefinitionType.F_INBOUND)) {
                    foundDeltaWrapper.addInbound(inboundValue.getNewValue().clone());
                } else {
                    foundDeltaWrapper.addOutbound(inboundValue.getNewValue().clone());
                }

            } else if (foundDeltaWrapper.value.getStatus().equals(ValueStatus.NOT_CHANGED)) {

                PrismContainerWrapper<InboundMappingType> deltaWrapperInboundContainer
                        = foundDeltaWrapper.value.findContainer(path);
                PrismContainerValue<InboundMappingType> inboundRealValue = inboundValue.getNewValue().clone();
                createAddDelta(inboundRealValue, deltaWrapperInboundContainer, deltas);
            }

        } else {
            DeltaWrapper deltaWrapper = new DeltaWrapper(virtualRef.getValue().getRealValue());
            if (path.equivalent(ResourceAttributeDefinitionType.F_INBOUND)) {
                deltaWrapper.addInbound(inboundValue.getNewValue().clone());
            } else {
                deltaWrapper.addOutbound(inboundValue.getNewValue().clone());
            }
            deltaWrappers.add(deltaWrapper);
        }
        if (isNotChangedState) {
            createDeleteDelta(inboundContainer, inboundValue, deltas, pathsForDelete);
        }
    }

    private <D extends ItemDelta> List<ItemPath> processAlreadyExistValue(
            AttributeMappingValueWrapper v,
            Collection<D> deltas,
            List<DeltaWrapper> deltaWrappers,
            ItemName containerPath,
            boolean isNotChangedState) throws SchemaException {

        List<ItemPath> pathsForDelete = new ArrayList<>();

        PrismContainerWrapper<InboundMappingType> inboundContainer =
                v.findContainer(containerPath);

        if (inboundContainer != null) {

            for (PrismContainerValueWrapper<InboundMappingType> inboundValue : inboundContainer.getValues()) {

                PrismContainerValue<InboundMappingType> prismInboundValue =
                        WebPrismUtil.cleanupEmptyContainerValue(inboundValue.getNewValue().clone());
                if (prismInboundValue == null || prismInboundValue.isEmpty() || prismInboundValue.isIdOnly()) {
                    LOGGER.trace("Value is empty, skipping delta processing.");
                    continue;
                }

                if (inboundValue.getStatus().equals(ValueStatus.NOT_CHANGED)) {
                    PrismPropertyWrapper<ItemPathType> virtualRef =
                            inboundValue.findProperty(ResourceAttributeDefinitionType.F_REF);

                    if (virtualRef == null) {
                        LOGGER.debug("Skip processing inbound container because, couldn't find virtual attribute property");
                        continue;
                    }

                    PrismPropertyWrapper<ItemPathType> ref = v.findProperty(ResourceAttributeDefinitionType.F_REF);
                    if (virtualRef.getItem().getRealValue() == null) {

                        if (ref.getItem().getRealValue() == null) {
                            continue;
                        }

                        createAttributeMappingWithoutRef(
                                inboundValue,
                                inboundContainer,
                                null,
                                containerPath,
                                deltas,
                                pathsForDelete,
                                isNotChangedState);

                        continue;
                    }

                    if (ref.getValue().getRealValue() != null
                            && ref.getValue().getRealValue().equivalent(virtualRef.getItem().getRealValue())) {
                        continue;
                    }
                    searchInDeltaWrappers(
                            inboundContainer,
                            inboundValue,
                            virtualRef,
                            containerPath,
                            deltas,
                            deltaWrappers,
                            pathsForDelete,
                            isNotChangedState);
                }
            }
        }
        return pathsForDelete;
    }

    private <D extends ItemDelta> void createAttributeMappingWithoutRef(
            PrismContainerValueWrapper value,
            PrismContainerWrapper container,
            ItemPathType attributeRef,
            ItemName path,
            Collection<D> deltas,
            List<ItemPath> pathsForDelete, boolean isNotChangedState) throws SchemaException {

        PrismContainerValue<ResourceAttributeDefinitionType> newValue =
                new ResourceAttributeDefinitionType().asPrismContainerValue();

        if (attributeRef != null) {
            newValue.asContainerable().setRef(attributeRef);
        }

        PrismContainer prismContainer = newValue.findOrCreateContainer(path);
        PrismContainerValue realValue = value.getNewValue().clone();
        realValue.clearParent();
        realValue.setParent(prismContainer);
        prismContainer.add(realValue);

        createAddDelta(newValue, this, deltas);

        if (isNotChangedState) {
            createDeleteDelta(container, value, deltas, pathsForDelete);
        }
    }

    private <D extends ItemDelta> void createAddDelta(
            PrismContainerValue newValue,
            PrismContainerWrapper containerWrapper,
            Collection<D> deltas) {

        ContainerDelta delta = containerWrapper.createEmptyDelta(containerWrapper.getPath());

        newValue = WebPrismUtil.cleanupEmptyContainerValue(newValue);
        delta.addValueToAdd(newValue);
        deltas.add((D) delta);

    }

    private <D extends ItemDelta> void createDeleteDelta(
            PrismContainerWrapper<InboundMappingType> inboundContainer,
            PrismContainerValueWrapper<InboundMappingType> inboundValue,
            Collection<D> deltas,
            List<ItemPath> pathsForDelete) {

        ContainerDelta deleteDelta = inboundContainer.createEmptyDelta(inboundContainer.getPath());
        deleteDelta.addValueToDelete(inboundValue.getOldValue().clone());
        deltas.add((D) deleteDelta);

        pathsForDelete.add(inboundValue.getPath());
    }

    private class DeltaWrapper {

        private AttributeMappingValueWrapper value;

        private ItemPathType attributeRef;

        private List<PrismContainerValue<InboundMappingType>> inbounds = new ArrayList<>();

        private List<PrismContainerValue<MappingType>> outbounds = new ArrayList<>();

        private DeltaWrapper(ItemPathType attributeRef) {
            this.attributeRef = attributeRef;
        }

        private DeltaWrapper(AttributeMappingValueWrapper value) {
            this.value = value;
            try {
                PrismPropertyWrapper<ItemPathType> refProperty = value.findProperty(ResourceAttributeDefinitionType.F_REF);
                this.attributeRef = refProperty.getValue().getRealValue();
            } catch (SchemaException e) {
                LOGGER.debug("Couldn't define attribute ref for mapping " + value);
            }
        }

        private DeltaWrapper addInbound(PrismContainerValue<InboundMappingType> inbound) {
            inbounds.add(inbound);
            return this;
        }

        private DeltaWrapper addOutbound(PrismContainerValue<MappingType> outbound) {
            outbounds.add(outbound);
            return this;
        }
    }

    public void applyDelta() throws SchemaException {
        Collection<ItemDelta<? extends PrismValue, ? extends ItemDefinition>> deltas = getDelta();
        getItem().clear();
        for (ItemDelta<? extends PrismValue, ? extends ItemDefinition> delta : deltas) {
            delta.applyTo(getItem());
        }
    }
}
