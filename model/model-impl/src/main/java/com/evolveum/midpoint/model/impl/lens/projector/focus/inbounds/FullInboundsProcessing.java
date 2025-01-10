/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asPrismObject;
import static com.evolveum.midpoint.util.DebugUtil.lazy;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.projector.focus.consolidation.DeltaSetTripleMapConsolidation.APrioriDeltaProvider;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep.FullInboundsContext;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep.FullInboundsSource;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep.FullInboundsTarget;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep.SingleShadowInboundsPreparation;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.*;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.config.MappingConfigItem;
import com.evolveum.midpoint.schema.config.OriginProvider;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Evaluation of inbound mappings from all projections in given lens context. This is the "full mode".
 * See the description in {@link AbstractInboundsProcessing}.
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
    void prepareMappings(OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {

        for (var projectionContext : lensContext.getProjectionContexts()) {

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
                // Normally, contexts whose shadows are deleted disappear in the wave following their deletion (as part of
                // removing rotten contexts by the context loader; the key is that their linkRef are deleted or marked as
                // "related"). However, there may be situations when the deletion is attempted but not executed
                // because of the resource unavailability. The result (before this code was created) was that
                // the inbound mappings were executed, created a difference in behavior - comparing to the case
                // in which the deletion is executed successfully. The shadow caching uncovered this problem, as the
                // inbounds no longer require a load-from-resource operation.
                if (projectionContext.isDelete() && projectionContext.isCompleted()) {
                    LOGGER.trace("Skipping processing of inbound expressions for projection {} because is is deleted and completed",
                            lazy(projectionContext::getHumanReadableName));
                    result.recordNotApplicable("projection is deleted and completed");
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
                    // Here we prepare all those complex source/target/context objects ...
                    PrismObject<F> objectCurrentOrNew = lensContext.getFocusContext().getObjectCurrentOrNew();
                    var inboundsContext = new FullInboundsContext(lensContext, env);
                    var inboundsSource = new FullInboundsSource(
                            getInboundSourceData(projectionContext),
                            projectionContext.getCompositeObjectDefinition(),
                            projectionContext,
                            inboundsContext);
                    var inboundsTarget = new FullInboundsTarget<>(
                            lensContext,
                            objectCurrentOrNew,
                            getFocusDefinition(objectCurrentOrNew),
                            itemDefinitionMap,
                            ItemPath.EMPTY_PATH);

                    // ... and run the preparation itself
                    var preparation = new SingleShadowInboundsPreparation<>(
                            evaluationRequestsMap, inboundsSource, inboundsTarget, inboundsContext,
                            new SpecialInboundsEvaluatorImpl(inboundsSource, inboundsTarget, inboundsContext, projectionContext));

                    preparation.prepareOrEvaluate(result);

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

    /**
     * Computes the source data (object + delta) for given projection context.
     */
    private static InboundSourceData getInboundSourceData(@NotNull LensProjectionContext projectionContext) {
        var currentShadow = projectionContext.getObjectCurrent();

        int wave = projectionContext.getLensContext().getProjectionWave();

        if (wave == 0) {
            return InboundSourceData.forShadow(
                    currentShadow, // Current is OK here, as this is the state "at the beginning".
                    currentShadow, // After we try to derive deltas from the cached shadows, this code will change.
                    projectionContext.getSyncDelta(),
                    false);
        } else if (wave == projectionContext.getWave() + 1) {
            // We are in the wave that follows right after this projection context was projected/executed in.
            // So, we would like to use the delta that was executed in that wave.
            var odos = projectionContext.getExecutedDeltas(projectionContext.getWave());
            if (odos.isEmpty()) {
                return InboundSourceData.forShadowWithoutDelta(currentShadow);
            } else {
                // Normally, here should be only one delta. But sometimes the waves are repeated.
                // So, let's take the last one.
                var odo = odos.get(odos.size() - 1);
                var baseObject = odo.getBaseObject(); // should be non-null, except for ADD delta
                return InboundSourceData.forShadow(
                        asPrismObject(baseObject), // Maybe we should take the base object from the first odo? Not sure.
                        currentShadow,
                        odo.getObjectDelta(),
                        true);
            }
        } else {
            return InboundSourceData.forShadowWithoutDelta(currentShadow);
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

    private class SpecialInboundsEvaluatorImpl implements SingleShadowInboundsPreparation.SpecialInboundsEvaluator {

        @NotNull private final FullInboundsSource inboundsSource;
        @NotNull private final FullInboundsTarget<F> inboundsTarget;
        @NotNull private final FullInboundsContext inboundsContext;
        @NotNull private final LensProjectionContext projectionContext;

        SpecialInboundsEvaluatorImpl(
                @NotNull FullInboundsSource inboundsSource,
                @NotNull FullInboundsTarget<F> inboundsTarget,
                @NotNull FullInboundsContext inboundsContext,
                @NotNull LensProjectionContext projectionContext) {
            this.inboundsSource = inboundsSource;
            this.inboundsTarget = inboundsTarget;
            this.inboundsContext = inboundsContext;
            this.projectionContext = projectionContext;
        }

        @Override
        public void evaluateSpecialInbounds(OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {
            var passwordValueLoaded = projectionContext.isPasswordValueLoaded();
            var activationLoaded = projectionContext.isActivationLoaded();
            // TODO convert to mapping creation requests
            evaluateSpecialInbounds( // [EP:M:IM] DONE, obviously belonging to the resource
                    inboundsSource.getInboundProcessingDefinition().getPasswordInboundMappings(),
                    SchemaConstants.PATH_PASSWORD_VALUE, SchemaConstants.PATH_PASSWORD_VALUE,
                    passwordValueLoaded,
                    result);
            evaluateSpecialInbounds( // [EP:M:IM] DONE, obviously belonging to the resource
                    getActivationInbound(ActivationType.F_ADMINISTRATIVE_STATUS),
                    SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
                    activationLoaded,
                    result);
            evaluateSpecialInbounds( // [EP:M:IM] DONE, obviously belonging to the resource
                    getActivationInbound(ActivationType.F_VALID_FROM),
                    SchemaConstants.PATH_ACTIVATION_VALID_FROM, SchemaConstants.PATH_ACTIVATION_VALID_FROM,
                    activationLoaded,
                    result);
            evaluateSpecialInbounds( // [EP:M:IM] DONE, obviously belonging to the resource
                    getActivationInbound(ActivationType.F_VALID_TO),
                    SchemaConstants.PATH_ACTIVATION_VALID_TO, SchemaConstants.PATH_ACTIVATION_VALID_TO,
                    activationLoaded,
                    result);
        }

        /**
         * Processing for special (fixed-schema) properties such as credentials and activation.
         *
         * The code is rather strange. TODO revisit and clean up
         *
         * Also it is not clear why these mappings are not collected to the map for later execution,
         * just like regular mappings are.
         *
         * [EP:M:IM] DONE 4/4
         */
        private void evaluateSpecialInbounds(
                List<MappingType> inboundMappingBeans,
                ItemPath sourcePath, ItemPath targetPath,
                boolean sourceItemAvailable, OperationResult result)
                throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
                ConfigurationException, SecurityViolationException {

            if (inboundMappingBeans == null || inboundMappingBeans.isEmpty()) {
                return;
            }

            LOGGER.trace("Collecting {} inbounds for special property {}", inboundMappingBeans.size(), sourcePath);

            F focus = inboundsTarget.getTargetRealValue();
            if (focus == null) {
                LOGGER.trace("No current/new focus, skipping.");
                return;
            }
            if (!sourceItemAvailable) {
                LOGGER.trace("Source item not loaded, skipping.");
                return;
            }

            ObjectDelta<F> userPrimaryDelta = lensContext.getFocusContext().getPrimaryDelta();
            if (userPrimaryDelta != null) {
                PropertyDelta<?> primaryPropDelta = userPrimaryDelta.findPropertyDelta(targetPath);
                if (primaryPropDelta != null && primaryPropDelta.isReplace()) {
                    LOGGER.trace("Primary delta of 'replace' overrides any inbounds, skipping. Delta: {}", primaryPropDelta);
                    return;
                }
            }

            MappingInitializer<PrismValue, ItemDefinition<?>> initializer =
                    (builder) -> {
                        if (projectionContext.getObjectNew() == null) {
                            String message = "Recomputing account " + projectionContext.getKey()
                                    + " results in null new account. Something must be really broken.";
                            LOGGER.error(message);
                            LOGGER.trace("Account context:\n{}", projectionContext.debugDumpLazily());
                            throw new SystemException(message);
                        }

                        ItemDeltaItem<PrismPropertyValue<?>, PrismPropertyDefinition<?>> sourceIdi =
                                projectionContext.getObjectDeltaObject().findIdi(sourcePath);
                        var itemDefinition = sourceIdi.getDefinition(); // maybe there's a simpler way to get the definition

                        var itemOld = inboundsSource.getSourceData().getItemOld(sourcePath);
                        var itemDelta = inboundsSource.getSourceData().getEffectiveItemDelta(sourcePath);
                        var source = new Source<>(
                                itemOld, itemDelta, null,
                                ExpressionConstants.VAR_INPUT_QNAME,
                                itemDefinition);
                        source.recompute();

                        builder.defaultSource(source)
                                .addVariableDefinition(ExpressionConstants.VAR_USER, focus, UserType.class)
                                .addVariableDefinition(ExpressionConstants.VAR_FOCUS, focus, FocusType.class)
                                .addAliasRegistration(ExpressionConstants.VAR_USER, ExpressionConstants.VAR_FOCUS);

                        PrismObject<ShadowType> account = this.inboundsSource.getSourceData().getShadowVariableValue();
                        builder.addVariableDefinition(ExpressionConstants.VAR_ACCOUNT, account, ShadowType.class)
                                .addVariableDefinition(ExpressionConstants.VAR_SHADOW, account, ShadowType.class)
                                .addVariableDefinition(ExpressionConstants.VAR_PROJECTION, account, ShadowType.class)
                                .addAliasRegistration(ExpressionConstants.VAR_ACCOUNT, ExpressionConstants.VAR_PROJECTION)
                                .addAliasRegistration(ExpressionConstants.VAR_SHADOW, ExpressionConstants.VAR_PROJECTION)
                                .addVariableDefinition(ExpressionConstants.VAR_RESOURCE, projectionContext.getResource(), ResourceType.class)
                                .valuePolicySupplier(inboundsContext.createValuePolicySupplier())
                                .mappingKind(MappingKindType.INBOUND)
                                .implicitSourcePath(sourcePath)
                                .implicitTargetPath(targetPath)
                                .originType(OriginType.INBOUND)
                                .originObject(this.projectionContext.getResource())
                                .mappingSpecification(
                                        inboundsSource.createMappingSpec(builder.getMappingName(), itemDefinition));

                        return builder;
                    };

            MappingOutputProcessor<PrismValue> processor =
                    (mappingOutputPath, outputStruct) -> {
                        PrismValueDeltaSetTriple<PrismValue> outputTriple = outputStruct.getOutputTriple();
                        if (outputTriple == null) {
                            LOGGER.trace("Mapping for property {} evaluated to null. Skipping inbound processing for that property.", sourcePath);
                            return false;
                        }

                        PrismObjectDefinition<F> focusDefinition = lensContext.getFocusContext().getObjectDefinition();
                        @SuppressWarnings("rawtypes")
                        PrismProperty mResult = focusDefinition.findPropertyDefinition(targetPath).instantiate();
                        //noinspection unchecked
                        mResult.addAll(PrismValueCollectionsUtil.cloneCollection(outputTriple.getNonNegativeValues()));

                        @SuppressWarnings("rawtypes")
                        PrismProperty targetPropertyNew = focus.asPrismObject().findOrCreateProperty(targetPath);
                        PropertyDelta<?> delta;
                        if (ProtectedStringType.COMPLEX_TYPE.equals(targetPropertyNew.getDefinition().getTypeName())) {
                            // We have to compare this in a special way. The cipherdata may be different due to a different
                            // IV, but the value may still be the same
                            ProtectedStringType resultValue = (ProtectedStringType) mResult.getRealValue();
                            ProtectedStringType targetPropertyNewValue = (ProtectedStringType) targetPropertyNew.getRealValue();
                            try {
                                if (beans.protector.compareCleartext(resultValue, targetPropertyNewValue)) {
                                    delta = null;
                                } else {
                                    //noinspection unchecked
                                    delta = targetPropertyNew.diff(mResult);
                                }
                            } catch (EncryptionException e) {
                                throw new SystemException(e.getMessage(), e);
                            }
                        } else {
                            //noinspection unchecked
                            delta = targetPropertyNew.diff(mResult);
                        }
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("targetPropertyNew:\n{}\ndelta:\n{}", targetPropertyNew.debugDump(1), DebugUtil.debugDump(delta, 1));
                        }
                        if (delta != null && !delta.isEmpty()) {
                            delta.setParentPath(targetPath.allExceptLast());
                            lensContext.getFocusContext().swallowToSecondaryDelta(delta);
                        }
                        return false;
                    };

            // [EP:M:IM] DONE, see above
            OriginProvider<MappingType> originProvider =
                    item -> ConfigurationItemOrigin.inResourceOrAncestor(projectionContext.getResourceRequired());

            MappingEvaluatorParams<PrismValue, ItemDefinition<?>, F, F> params = new MappingEvaluatorParams<>();
            params.setMappingConfigItems( // [EP:M:IM] DONE, see above
                    ConfigurationItem.ofList(inboundMappingBeans, originProvider, MappingConfigItem.class));
            params.setMappingDesc("inbound mapping for " + sourcePath + " in " + projectionContext.getResource());
            params.setNow(inboundsContext.getEnv().now);
            params.setInitializer(initializer);
            params.setProcessor(processor);
            //noinspection unchecked
            params.setAPrioriTargetObject((PrismObject<F>) focus.asPrismObject());
            params.setAPrioriTargetDelta(userPrimaryDelta);
            params.setTargetContext(lensContext.getFocusContext());
            params.setDefaultTargetItemPath(targetPath);
            params.setEvaluateCurrent(MappingTimeEval.CURRENT);
            params.setContext(lensContext);
            params.setTargetValueAvailable(true);
            try {
                beans.projectionMappingSetEvaluator.evaluateMappingsToTriples(params, inboundsContext.getEnv().task, result);
            } catch (MappingLoader.NotLoadedException e) {
                // FIXME Originally, NotLoadedException was ObjectNotFoundException anyway. This brings that old behavior here.
                //  Please fix this some day.
                throw new ObjectNotFoundException("Projection was not loaded: " + e.getMessage(), e);
            }
        }

        private List<MappingType> getActivationInbound(ItemName itemName) {
            return inboundsSource.getInboundProcessingDefinition().getActivationInboundMappings(itemName);
        }
    }
}
