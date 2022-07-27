/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep.PreContext;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep.PreShadowInboundsPreparation;
import com.evolveum.midpoint.model.impl.sync.SynchronizationContext;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * Evaluation of inbound mappings during correlation, i.e. before clockwork is started.
 */
public class PreInboundsProcessing<F extends FocusType> extends AbstractInboundsProcessing<F> {

    private static final Trace LOGGER = TraceManager.getTrace(PreInboundsProcessing.class);

    @NotNull private final PreInboundsContext<F> ctx;

    public PreInboundsProcessing(
            @NotNull PreInboundsContext<F> ctx,
            @NotNull ModelBeans beans,
            @NotNull MappingEvaluationEnvironment env,
            @NotNull OperationResult result) {
        super(beans, env, result);
        this.ctx = ctx;
    }

    /**
     * Collects mappings for the given shadow.
     */
    void collectMappings()
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {

        try {
            PrismObject<F> preFocus = ctx.getPreFocusAsPrismObject();
            new PreShadowInboundsPreparation<>(
                    mappingsMap,
                    new PreContext(ctx, env, result, beans),
                    preFocus,
                    getFocusDefinition(preFocus))
                    .collectOrEvaluate();
        } catch (StopProcessingProjectionException e) {
            // Should be used only in clockwork processing.
            throw new IllegalStateException("Unexpected 'stop processing' exception: " + e.getMessage(), e);
        }
    }

    @Override
    @NotNull PrismObjectDefinition<F> getFocusDefinition(@Nullable PrismObject<F> focus) {
        // The interface expects nullable focus, but in fact we always have a non-null focus here
        Objects.requireNonNull(focus, "no focus");
        if (focus.getDefinition() != null) {
            return focus.getDefinition();
        } else {
            //noinspection unchecked
            return (PrismObjectDefinition<F>) beans.prismContext.getSchemaRegistry()
                    .findObjectDefinitionByCompileTimeClass(focus.asObjectable().getClass());
        }
    }

    @Override
    void applyComputedDeltas(Collection<ItemDelta<?, ?>> itemDeltas) throws SchemaException {
        LOGGER.trace("Applying deltas to the pre-focus:\n{}", DebugUtil.debugDumpLazily(itemDeltas, 1));
        ItemDeltaCollectionsUtil.applyTo(
                itemDeltas, ctx.getPreFocusAsPrismObject());
    }

    @Override
    @NotNull
    Function<ItemPath, Boolean> getFocusPrimaryItemDeltaExistsProvider() {
        return itemPath -> false; // No focus primary item deltas at this point. (We don't know the focus yet.)
    }

    @Override
    @Nullable PrismObject<F> getFocusNew() throws SchemaException {
        return ctx.getPreFocusAsPrismObject();
    }

    @Override
    protected @Nullable ObjectDelta<F> getFocusAPrioriDelta() {
        return null; // No focus -> no a priori delta for it.
    }

    @Override
    @Nullable
    LensContext<?> getLensContextIfPresent() {
        return null; // No lens context at this time.
    }

    // TODO !!!!!!!!!!!!!!
    public VariablesMap getVariablesMap() throws SchemaException {
        VariablesMap variables = ModelImplUtils.getDefaultVariablesMap(
                ctx.getPreFocus(),
                ctx.getShadowedResourceObject(),
                ctx.getResource(),
                ctx.getSystemConfiguration());
        variables.put(ExpressionConstants.VAR_SYNCHRONIZATION_CONTEXT, ctx, SynchronizationContext.class);
        return variables;
    }

    @Override
    void updateFocusIdentityData() {
        // Not applicable here.
    }
}
