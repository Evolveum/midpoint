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
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.projector.focus.consolidation.DeltaSetTripleMapConsolidation.APrioriDeltaProvider;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep.FullContext;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep.FullInboundsPreparation;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * Evaluation of inbound mappings from all projections in given lens context. This is the "full mode".
 */
public class FullInboundsProcessing<F extends FocusType> extends AbstractInboundsProcessing<F> {

    private static final Trace LOGGER = TraceManager.getTrace(FullInboundsProcessing.class);

    private static final String OP_COLLECT_MAPPINGS = FullInboundsProcessing.class.getName() + ".collectMappings";

    @NotNull private final LensContext<F> context;

    public FullInboundsProcessing(
            @NotNull LensContext<F> context,
            @NotNull MappingEvaluationEnvironment env) {
        super(env);
        this.context = context;
    }

    /**
     * Collects all the mappings from all the projects, sorted by target property.
     *
     * Original motivation (is it still valid?): we need to evaluate them together, e.g. in case that there are several mappings
     * from several projections targeting the same property.
     */
    void collectMappings(OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {

        for (LensProjectionContext projectionContext : context.getProjectionContexts()) {

            OperationResult result = parentResult.subresult(OP_COLLECT_MAPPINGS)
                    .addParam("projectionContext", projectionContext.getHumanReadableName())
                    .build();

            try {
                // Preliminary checks. (Before computing apriori delta and other things.)

                if (projectionContext.isGone()) {
                    LOGGER.trace("Skipping processing of inbound expressions for projection {} because is is gone",
                            lazy(projectionContext::getHumanReadableName));
                    result.recordNotApplicable("projection is gone");
                    continue;
                }
                if (!projectionContext.isCanProject()) {
                    LOGGER.trace("Skipping processing of inbound expressions for projection {}: "
                                    + "there is a limit to propagate changes only from resource {}",
                            lazy(projectionContext::getHumanReadableName), context.getTriggeringResourceOid());
                    result.recordNotApplicable("change propagation is limited");
                    continue;
                }

                try {
                    PrismObject<F> objectCurrentOrNew = context.getFocusContext().getObjectCurrentOrNew();
                    new FullInboundsPreparation<>(
                            projectionContext,
                            context,
                            evaluationRequests,
                            itemDefinitionMap,
                            new FullContext(context, env),
                            objectCurrentOrNew,
                            getFocusDefinition(objectCurrentOrNew))
                            .collectOrEvaluate(result);
                } catch (StopProcessingProjectionException e) {
                    LOGGER.debug("Inbound processing on {} interrupted because the projection is broken", projectionContext);
                }

            } catch (Throwable t) {
                result.recordException(t);
                throw t;
            } finally {
                result.close();
            }
        }
    }

    @Override
    @Nullable PrismObjectValue<F> getTarget() {
        return context.getFocusContext().getObjectNew().getValue();
    }

    @Override
    protected @NotNull APrioriDeltaProvider getFocusAPrioriDeltaProvider() {
        return APrioriDeltaProvider.forDelta(
                context.getFocusContextRequired().getCurrentDelta());
    }

    @Override
    @NotNull
    Function<ItemPath, Boolean> getFocusPrimaryItemDeltaExistsProvider() {
        return context::primaryFocusItemDeltaExists;
    }

    private @NotNull PrismObjectDefinition<F> getFocusDefinition(@Nullable PrismObject<F> focus) {
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
