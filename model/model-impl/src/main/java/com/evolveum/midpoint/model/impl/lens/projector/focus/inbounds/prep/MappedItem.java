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
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.InboundMappingInContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.repo.common.expression.VariableProducer;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.evolveum.midpoint.repo.common.expression.ExpressionUtil.getPath;
import static com.evolveum.midpoint.schema.constants.ExpressionConstants.VAR_FOCUS;
import static com.evolveum.midpoint.schema.constants.ExpressionConstants.VAR_USER;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;

/**
 * Source item (attribute, association, and so on) for which mapping(s) have to be created.
 *
 * It it here mainly to allow gathering all such requests first, then looking if we need to load the resource object,
 * and then create all the mappings with the resource object loaded.
 */
class MappedItem<V extends PrismValue, D extends ItemDefinition<?>, F extends FocusType> {

    private static final Trace LOGGER = TraceManager.getTrace(MappedItem.class);

    private final MSource source;
    private final Target<F> target;
    private final Context context;

    /** [EP:M:IM] DONE These mappings must come from `source.resource`. Currently it seems so. */
    private final Collection<? extends MappingType> mappingBeans;
    private final ItemPath implicitSourcePath;
    final String itemDescription;
    private final ItemDelta<V, D> itemAPrioriDelta;
    private final D itemDefinition;
    private final ItemProvider<V, D> itemProvider;
    private final PostProcessor<V, D> postProcessor;
    private final VariableProducer variableProducer;
    @NotNull private final ProcessingMode processingMode; // Never NONE

    @NotNull private final ModelBeans beans;

    MappedItem(
            MSource source,
            Target<F> target,
            Context context,
            Collection<? extends MappingType> mappingBeans,
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
        this.mappingBeans = mappingBeans;
        this.implicitSourcePath = implicitSourcePath;
        this.itemDescription = itemDescription;
        this.itemAPrioriDelta = itemAPrioriDelta;
        this.itemDefinition = itemDefinition;
        this.itemProvider = itemProvider;
        this.postProcessor = postProcessor;
        this.variableProducer = variableProducer;
        this.processingMode = processingMode;
        argCheck(processingMode != ProcessingMode.NONE, "Processing mode cannot be NONE");
        this.beans = context.beans;
    }

