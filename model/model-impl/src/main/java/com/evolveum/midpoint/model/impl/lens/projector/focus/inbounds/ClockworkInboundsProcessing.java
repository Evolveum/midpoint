/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds;

import static com.evolveum.midpoint.util.DebugUtil.lazy;

import java.util.Collection;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep.ClockworkContext;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep.ClockworkShadowInboundsPreparation;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * Evaluation of inbound mappings from all projections in given lens context.
 *
 * Responsibility of this class:
 *
 * 1. collects inbound mappings to be evaluated
 * 2. evaluates them
 * 3. consolidates the results into deltas
 */
public class ClockworkInboundsProcessing<F extends FocusType> extends AbstractInboundsProcessing<F> {

    private static final Trace LOGGER = TraceManager.getTrace(ClockworkInboundsProcessing.class);

    @NotNull private final LensContext<F> context;

    public ClockworkInboundsProcessing(
            @NotNull LensContext<F> context,
            @NotNull ModelBeans beans,
            @NotNull MappingEvaluationEnvironment env,
            @NotNull OperationResult result) {
        super(beans, env, result);
        this.context = context;
    }

    /**
     * Collects all the mappings from all the projects, sorted by target property.
     *
     * Original motivation (is it still valid?): we need to evaluate them together, e.g. in case that there are several mappings
     * from several projections targeting the same property.
     */
    void collectMappings()
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {

        for (LensProjectionContext projectionContext : context.getProjectionContexts()) {

            // Preliminary checks. (Before computing apriori delta and other things.)

            if (projectionContext.isGone()) {
                LOGGER.trace("Skipping processing of inbound expressions for projection {} because is is gone",
                        lazy(projectionContext::getHumanReadableName));
                continue;
            }
            if (!projectionContext.isCanProject()) {
                LOGGER.trace("Skipping processing of inbound expressions for projection {}: "
                                + "there is a limit to propagate changes only from resource {}",
                        lazy(projectionContext::getHumanReadableName), context.getTriggeringResourceOid());
                continue;
            }

            try {
                PrismObject<F> objectCurrentOrNew = context.getFocusContext().getObjectCurrentOrNew();
                new ClockworkShadowInboundsPreparation<>(
                        projectionContext,
                        context,
                        mappingsMap,
                        itemDefinitionMap,
                        new ClockworkContext(context, env, result, beans),
                        objectCurrentOrNew,
                        getFocusDefinition(objectCurrentOrNew))
                        .collectOrEvaluate();
            } catch (StopProcessingProjectionException e) {
                LOGGER.debug("Inbound processing on {} interrupted because the projection is broken", projectionContext);
            }
        }
    }

    @Override
    @Nullable PrismObject<F> getFocusNew() {
        return context.getFocusContext().getObjectNew();
    }

    @Override
    protected @Nullable ObjectDelta<F> getFocusAPrioriDelta() {
        return context.getFocusContextRequired().getCurrentDelta();
    }

    @Override
    @NotNull
    Function<ItemPath, Boolean> getFocusPrimaryItemDeltaExistsProvider() {
        return context::primaryFocusItemDeltaExists;
    }

    @Override
    @NotNull PrismObjectDefinition<F> getFocusDefinition(@Nullable PrismObject<F> focus) {
        if (focus != null && focus.getDefinition() != null) {
            return focus.getDefinition();
        } else {
            return context.getFocusContextRequired().getObjectDefinition();
        }
    }

    @Override
    void applyComputedDeltas(Collection<ItemDelta<?, ?>> itemDeltas) throws SchemaException {
        context.getFocusContextRequired().swallowToSecondaryDelta(itemDeltas);
    }

    @Override
    @Nullable LensContext<?> getLensContextIfPresent() {
        return context;
    }
}
