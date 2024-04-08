/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep;

import com.evolveum.midpoint.model.common.mapping.MappingBuilder;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.MappingEvaluationRequests;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.repo.common.expression.VariableProducer;
import com.evolveum.midpoint.schema.config.AbstractMappingConfigItem;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.processor.ShadowAssociationValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.evolveum.midpoint.repo.common.expression.ExpressionUtil.getPath;
import static com.evolveum.midpoint.schema.constants.ExpressionConstants.*;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;

/**
 * Source item (attribute, association, and so on) for which mapping(s) have to be created.
 *
 * It exists mainly to allow gathering all such requests first, then looking if we need to load the resource object,
 * and then create all the mappings with the resource object loaded.
 */
class MappedItem<V extends PrismValue, D extends ItemDefinition<?>, T extends Containerable> {

    private static final Trace LOGGER = TraceManager.getTrace(MappedItem.class);

    private final MSource source;
    private final Target<T> target;
    private final Context context;

    /** [EP:M:IM] DONE These mappings must come from `source.resource`. Currently it seems so. */
    private final Collection<? extends AbstractMappingConfigItem<?>> mappings;
    private final ItemPath implicitSourcePath;
    final String itemDescription;
    private final ItemDelta<V, D> itemAPrioriDelta;
    private final D itemDefinition;
    private final ItemProvider<V, D> itemProvider;
    private final PostProcessor<V, D> postProcessor;
    private final VariableProducer variableProducer;
    @NotNull private final ProcessingMode processingMode; // Never NONE

    @NotNull private final ModelBeans beans = ModelBeans.get();

    MappedItem(
            MSource source,
            Target<T> target,
            Context context,
            Collection<? extends AbstractMappingConfigItem<?>> mappings,
            ItemPath implicitSourcePath,
            String itemDescription,
            ItemDelta<V, D> itemAPrioriDelta,
            D itemDefinition,
            ItemProvider<V, D> itemProvider,
            PostProcessor<V, D> postProcessor,
            VariableProducer variableProducer,
            @NotNull ProcessingMode processingMode) {
        this.source = source;
        this.target = target;
        this.context = context;
        this.mappings = mappings;
        this.implicitSourcePath = implicitSourcePath;
        this.itemDescription = itemDescription;
        this.itemAPrioriDelta = itemAPrioriDelta;
        this.itemDefinition = itemDefinition;
        this.itemProvider = itemProvider;
        this.postProcessor = postProcessor;
        this.variableProducer = variableProducer;
        this.processingMode = processingMode;
        argCheck(processingMode != ProcessingMode.NONE, "Processing mode cannot be NONE");
    }

