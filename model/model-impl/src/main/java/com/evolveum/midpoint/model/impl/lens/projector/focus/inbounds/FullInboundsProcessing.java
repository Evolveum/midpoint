/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds;

import static com.evolveum.midpoint.util.DebugUtil.lazy;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import com.evolveum.midpoint.model.api.InboundSourceData;
import com.evolveum.midpoint.model.impl.lens.LensObjectDeltaOperation;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep.*;

import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingEvaluatorParams;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingInitializer;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingOutputProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingTimeEval;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.config.MappingConfigItem;
import com.evolveum.midpoint.schema.config.OriginProvider;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.projector.focus.consolidation.DeltaSetTripleMapConsolidation.APrioriDeltaProvider;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

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
                            InboundSourceData.forShadow(
                                    projectionContext.getObjectCurrent(),
                                    getAPrioriDelta(projectionContext),
                                    projectionContext.getCompositeObjectDefinitionRequired()),
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
     * Computes a priori delta for given projection context.
     *
     * TODO revise this method
     */
    private static ObjectDelta<ShadowType> getAPrioriDelta(@NotNull LensProjectionContext projectionContext) {
        int wave = projectionContext.getLensContext().getProjectionWave();
        if (wave == 0) {
            return projectionContext.getSyncDelta();
        }
        if (wave == projectionContext.getWave() + 1) {
            // If this resource was processed in a previous wave ....
            // Normally, we take executed delta. However, there are situations (like preview changes - i.e. projector without execution),
            // when there is no executed delta. In that case we take standard primary + secondary delta.
            // TODO is this really correct? Think if the following can happen:
            // - NOT previewing
            // - no executed deltas but
            // - existing primary/secondary delta.
            List<LensObjectDeltaOperation<ShadowType>> executed = projectionContext.getExecutedDeltas();
            if (!executed.isEmpty()) {
                // TODO why the last one?
                return executed.get(executed.size() - 1).getObjectDelta();
            } else {
                return projectionContext.getSummaryDelta(); // TODO check this
            }
        }
        return null;
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

    class SpecialInboundsEvaluatorImpl implements SingleShadowInboundsPreparation.SpecialInboundsEvaluator {

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
            // TODO how exactly can be the password cached? Normally it's hashed, and that means it is not usable for inbounds.
            var passwordValueLoaded = projectionContext.isPasswordValueLoaded();
            var activationLoaded = projectionContext.isActivationLoaded();
            // TODO convert to mapping creation requests
            evaluateSpecialInbounds( // [EP:M:IM] DONE, obviously belonging to the resource
                    inboundsSource.getInboundDefinition().getPasswordInbound(),
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

                        ItemDelta<PrismPropertyValue<?>, PrismPropertyDefinition<?>> specialAttributeDelta;
                        if (inboundsSource.getAPrioriDelta() != null) {
                            specialAttributeDelta = inboundsSource.getAPrioriDelta().findItemDelta(sourcePath);
                        } else {
                            specialAttributeDelta = null;
                        }
                        ItemDeltaItem<PrismPropertyValue<?>, PrismPropertyDefinition<?>> sourceIdi =
                                projectionContext.getObjectDeltaObject().findIdi(sourcePath);
                        if (specialAttributeDelta == null) {
                            specialAttributeDelta = sourceIdi.getDelta();
                        }
                        Source<PrismPropertyValue<?>, PrismPropertyDefinition<?>> source = new Source<>(
                                sourceIdi.getItemOld(), specialAttributeDelta, sourceIdi.getItemOld(),
                                ExpressionConstants.VAR_INPUT_QNAME,
                                sourceIdi.getDefinition());
                        builder.defaultSource(source)
                                .addVariableDefinition(ExpressionConstants.VAR_USER, focus, UserType.class)
                                .addVariableDefinition(ExpressionConstants.VAR_FOCUS, focus, FocusType.class)
                                .addAliasRegistration(ExpressionConstants.VAR_USER, ExpressionConstants.VAR_FOCUS);

                        PrismObject<ShadowType> account = this.inboundsSource.getSourceData().getShadowIfPresent();
                        builder.addVariableDefinition(ExpressionConstants.VAR_ACCOUNT, account, ShadowType.class)
                                .addVariableDefinition(ExpressionConstants.VAR_SHADOW, account, ShadowType.class)
                                .addVariableDefinition(ExpressionConstants.VAR_PROJECTION, account, ShadowType.class)
                                .addAliasRegistration(ExpressionConstants.VAR_ACCOUNT, ExpressionConstants.VAR_PROJECTION)
                                .addAliasRegistration(ExpressionConstants.VAR_SHADOW, ExpressionConstants.VAR_PROJECTION)
                                .addVariableDefinition(ExpressionConstants.VAR_RESOURCE, this.projectionContext.getResource(), ResourceType.class)
                                .valuePolicySupplier(inboundsContext.createValuePolicySupplier())
                                .mappingKind(MappingKindType.INBOUND)
                                .implicitSourcePath(sourcePath)
                                .implicitTargetPath(targetPath)
                                .originType(OriginType.INBOUND)
                                .originObject(this.projectionContext.getResource());

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
                        PrismProperty mResult = focusDefinition.findPropertyDefinition(targetPath).instantiate();
                        //noinspection unchecked
                        mResult.addAll(PrismValueCollectionsUtil.cloneCollection(outputTriple.getNonNegativeValues()));

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
            beans.projectionMappingSetEvaluator.evaluateMappingsToTriples(params, inboundsContext.getEnv().task, result);
        }

        private List<MappingType> getActivationInbound(ItemName itemName) {
            ResourceBidirectionalMappingType biDirMapping =
                    inboundsSource.getInboundDefinition().getActivationBidirectionalMappingType(itemName);
            return biDirMapping != null ? biDirMapping.getInbound() : Collections.emptyList();
        }
    }
}
