/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep;

import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.InboundMappingInContext;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.StopProcessingProjectionException;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.processor.PropertyLimitations;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

class PreSource extends Source {

    PreSource(PrismObject<ShadowType> currentShadow, @Nullable ObjectDelta<ShadowType> aPrioriDelta, ResourceObjectDefinition resourceObjectDefinition) {
        super(currentShadow, aPrioriDelta, resourceObjectDefinition);
    }

    @Override
    boolean isEligibleForInboundProcessing() {
        return false;
    }

    @Override
    @NotNull ResourceType getResource() {
        return null;
    }

    @Override
    Object getContextDump() {
        return null;
    }

    @Override
    String getProjectionHumanReadableName() {
        return null;
    }

    @Override
    boolean isClockwork() {
        return false;
    }

    @Override
    boolean isProjectionBeingDeleted() {
        return false;
    }

    @Override
    boolean isAbsoluteStateAvailable() {
        return false;
    }

    @Override
    <V extends PrismValue, D extends ItemDefinition<?>> void setValueMetadata(
            Item<V, D> currentProjectionItem, ItemDelta<V, D> itemAPrioriDelta) {
    }

    @Override
    PrismObject<ShadowType> getResourceObjectNew() {
        return null;
    }

    @Override
    String getChannel() {
        return null;
    }

    @Override
    @NotNull ProcessingMode getItemProcessingMode(String itemDescription, ItemDelta<?, ?> itemAPrioriDelta, List<MappingType> mappingBeans, boolean ignored, PropertyLimitations limitations) {
        return null;
    }

    @Override
    void loadFullShadowIfNeeded(boolean fullStateRequired, @NotNull Context context) throws SchemaException, StopProcessingProjectionException {

    }

    @Override
    void resolveInputEntitlements(ItemDelta<PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>> associationAPrioriDelta, Item<PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>> currentAssociation) {

    }

    @Override
    void getEntitlementVariableProducer(com.evolveum.midpoint.repo.common.expression.@NotNull Source<?, ?> source, @Nullable PrismValue value, @NotNull VariablesMap variables) {

    }

    @Override
    <V extends PrismValue, D extends ItemDefinition<?>> InboundMappingInContext<V, D> createInboundMappingInContext(MappingImpl<V, D> mapping) {
        return null;
    }
}
