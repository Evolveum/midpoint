/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep;

import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.InboundMappingInContext;
import com.evolveum.midpoint.model.impl.sync.SynchronizationContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.processor.PropertyLimitations;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;

class PreSource extends MSource {

    @NotNull private final SynchronizationContext<?> syncCtx;

    PreSource(@NotNull SynchronizationContext<?> syncCtx) throws SchemaException, ConfigurationException {
        super(
                syncCtx.getShadowedResourceObject(),
                syncCtx.getResourceObjectDelta(),
                syncCtx.getObjectTypeDefinition());
        this.syncCtx = syncCtx;
    }

    @Override
    boolean isEligibleForInboundProcessing() {
        return true; // The shadow (as such) is always eligible for inbounds processing
    }

    @Override
    @NotNull ResourceType getResource() {
        return syncCtx.getResource().asObjectable();
    }

    @Override
    Object getContextDump() {
        return syncCtx.debugDumpLazily();
    }

    @Override
    String getProjectionHumanReadableName() {
        return syncCtx.toString();
    }

    @Override
    boolean isClockwork() {
        return false;
    }

    @Override
    boolean isProjectionBeingDeleted() {
        return ObjectDelta.isDelete(syncCtx.getResourceObjectDelta());
    }

    @Override
    boolean isAbsoluteStateAvailable() {
        return true; // We hope so ;)
    }

    @Override
    <V extends PrismValue, D extends ItemDefinition<?>> void setValueMetadata(
            Item<V, D> currentProjectionItem, ItemDelta<V, D> itemAPrioriDelta) {
        // Not supported for pre-mappings.
    }

    @Override
    PrismObject<ShadowType> getResourceObjectNew() {
        return syncCtx.getShadowedResourceObject(); // TODO what if delta is delete?
    }

    @Override
    String getChannel() {
        return syncCtx.getChannel();
    }

    @Override
    @NotNull ProcessingMode getItemProcessingMode(
            String itemDescription,
            ItemDelta<?, ?> itemAPrioriDelta,
            List<? extends MappingType> mappingBeans,
            boolean ignored,
            PropertyLimitations limitations) {
        return ProcessingMode.ABSOLUTE_STATE_IF_KNOWN; // TODO
    }

    @Override
    void loadFullShadowIfNeeded(boolean fullStateRequired, @NotNull Context context) {
        // Nothing to do here
    }

    @Override
    void resolveInputEntitlements(
            ItemDelta<PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>> associationAPrioriDelta,
            Item<PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>> currentAssociation) {
        // Associations are not yet supported in pre-mappings
    }

    @Override
    void getEntitlementVariableProducer(
            com.evolveum.midpoint.repo.common.expression.@NotNull Source<?, ?> source,
            @Nullable PrismValue value,
            @NotNull VariablesMap variables) {
        // Associations are not yet supported in pre-mappings
    }

    @Override
    <V extends PrismValue, D extends ItemDefinition<?>> InboundMappingInContext<V, D> createInboundMappingInContext(
            MappingImpl<V, D> mapping) {
        return new InboundMappingInContext<>(mapping, null);
    }

    @Override
    @NotNull InboundMappingEvaluationPhaseType getCurrentEvaluationPhase() {
        return InboundMappingEvaluationPhaseType.CORRELATION;
    }
}
