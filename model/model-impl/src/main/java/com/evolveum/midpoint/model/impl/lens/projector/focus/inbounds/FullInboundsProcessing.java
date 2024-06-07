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

    @NotNull private final LensContext<F> lensContext;

    public FullInboundsProcessing(
            @NotNull LensContext<F> lensContext,
            @NotNull MappingEvaluationEnvironment env) {
        super(env);
        this.lensContext = lensContext;
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

        for (LensProjectionContext projectionContext : lensContext.getProjectionContexts()) {

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
                            lazy(projectionContext::getHumanReadableName), lensContext.getTriggeringResourceOid());
                    result.recordNotApplicable("change propagation is limited");
                    continue;
                }

                try {
                    PrismObject<F> objectCurrentOrNew = lensContext.getFocusContext().getObjectCurrentOrNew();
                    new FullInboundsPreparation<>(
                            projectionContext,
                            lensContext,
                            evaluationRequests,
                            itemDefinitionMap,
                            new FullContext(lensContext, env, assignmentsProcessingContext),
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
    @Nullable PrismObjectValue<F> getTargetNew() {
        return lensContext.getFocusContext().getObjectNew().getValue();
    }

    @Override
    @Nullable PrismObjectValue<F> getTarget() {
        var current = lensContext.getFocusContext().getObjectCurrent();
        return current != null ? current.getValue() : null;
    }

    @Override
    protected @NotNull APrioriDeltaProvider getFocusAPrioriDeltaProvider() {
        return APrioriDeltaProvider.forDelta(
                lensContext.getFocusContextRequired().getCurrentDelta());
    }

    @Override
    @NotNull
    Function<ItemPath, Boolean> getFocusPrimaryItemDeltaExistsProvider() {
        return lensContext::primaryFocusItemDeltaExists;
    }

    private @NotNull PrismObjectDefinition<F> getFocusDefinition(@Nullable PrismObject<F> focus) {
        if (focus != null && focus.getDefinition() != null) {
            return focus.getDefinition();
        } else {
            return lensContext.getFocusContextRequired().getObjectDefinition();
        }
    }

    @Override
    void applyComputedDeltas(Collection<? extends ItemDelta<?, ?>> itemDeltas) throws SchemaException {
        lensContext.getFocusContextRequired().swallowToSecondaryDelta(itemDeltas);
    }

    @Override
    @Nullable LensContext<?> getLensContextIfPresent() {
        return lensContext;
    }
}