    /**
     * Creates the respective mapping(s).
     */
    void createMappings(@NotNull PathKeyedMap<List<InboundMappingInContext<?, ?>>> mappingsMap)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {

        boolean fromAbsoluteState =
                processingMode == ProcessingMode.ABSOLUTE_STATE
                        || processingMode == ProcessingMode.ABSOLUTE_STATE_IF_KNOWN;

        if (fromAbsoluteState && !source.isAbsoluteStateAvailable()) {
            LOGGER.trace("Skipping inbound mapping(s) for {} as they should be processed from absolute state, but we don't"
                    + " have one", itemDescription);
            return;
        }

        Item<V, D> currentProjectionItem = itemProvider.provide();

        if (postProcessor != null) {
            postProcessor.postProcess(itemAPrioriDelta, currentProjectionItem);
        }

        LOGGER.trace("Creating {} inbound mapping(s) for {} in {} ({}). Relevant values are:\n"
                        + "- a priori item delta:\n{}\n"
                        + "- current item:\n{}",
                mappingBeans.size(),
                itemDescription,
                source.getProjectionHumanReadableNameLazy(),
                fromAbsoluteState ? "absolute mode" : "relative mode",
                DebugUtil.debugDumpLazily(itemAPrioriDelta, 1),
                DebugUtil.debugDumpLazily(currentProjectionItem, 1));

        if (currentProjectionItem != null && currentProjectionItem.hasRaw()) {
            throw new SystemException("Property " + currentProjectionItem + " has raw parsing state,"
                    + " such property cannot be used in inbound expressions");
        }

        source.setValueMetadata(currentProjectionItem, itemAPrioriDelta);

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

        for (MappingType mappingBean : mappingBeans) {

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

            ItemPath declaredTargetPath =
                    stripFocusVariable(
                            getPath(mappingBean.getTarget()),
                            contextDescription);
            if (ItemPath.isEmpty(declaredTargetPath)) {
                throw new ConfigurationException("Empty target path in " + contextDescription);
            }

            ItemPath targetPathOverride = source.determineTargetPathOverride(declaredTargetPath);
            LOGGER.trace("Target path override: {}", targetPathOverride);

            var origin = ConfigurationItemOrigin.inResourceOrAncestor(resource);

            MappingBuilder<V, D> builder = beans.mappingFactory.<V, D>createMappingBuilder()
                    .mappingBean(mappingBean, origin) // [EP:M:IM] DONE (mapping bean is from the resource, see callers)
                    .mappingKind(MappingKindType.INBOUND)
                    .implicitSourcePath(implicitSourcePath)
                    .targetPathOverride(targetPathOverride)
                    .contextDescription(contextDescription)
                    .defaultSource(defaultSource)
                    .targetContext(target.focusDefinition)
                    .addVariableDefinition(VAR_USER, target.focus, target.focusDefinition)
                    .addVariableDefinition(ExpressionConstants.VAR_FOCUS, target.focus, target.focusDefinition)
                    .addAliasRegistration(VAR_USER, ExpressionConstants.VAR_FOCUS)
                    .addVariableDefinition(ExpressionConstants.VAR_ACCOUNT, shadowVariableValue, shadowVariableDef)
                    .addVariableDefinition(ExpressionConstants.VAR_SHADOW, shadowVariableValue, shadowVariableDef)
                    .addVariableDefinition(ExpressionConstants.VAR_PROJECTION, shadowVariableValue, shadowVariableDef)
                    .addAliasRegistration(ExpressionConstants.VAR_ACCOUNT, ExpressionConstants.VAR_PROJECTION)
                    .addAliasRegistration(ExpressionConstants.VAR_SHADOW, ExpressionConstants.VAR_PROJECTION)
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
                assert target.focus != null;
                TypedValue<PrismObject<F>> targetContext = new TypedValue<>(target.focus);
                builder.originalTargetValues(
                        ExpressionUtil.computeTargetValues(
                                targetPathOverride != null ? targetPathOverride : declaredTargetPath,
                                targetContext,
                                builder.getVariables(),
                                beans.mappingFactory.getObjectResolver(),
                                "resolving target values",
                                context.env.task,
                                context.result));
            }

            MappingImpl<V, D> mapping = builder.build();

            ItemPath realTargetPath = mapping.getOutputPath();

            // We check the weak mapping skipping using the declared path, not the overridden path pointing to identities data.
            if (checkWeakSkip(mapping, declaredTargetPath)) {
                LOGGER.trace("Skipping because of mapping is weak and focus property has already a value");
                continue;
            }

            rememberItemDefinition(mapping, declaredTargetPath, targetPathOverride);

            mappingsMap
                    .computeIfAbsent(realTargetPath, k -> new ArrayList<>())
                    .add(source.createInboundMappingInContext(mapping));
        }
    }

    private ItemPath stripFocusVariable(ItemPath targetPath, String contextDescription) {
        if (targetPath == null) {
            return null;
        }
        QName variable = targetPath.firstToVariableNameOrNull();
        if (variable == null) {
            return targetPath;
        }
        if (VAR_USER.equals(variable.getLocalPart())
                || VAR_FOCUS.equals(variable.getLocalPart())) {
            return targetPath.rest();
        } else {
            throw new IllegalStateException(
                    String.format("Unsupported variable in target path '%s' in %s. Only $focus and $user are allowed here.",
                            targetPath, contextDescription));
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
        if (target.focus != null) {
            Item<?, ?> item = target.focus.findItem(declaredTargetPath);
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
            MutableItemDefinition<?> clone = outputDefinition.clone().toMutable();
            clone.setDynamic(true); // To serialize xsi:type along with the values.
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