    /**
     * Creates the respective mapping(s) and puts them into `evaluationRequestsBeingCollected` parameter.
     */
    void createMappings(@NotNull MappingEvaluationRequests evaluationRequestsBeingCollected, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {

        boolean fromAbsoluteState =
                processingMode == ProcessingMode.ABSOLUTE_STATE
                        || processingMode == ProcessingMode.ABSOLUTE_STATE_IF_KNOWN;

        if (fromAbsoluteState && !source.isAbsoluteStateAvailable()) {
            LOGGER.trace(
                    "Skipping inbound mapping(s) for {} as they should be processed from absolute state, but we don't have one",
                    itemDescription);
            return;
        }

        Item<V, D> currentProjectionItem = itemProvider.provide();

        if (postProcessor != null) {
            postProcessor.postProcess(itemAPrioriDelta, currentProjectionItem);
        }

        LOGGER.trace("""
                        Creating {} inbound mapping(s) for {} in {} ({}). Relevant values are:
                        - a priori item delta:
                        {}
                        - current item:
                        {}""",
                mappings.size(),
                itemDescription,
                source.getProjectionHumanReadableNameLazy(),
                fromAbsoluteState ? "absolute mode" : "relative mode",
                DebugUtil.debugDumpLazily(itemAPrioriDelta, 1),
                DebugUtil.debugDumpLazily(currentProjectionItem, 1));

        if (currentProjectionItem != null && currentProjectionItem.hasRaw()) {
            throw new SystemException("Property " + currentProjectionItem + " has raw parsing state,"
                    + " such property cannot be used in inbound expressions");
        }

        source.setValueMetadata(currentProjectionItem, itemAPrioriDelta, result);

        ResourceType resource = source.getResource();

        // Value for the $shadow ($projection, $account) variable.
        // TODO Why do we use "object new" here? (We should perhaps go with ODO, shouldn't we?)
        //  Bear in mind that the value might not contain the full shadow (for example)
        PrismObject<ShadowType> shadowVariableValue = source.getResourceObjectNew();
        PrismObjectDefinition<ShadowType> shadowVariableDef = getShadowDefinition(shadowVariableValue);

        Source<V, D> defaultSource = new Source<>(
                currentProjectionItem,
                itemAPrioriDelta,
                null,
                ExpressionConstants.VAR_INPUT_QNAME,
                itemDefinition);

        defaultSource.recompute();

        for (AbstractMappingConfigItem<?> mappingCI : mappings) {

            AbstractMappingType mappingBean = mappingCI.value();

            String channel = source.getChannel();
            if (!MappingImpl.isApplicableToChannel(mappingBean, channel)) {
                LOGGER.trace("Mapping is not applicable to channel {}", channel);
                continue;
            }
            if (!context.env.task.canSee(mappingBean)) {
                LOGGER.trace("Mapping is not applicable to the task execution mode");
                continue;
            }

            String contextDescription = "inbound expression for " + itemDescription + " in " + resource;

            ItemPath targetFullPath = getTargetFullPath(mappingBean, contextDescription); // without variable, with prefix

            //noinspection unchecked
            MappingBuilder<V, D> builder = beans.mappingFactory.<V, D>createMappingBuilder()
                    .mapping((ConfigurationItem<MappingType>) mappingCI) // [EP:M:IM] DONE (mapping bean is from the resource, see callers)
                    .mappingKind(MappingKindType.INBOUND)
                    .implicitSourcePath(implicitSourcePath)
                    .targetPathOverride(targetFullPath)
                    .targetPathExecutionOverride(source.determineTargetPathExecutionOverride(targetFullPath))
                    .contextDescription(contextDescription)
                    .defaultSource(defaultSource)
                    .targetContext(target.targetDefinition)
                    .addVariableDefinition(VAR_USER, target.getTargetRealValue(), target.targetDefinition)
                    .addVariableDefinition(ExpressionConstants.VAR_FOCUS, target.getTargetRealValue(), target.targetDefinition)
                    .addAliasRegistration(VAR_USER, ExpressionConstants.VAR_FOCUS)
                    .addVariableDefinition(ExpressionConstants.VAR_ACCOUNT, shadowVariableValue, shadowVariableDef)
                    .addVariableDefinition(ExpressionConstants.VAR_SHADOW, shadowVariableValue, shadowVariableDef)
                    .addVariableDefinition(ExpressionConstants.VAR_PROJECTION, shadowVariableValue, shadowVariableDef)
                    .addAliasRegistration(ExpressionConstants.VAR_ACCOUNT, ExpressionConstants.VAR_PROJECTION)
                    .addAliasRegistration(ExpressionConstants.VAR_SHADOW, ExpressionConstants.VAR_PROJECTION)
                    .addVariableDefinition(ExpressionConstants.VAR_ASSOCIATED_SHADOW, getAssociatedShadow(currentProjectionItem), shadowVariableDef)
                    .addVariableDefinition(ExpressionConstants.VAR_RESOURCE, resource, resource.asPrismObject().getDefinition())
                    .addVariableDefinition(ExpressionConstants.VAR_CONFIGURATION,
                            context.getSystemConfiguration(), getSystemConfigurationDefinition())
                    .addVariableDefinition(ExpressionConstants.VAR_OPERATION, context.getOperation(), String.class)
                    .variableResolver(variableProducer)
                    .valuePolicySupplier(context.createValuePolicySupplier())
                    .originType(OriginType.INBOUND)
                    .originObject(resource)
                    .now(context.env.now);

            if (!target.isFocusBeingDeleted()) {
                builder.originalTargetValues(
                        ExpressionUtil.computeTargetValues(
                                source.determineTargetPathExecutionOverride(targetFullPath) != null ? source.determineTargetPathExecutionOverride(targetFullPath) : targetFullPath,
                                new TypedValue<>(target.getTargetRealValue(), target.targetDefinition),
                                builder.getVariables(),
                                beans.mappingFactory.getObjectResolver(),
                                "resolving target values",
                                context.env.task,
                                result));
            }

            MappingImpl<V, D> mapping = builder.build();

            // We check the weak mapping skipping using the declared path, not the overridden path pointing to identities data.
            if (checkWeakSkip(mapping, targetFullPath)) {
                LOGGER.trace("Skipping because of mapping is weak and focus property has already a value");
                continue;
            }

            rememberItemDefinition(mapping, targetFullPath, source.determineTargetPathExecutionOverride(targetFullPath));

            ItemPath realTargetPath = mapping.getOutputPath();
            evaluationRequestsBeingCollected.add(realTargetPath, source.createMappingRequest(mapping));
        }
    }

    // FIXME brutal hack
    private PrismObject<ShadowType> getAssociatedShadow(Item<V, D> currentProjectionItem) {
        if (currentProjectionItem == null
                || !currentProjectionItem.getPath().startsWith(ShadowType.F_ASSOCIATIONS)) {
            return null;
        }
        List<V> values = currentProjectionItem.getValues();
        if (values.size() != 1) {
            return null;
        }
        return ((ShadowAssociationValue) values.get(0)).getShadowBean().asPrismObject();
    }

    private @NotNull ItemPath getTargetFullPath(AbstractMappingType mappingBean, String errorCtxDesc)
            throws ConfigurationException {
        ItemPath path = getPath(mappingBean.getTarget());
        QName variable = path != null ? path.firstToVariableNameOrNull() : null;
        String varLocalPart = variable != null ? variable.getLocalPart() : null;
        ItemPath pathAfterVariable = path != null ? path.stripVariableSegment() : null;
        if (ItemPath.isEmpty(pathAfterVariable)) {
            throw new ConfigurationException("Empty target path in " + errorCtxDesc);
        }

        if (varLocalPart == null || VAR_TARGET.equals(varLocalPart)) {
            return target.getTargetPathPrefix().append(pathAfterVariable);
        } else if (VAR_USER.equals(varLocalPart) || VAR_FOCUS.equals(varLocalPart)) {
            return pathAfterVariable;
        } else {
            throw new IllegalStateException(String.format(
                    "Unsupported variable in target path '%s' in %s. Only $focus, $user, and $target are allowed here.",
                            path, errorCtxDesc));
        }
    }

    private PrismObjectDefinition<ShadowType> getShadowDefinition(PrismObject<ShadowType> shadowNew) {
        if (shadowNew != null && shadowNew.getDefinition() != null) {
            return shadowNew.getDefinition();
        } else {
            return beans.prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
        }
    }

    private @NotNull PrismObjectDefinition<SystemConfigurationType> getSystemConfigurationDefinition() {
        PrismObject<SystemConfigurationType> config = context.getSystemConfiguration();
        if (config != null && config.getDefinition() != null) {
            return config.getDefinition();
        } else {
            return Objects.requireNonNull(
                    beans.prismContext.getSchemaRegistry()
                            .findObjectDefinitionByCompileTimeClass(SystemConfigurationType.class));
        }
    }

    private boolean checkWeakSkip(MappingImpl<?, ?> inbound, ItemPath declaredTargetPath) {
        if (inbound.getStrength() != MappingStrengthType.WEAK) {
            return false;
        }
        if (target.targetPcv != null) {
            Item<?, ?> item = target.targetPcv.findItem(declaredTargetPath);
            return item != null && !item.isEmpty();
        } else {
            return false;
        }
    }

    boolean doesRequireAbsoluteState() {
        return processingMode == ProcessingMode.ABSOLUTE_STATE;
    }

    private void rememberItemDefinition(MappingImpl<V, D> mapping, ItemPath declaredTargetPath, ItemPath targetPathOverride)
            throws ConfigurationException {
        D outputDefinition =
                MiscUtil.configNonNull(
                        mapping.getOutputDefinition(),
                        () -> "No definition for target item " + declaredTargetPath + " in " + mapping.getContextDescription());
        target.addItemDefinition(declaredTargetPath, outputDefinition);
        if (targetPathOverride != null) {
            ItemDefinition<?> clone = outputDefinition.clone();
            clone.mutator().setDynamic(true); // To serialize xsi:type along with the values.
            target.addItemDefinition(targetPathOverride, clone);
        }
    }

    @FunctionalInterface
    interface ItemProvider<V extends PrismValue, D extends ItemDefinition<?>> {
        Item<V, D> provide() throws SchemaException;
    }

    @FunctionalInterface
    interface PostProcessor<V extends PrismValue, D extends ItemDefinition<?>> {
        void postProcess(ItemDelta<V, D> aPrioriDelta, Item<V, D> currentItem) throws SchemaException;
    }
}
