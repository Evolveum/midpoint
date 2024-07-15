/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep;

import java.util.List;

import com.evolveum.midpoint.model.api.InboundSourceData;
import com.evolveum.midpoint.model.api.identities.IdentityItemConfiguration;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.SingleShadowInboundsProcessingContext;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.config.AbstractMappingConfigItem;
import com.evolveum.midpoint.schema.processor.ShadowAssociation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.MappingEvaluationRequest;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.processor.PropertyLimitations;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;

public class LimitedSource extends MappingSource {

    @NotNull private final SingleShadowInboundsProcessingContext<?> ctx;

    public LimitedSource(@NotNull SingleShadowInboundsProcessingContext<?> ctx) throws SchemaException, ConfigurationException {
        super(
                InboundSourceData.forShadowLikeValue(
                        ctx.getShadowLikeValue(),
                        ctx.getResourceObjectDelta(),
                        ctx.getObjectDefinitionRequired()),
                ctx.getInboundDefinition(),
                ctx.getResource(),
                ctx.toString());
        this.ctx = ctx;
    }

    @Override
    boolean isEligibleForInboundProcessing(OperationResult result) {
        return true; // The shadow (as such) is always eligible for inbounds processing
    }

    @Override
    boolean isClockwork() {
        return !ctx.isBeforeCorrelation();
    }

    @Override
    boolean isProjectionBeingDeleted() {
        return ObjectDelta.isDelete(ctx.getResourceObjectDelta());
    }

    @Override
    boolean isAbsoluteStateAvailable() {
        return true; // We hope so ;)
    }

    @Override
    <V extends PrismValue, D extends ItemDefinition<?>> void setValueMetadata(
            Item<V, D> currentProjectionItem, ItemDelta<V, D> itemAPrioriDelta, OperationResult result) {
        // Not supported for pre-mappings.
    }

    @Override
    String getChannel() {
        return ctx.getChannel();
    }

    @Override
    @NotNull ProcessingMode getItemProcessingMode(
            String itemDescription,
            ItemDelta<?, ?> itemAPrioriDelta,
            List<? extends AbstractMappingConfigItem<?>> mappings,
            boolean executionModeVisible,
            boolean ignored,
            PropertyLimitations limitations) {
        if (shouldBeMappingSkipped(itemDescription, executionModeVisible, ignored, limitations)) {
            return ProcessingMode.NONE;
        }
        return ProcessingMode.ABSOLUTE_STATE_IF_KNOWN; // TODO
    }

    @Override
    void loadFullShadowIfNeeded(boolean fullStateRequired, @NotNull MappingContext context, OperationResult result) {
        // Nothing to do here
    }

    @Override
    void resolveInputEntitlements(
            ContainerDelta<ShadowAssociationValueType> associationAPrioriDelta,
            ShadowAssociation currentAssociation) {
        // Associations are not yet supported in limited processing
    }

    @Override
    void getEntitlementVariableProducer(
            com.evolveum.midpoint.repo.common.expression.@NotNull Source<?, ?> source,
            @Nullable PrismValue value,
            @NotNull VariablesMap variables) {
        // Associations are not yet supported in limited processing
    }

    @Override
    <V extends PrismValue, D extends ItemDefinition<?>> MappingEvaluationRequest<V, D> createMappingRequest(
            MappingImpl<V, D> mapping) {
        // The projection context may come from embedding mapping (e.g., for associations).
        return new MappingEvaluationRequest<>(
                mapping,
                false,
                (LensProjectionContext) ModelExpressionThreadLocalHolder.getProjectionContext());
    }

    @Override
    @NotNull InboundMappingEvaluationPhaseType getCurrentEvaluationPhase() {
        return ctx.isBeforeCorrelation() ?
                InboundMappingEvaluationPhaseType.BEFORE_CORRELATION :
                InboundMappingEvaluationPhaseType.CLOCKWORK;
    }

    @Override
    @Nullable FocusIdentitySourceType getFocusIdentitySource() {
        return null;
    }

    @Override
    @Nullable IdentityItemConfiguration getIdentityItemConfiguration(@NotNull ItemPath itemPath) {
        return null; // at least for the time being
    }

    @Override
    ItemPath determineTargetPathExecutionOverride(ItemPath targetItemPath) {
        return null;
    }
}
