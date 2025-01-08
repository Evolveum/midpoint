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

import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.correlation.CorrelatorContextCreator;
import com.evolveum.midpoint.model.impl.lens.projector.focus.consolidation.DeltaSetTripleMapConsolidation.APrioriDeltaProvider;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep.*;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.PathSet;

import com.evolveum.midpoint.schema.CorrelatorDiscriminator;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.sync.SynchronizationContext;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import static com.evolveum.midpoint.prism.Referencable.getOid;

/**
 * Evaluation of inbound mappings for a single shadow only, e.g., for the purposes of correlation
 * or association value synchronization.
 */
public class SingleShadowInboundsProcessing<T extends Containerable> extends AbstractInboundsProcessing<T> {

    private static final Trace LOGGER = TraceManager.getTrace(SingleShadowInboundsProcessing.class);

    private static final String OP_EVALUATE = SingleShadowInboundsProcessing.class.getName() + ".evaluate";

    @NotNull private final SingleShadowInboundsProcessingContext<T> ctx;

    private SingleShadowInboundsProcessing(
            @NotNull SingleShadowInboundsProcessingContext<T> ctx,
            @NotNull MappingEvaluationEnvironment env) {
        super(env);
        this.ctx = ctx;
    }

    public static <C extends Containerable> C evaluate(SingleShadowInboundsProcessingContext<C> ctx, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        OperationResult result = parentResult.subresult(OP_EVALUATE)
                .addArbitraryObjectAsParam("shadow", ctx.getShadowLikeValue())
                .build();
        try {

            new SingleShadowInboundsProcessing<>(ctx, createEnv(ctx))
                    .executeToDeltas(result);

            var focus = ctx.getPreFocus();
            LOGGER.debug("Focus:\n{}", focus.debugDumpLazily(1));
            return focus;

        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    public static SingleShadowInboundsProcessing<?> evaluateToTripleMap(
            SingleShadowInboundsProcessingContext<?> ctx, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {
        OperationResult result = parentResult.subresult(OP_EVALUATE)
                .addArbitraryObjectAsParam("shadow", ctx.getShadowLikeValue())
                .build();
        try {

            var processing = new SingleShadowInboundsProcessing<>(ctx, createEnv(ctx));
            processing.executeToTriples(result);

            LOGGER.debug("Triple map:\n{}", DebugUtil.debugDumpLazily(processing.getOutputTripleMap(), 1));

            return processing;

        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    private static <C extends Containerable> @NotNull MappingEvaluationEnvironment createEnv(
            SingleShadowInboundsProcessingContext<C> ctx) {
        return new MappingEvaluationEnvironment(
                "inbounds processing of " + ctx.getShadowLikeValue(),
                ModelBeans.get().clock.currentTimeXMLGregorianCalendar(),
                ctx.getTask());
    }

    /**
     * Collects mappings for the given shadow.
     */
    void prepareMappings(OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {

        try {
            var preFocusPcv = ctx.getPreFocusAsPcv();
            new SingleShadowInboundsPreparation<>(
                    evaluationRequestsMap,
                    new LimitedInboundsSource(ctx),
                    new LimitedInboundsTarget<>(preFocusPcv, getFocusDefinition(preFocusPcv), itemDefinitionMap),
                    new LimitedInboundsContext(ctx, getCorrelationItemPaths(result), env),
                    lResult -> {})
                    .prepareOrEvaluate(result);
        } catch (StopProcessingProjectionException e) {
            // Should be used only in clockwork processing.
            throw new IllegalStateException("Unexpected 'stop processing' exception: " + e.getMessage(), e);
        }
    }

    /** We need to get paths to all correlation items - to enable pre-inbounds for the respective attributes. */
    private PathSet getCorrelationItemPaths(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException {
        CorrelatorContext<?> correlatorContext =
                CorrelatorContextCreator.createRootContext(
                        getCorrelationDefinitionBean(),
                        CorrelatorDiscriminator.forSynchronization(),
                        getObjectTemplate(result),
                        ctx.getSystemConfiguration());
        PathSet paths = correlatorContext.getConfiguration().getCorrelationItemPaths();
        LOGGER.trace("Correlation items: {}", paths);
        return paths;
    }

    private @NotNull CorrelationDefinitionType getCorrelationDefinitionBean() throws SchemaException, ConfigurationException {
        return Objects.requireNonNullElseGet(
                ctx.getInboundProcessingDefinition().getCorrelation(),
                () -> new CorrelationDefinitionType());
    }

    private ObjectTemplateType getObjectTemplate(OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException {
        String archetypeOid = ctx.getArchetypeOid();
        if (archetypeOid == null) {
            return null;
        }
        ArchetypePolicyType policy = beans.archetypeManager.getPolicyForArchetype(archetypeOid, result);
        if (policy == null) {
            return null;
        }
        String templateOid = getOid(policy.getObjectTemplateRef());
        if (templateOid == null) {
            return null;
        }
        return beans.archetypeManager.getExpandedObjectTemplate(templateOid, env.task.getExecutionMode(), result);
    }

    private @NotNull PrismContainerDefinition<T> getFocusDefinition(@Nullable PrismContainerValue<T> focus) {
        // The interface expects nullable focus, but in fact we always have a non-null focus here
        Objects.requireNonNull(focus, "no focus");
        if (focus.getDefinition() != null) {
            return focus.getDefinition();
        } else {
            // FIXME brutal hack - may or may not work for all container types
            //noinspection unchecked
            return (PrismContainerDefinition<T>) beans.prismContext.getSchemaRegistry()
                    .findContainerDefinitionByCompileTimeClass(focus.asContainerable().getClass());
        }
    }

    @Override
    void applyComputedDeltas(Collection<? extends ItemDelta<?, ?>> itemDeltas) throws SchemaException {
        LOGGER.trace("Applying deltas to the pre-focus:\n{}", DebugUtil.debugDumpLazily(itemDeltas, 1));
        ItemDeltaCollectionsUtil.applyTo(
                itemDeltas, ctx.getPreFocusAsPcv());
    }

    @Override
    @NotNull
    Function<ItemPath, Boolean> getFocusPrimaryItemDeltaExistsProvider() {
        return itemPath -> false; // No focus primary item deltas at this point. (We don't know the focus yet.)
    }

    @Override
    @Nullable PrismContainerValue<T> getTargetNew() {
        return ctx.getPreFocusAsPcv();
    }

    @Override
    @Nullable PrismContainerValue<T> getTarget() throws SchemaException {
        return null;
    }

    @Override
    protected @NotNull APrioriDeltaProvider getFocusAPrioriDeltaProvider() {
        return APrioriDeltaProvider.none(); // No focus -> no a priori delta for it.
    }

    @Override
    @Nullable
    LensContext<?> getLensContextIfPresent() {
        return null; // No lens context at this time.
    }

    // TODO !!!!!!!!!!!!!!
    public VariablesMap getVariablesMap() {
        VariablesMap variables = ModelImplUtils.getDefaultVariablesMap(
                null, // FIXME
                ctx.getShadowIfPresent(),
                ctx.getResource(),
                ctx.getSystemConfiguration());
        variables.put(ExpressionConstants.VAR_SYNCHRONIZATION_CONTEXT, ctx, SynchronizationContext.class);
        return variables;
    }
}
