/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep;

import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensObjectDeltaOperation;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.InboundMappingInContext;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingEvaluatorParams;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingInitializer;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingOutputProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingTimeEval;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.config.OriginProvider;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

public class ClockworkShadowInboundsPreparation<F extends FocusType> extends ShadowInboundsPreparation<F> {

    private static final Trace LOGGER = TraceManager.getTrace(ClockworkShadowInboundsPreparation.class);

    @NotNull private final LensProjectionContext projectionContext;
    @NotNull private final LensContext<F> lensContext;

    public ClockworkShadowInboundsPreparation(
            @NotNull LensProjectionContext projectionContext,
            @NotNull LensContext<F> lensContext,
            @NotNull PathKeyedMap<List<InboundMappingInContext<?, ?>>> mappingsMap,
            @NotNull PathKeyedMap<ItemDefinition<?>> itemDefinitionMap,
            @NotNull ClockworkContext context,
            @Nullable PrismObject<F> focus,
            @NotNull PrismObjectDefinition<F> focusDefinition) throws SchemaException, ConfigurationException {
        super(
                mappingsMap,
                new ClockworkSource(
                        projectionContext.getObjectCurrent(),
                        getAPrioriDelta(projectionContext),
                        projectionContext.getCompositeObjectDefinition(),
                        projectionContext,
                        context),
                new ClockworkTarget<>(lensContext, focus, focusDefinition, itemDefinitionMap),
                context
        );
        this.projectionContext = projectionContext;
        this.lensContext = lensContext;
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

    /**
     * Processing for special (fixed-schema) properties such as credentials and activation.
     *
     * The code is rather strange. TODO revisit and clean up
     *
     * Also it is not clear why these mappings are not collected to the map for later execution,
     * just like regular mappings are.
     */
    private void evaluateSpecialInbounds(
            List<MappingType> inboundMappingBeans,
            ItemPath sourcePath, ItemPath targetPath) throws SchemaException,
            ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException {

        if (inboundMappingBeans == null || inboundMappingBeans.isEmpty()) {
            return;
        }

        LOGGER.trace("Collecting {} inbounds for special property {}", inboundMappingBeans.size(), sourcePath);

        PrismObject<F> focus = target.focus;
        if (focus == null) {
            LOGGER.trace("No current/new focus, skipping.");
            return;
        }
        if (!projectionContext.isFullShadow()) {
            // TODO - is this ok?
            LOGGER.trace("Full shadow not loaded, skipping.");
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
                    if (source.aPrioriDelta != null) {
                        specialAttributeDelta = source.aPrioriDelta.findItemDelta(sourcePath);
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

                    PrismObject<ShadowType> accountNew = this.projectionContext.getObjectNew();
                    builder.addVariableDefinition(ExpressionConstants.VAR_ACCOUNT, accountNew, ShadowType.class)
                            .addVariableDefinition(ExpressionConstants.VAR_SHADOW, accountNew, ShadowType.class)
                            .addVariableDefinition(ExpressionConstants.VAR_PROJECTION, accountNew, ShadowType.class)
                            .addAliasRegistration(ExpressionConstants.VAR_ACCOUNT, ExpressionConstants.VAR_PROJECTION)
                            .addAliasRegistration(ExpressionConstants.VAR_SHADOW, ExpressionConstants.VAR_PROJECTION)
                            .addVariableDefinition(ExpressionConstants.VAR_RESOURCE, this.projectionContext.getResource(), ResourceType.class)
                            .valuePolicySupplier(context.createValuePolicySupplier())
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
                    PrismProperty result = focusDefinition.findPropertyDefinition(targetPath).instantiate();
                    //noinspection unchecked
                    result.addAll(PrismValueCollectionsUtil.cloneCollection(outputTriple.getNonNegativeValues()));

                    PrismProperty targetPropertyNew = focus.findOrCreateProperty(targetPath);
                    PropertyDelta<?> delta;
                    if (ProtectedStringType.COMPLEX_TYPE.equals(targetPropertyNew.getDefinition().getTypeName())) {
                        // We have to compare this in a special way. The cipherdata may be different due to a different
                        // IV, but the value may still be the same
                        ProtectedStringType resultValue = (ProtectedStringType) result.getRealValue();
                        ProtectedStringType targetPropertyNewValue = (ProtectedStringType) targetPropertyNew.getRealValue();
                        try {
                            if (context.beans.protector.compareCleartext(resultValue, targetPropertyNewValue)) {
                                delta = null;
                            } else {
                                //noinspection unchecked
                                delta = targetPropertyNew.diff(result);
                            }
                        } catch (EncryptionException e) {
                            throw new SystemException(e.getMessage(), e);
                        }
                    } else {
                        //noinspection unchecked
                        delta = targetPropertyNew.diff(result);
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

        OriginProvider<MappingType> originProvider =
                item -> ConfigurationItemOrigin.inResourceOrAncestor(projectionContext.getResourceRequired());

        MappingEvaluatorParams<PrismValue, ItemDefinition<?>, F, F> params = new MappingEvaluatorParams<>();
        params.setMappingBeans(ConfigurationItem.ofList(inboundMappingBeans, originProvider));
        params.setMappingDesc("inbound mapping for " + sourcePath + " in " + projectionContext.getResource());
        params.setNow(context.env.now);
        params.setInitializer(initializer);
        params.setProcessor(processor);
        params.setAPrioriTargetObject(focus);
        params.setAPrioriTargetDelta(userPrimaryDelta);
        params.setTargetContext(lensContext.getFocusContext());
        params.setDefaultTargetItemPath(targetPath);
        params.setEvaluateCurrent(MappingTimeEval.CURRENT);
        params.setContext(lensContext);
        params.setHasFullTargetObject(true);
        context.beans.projectionMappingSetEvaluator.evaluateMappingsToTriples(params, context.env.task, context.result);
    }

    @Override
    void evaluateSpecialInbounds()
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        // TODO convert to mapping creation requests
        evaluateSpecialInbounds(source.resourceObjectDefinition.getPasswordInbound(),
                SchemaConstants.PATH_PASSWORD_VALUE, SchemaConstants.PATH_PASSWORD_VALUE);
        evaluateSpecialInbounds(getActivationInbound(ActivationType.F_ADMINISTRATIVE_STATUS),
                SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
        evaluateSpecialInbounds(getActivationInbound(ActivationType.F_VALID_FROM),
                SchemaConstants.PATH_ACTIVATION_VALID_FROM, SchemaConstants.PATH_ACTIVATION_VALID_FROM);
        evaluateSpecialInbounds(getActivationInbound(ActivationType.F_VALID_TO),
                SchemaConstants.PATH_ACTIVATION_VALID_TO, SchemaConstants.PATH_ACTIVATION_VALID_TO);
    }

    private List<MappingType> getActivationInbound(ItemName itemName) {
        ResourceBidirectionalMappingType biDirMapping = source.resourceObjectDefinition
                .getActivationBidirectionalMappingType(itemName);
        return biDirMapping != null ? biDirMapping.getInbound() : Collections.emptyList();
    }
}
