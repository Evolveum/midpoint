/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep;

import com.evolveum.midpoint.model.api.InboundSourceData;
import com.evolveum.midpoint.model.common.mapping.MappingBuilder;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.FullInboundsProcessing;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.MappingEvaluationRequestsMap;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.schema.config.AbstractMappingConfigItem;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.processor.ShadowAssociation;
import com.evolveum.midpoint.schema.processor.ShadowAssociationDefinition;
import com.evolveum.midpoint.schema.processor.ShadowAssociationValue;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttributeValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.Objects;

import static com.evolveum.midpoint.repo.common.expression.ExpressionUtil.getPath;
import static com.evolveum.midpoint.schema.constants.ExpressionConstants.*;

/**
 * *Source* item (attribute, association, activation property, and so on) for which mapping(s) have to be created.
 *
 * It exists mainly to allow gathering all such requests first, then looking if we need to load the resource object,
 * and then create all the mappings with the resource object loaded.
 */
class MappedSourceItem<V extends PrismValue, D extends ItemDefinition<?>, T extends Containerable> {

    private static final Trace LOGGER = TraceManager.getTrace(MappedSourceItem.class);

    @NotNull private final InboundsSource inboundsSource;
    @NotNull private final InboundsTarget<T> inboundsTarget;
    @NotNull private final InboundsContext inboundsContext;

    /**
     * Mappings (config items) that are to be evaluated for this source item.
     *
     * [EP:M:IM] DONE These mappings must come from `source.resource`. Currently it seems so.
     */
    @NotNull private final Collection<? extends AbstractMappingConfigItem<?>> mappingsCIs;

    /** Path of the source item, like `attributes/ri:firstName`. */
    @NotNull private final ItemPath itemPath;

    /** Human-readable description of the source item, like "attribute firstName". */
    @NotNull final String itemDescription;

    /**
     * A-priori delta for the source item, if present: sync delta or previously computed one.
     *
     * @see FullInboundsProcessing#getAPrioriDelta(LensProjectionContext)
     * @see InboundSourceData#getItemAPrioriDelta(ItemPath)
     */
    @Nullable private final ItemDelta<V, D> itemAPrioriDelta;

    /** The (most current) source item definition. */
    @NotNull private final D itemDefinition;

    /**
     * When called, provides the current (potentially null/empty) source item.
     * The item may be unavailable initially, hence the provider is needed.
     *
     * TODO Before 4.9, this was needed because of amalgamated associations. Now it could be probably simplified,
     *  retrieving the data from `sourceData` in {@link #inboundsSource}, using {@link #itemPath}.
     */
    @NotNull private final ItemProvider<V, D> itemProvider;

    /** Value of `true` means that we will load the shadow because of this item (if it's not already loaded). */
    private final boolean triggersFullShadowLoading;

    /** Value of `true` means that we will skip this item if the full shadow is not available (regardless of the reason). */
    private final boolean ignoreIfNoFullShadow;

    @NotNull private final ModelBeans beans = ModelBeans.get();

    MappedSourceItem(
            @NotNull InboundsSource inboundsSource,
            @NotNull InboundsTarget<T> inboundsTarget,
            @NotNull InboundsContext inboundsContext,
            @NotNull Collection<? extends AbstractMappingConfigItem<?>> mappingsCIs,
            @NotNull ItemPath itemPath,
            @NotNull String itemDescription,
            @Nullable ItemDelta<V, D> itemAPrioriDelta,
            @NotNull D itemDefinition,
            @NotNull ItemProvider<V, D> itemProvider,
            boolean triggersFullShadowLoading,
            boolean ignoreIfNoFullShadow) {
        this.inboundsSource = inboundsSource;
        this.inboundsTarget = inboundsTarget;
        this.inboundsContext = inboundsContext;
        this.mappingsCIs = mappingsCIs;
        this.itemPath = itemPath;
        this.itemDescription = itemDescription;
        this.itemAPrioriDelta = itemAPrioriDelta;
        this.itemDefinition = itemDefinition;
        this.itemProvider = itemProvider;
        this.triggersFullShadowLoading = triggersFullShadowLoading;
        this.ignoreIfNoFullShadow = ignoreIfNoFullShadow;
    }

    /**
     * Creates the respective mapping(s) and puts them into `evaluationRequestsBeingCollected` parameter.
     */
    void createMappings(@NotNull MappingEvaluationRequestsMap evaluationRequestsBeingCollected, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {

        if (ignoreIfNoFullShadow && !inboundsSource.isAbsoluteStateAvailable()) {
            LOGGER.trace("Skipping inbound mapping(s) for {} as we don't have the full shadow", itemDescription);
            return;
        }

        Item<V, D> currentProjectionItem = itemProvider.provide();

        // TODO reconsider if this is still needed
        if (isAssociation()) {
            //noinspection unchecked
            inboundsSource.resolveInputEntitlements(
                    (ContainerDelta<ShadowAssociationValueType>) itemAPrioriDelta,
                    (ShadowAssociation) currentProjectionItem);
        }

        LOGGER.trace("""
                        Creating {} inbound mapping(s) for {} in {} (ignore-if-no-full-shadow: {}). Relevant values are:
                        - a priori item delta:
                        {}
                        - current item:
                        {}""",
                mappingsCIs.size(),
                itemDescription,
                inboundsSource.getProjectionHumanReadableName(),
                ignoreIfNoFullShadow,
                DebugUtil.debugDumpLazily(itemAPrioriDelta, 1),
                DebugUtil.debugDumpLazily(currentProjectionItem, 1));

        if (currentProjectionItem != null && currentProjectionItem.hasRaw()) {
            throw new SystemException("Item " + currentProjectionItem + " has raw parsing state,"
                    + " such property cannot be used in inbound expressions");
        }

        inboundsSource.setValueMetadata(currentProjectionItem, itemAPrioriDelta, result);

        ResourceType resource = inboundsSource.getResource();

        // Value for the $shadow ($projection, $account) variable.
        // Bear in mind that the value might not contain the full shadow (for example)
        PrismObject<ShadowType> shadowVariableValue = inboundsSource.sourceData.getShadowIfPresent();
        PrismObjectDefinition<ShadowType> shadowVariableDef = getShadowDefinition(shadowVariableValue);

        Source<V, D> defaultSource = new Source<>(
                currentProjectionItem,
                itemAPrioriDelta,
                null,
                ExpressionConstants.VAR_INPUT_QNAME,
                itemDefinition);

        defaultSource.recompute();

        for (AbstractMappingConfigItem<?> mappingCI : mappingsCIs) {

            AbstractMappingType mappingBean = mappingCI.value();

            String channel = inboundsSource.getChannel();
            if (!MappingImpl.isApplicableToChannel(mappingBean, channel)) {
                LOGGER.trace("Mapping is not applicable to channel {}", channel);
                continue;
            }
            if (!inboundsContext.env.task.canSee(mappingBean)) {
                LOGGER.trace("Mapping is not applicable to the task execution mode");
                continue;
            }

            String contextDescription = "inbound expression for " + itemDescription + " in " + resource;

            ItemPath targetFullPath = getTargetFullPath(mappingBean, contextDescription); // without variable, with prefix

            //noinspection unchecked
            MappingBuilder<V, D> builder = beans.mappingFactory.<V, D>createMappingBuilder()
                    .mapping((AbstractMappingConfigItem<MappingType>) mappingCI) // [EP:M:IM] DONE (mapping bean is from the resource, see callers)
                    .mappingKind(MappingKindType.INBOUND)
                    .implicitSourcePath(itemPath)
                    .targetPathOverride(targetFullPath)
                    .targetPathExecutionOverride(inboundsSource.determineTargetPathExecutionOverride(targetFullPath))
                    .contextDescription(contextDescription)
                    .defaultSource(defaultSource)
                    .targetContextDefinition(inboundsTarget.targetDefinition)
                    .addVariableDefinition(VAR_USER, inboundsTarget.getTargetRealValue(), inboundsTarget.targetDefinition)
                    .addVariableDefinition(ExpressionConstants.VAR_FOCUS, inboundsTarget.getTargetRealValue(), inboundsTarget.targetDefinition)
                    .addAliasRegistration(VAR_USER, ExpressionConstants.VAR_FOCUS)
                    .addVariableDefinition(ExpressionConstants.VAR_ACCOUNT, shadowVariableValue, shadowVariableDef)
                    .addVariableDefinition(ExpressionConstants.VAR_SHADOW, shadowVariableValue, shadowVariableDef)
                    .addVariableDefinition(ExpressionConstants.VAR_PROJECTION, shadowVariableValue, shadowVariableDef)
                    .addAliasRegistration(ExpressionConstants.VAR_ACCOUNT, ExpressionConstants.VAR_PROJECTION)
                    .addAliasRegistration(ExpressionConstants.VAR_SHADOW, ExpressionConstants.VAR_PROJECTION)
                    .addVariableDefinition(ExpressionConstants.VAR_OBJECT, getReferencedShadow(currentProjectionItem), shadowVariableDef)
                    .addVariableDefinition(ExpressionConstants.VAR_ASSOCIATION, inboundsSource.sourceData.getAssociationValueBeanIfPresent(), ShadowAssociationValueType.class)
                    .addVariableDefinition(ExpressionConstants.VAR_RESOURCE, resource, resource.asPrismObject().getDefinition())
                    .addVariableDefinition(ExpressionConstants.VAR_CONFIGURATION,
                            inboundsContext.getSystemConfiguration(), getSystemConfigurationDefinition())
                    .addVariableDefinition(ExpressionConstants.VAR_OPERATION, inboundsContext.getOperation(), String.class)
                    .variableProducer(isAssociation() ? inboundsSource::getEntitlementVariableProducer : null)
                    .valuePolicySupplier(inboundsContext.createValuePolicySupplier())
                    .originType(OriginType.INBOUND)
                    .originObject(resource)
                    .now(inboundsContext.env.now);

            if (!inboundsTarget.isFocusBeingDeleted()) {
                builder.originalTargetValues(
                        ExpressionUtil.computeTargetValues(
                                inboundsSource.determineTargetPathExecutionOverride(targetFullPath) != null ? inboundsSource.determineTargetPathExecutionOverride(targetFullPath) : targetFullPath,
                                new TypedValue<>(inboundsTarget.getTargetRealValue(), inboundsTarget.targetDefinition),
                                builder.getVariables(),
                                beans.mappingFactory.getObjectResolver(),
                                "resolving target values",
                                inboundsContext.env.task,
                                result));
            }

            MappingImpl<V, D> mapping = builder.build();

            // We check the weak mapping skipping using the declared path, not the overridden path pointing to identities data.
            if (checkWeakSkip(mapping, targetFullPath)) {
                LOGGER.trace("Skipping because of mapping is weak and focus property has already a value");
                continue;
            }

            rememberItemDefinition(mapping, targetFullPath, inboundsSource.determineTargetPathExecutionOverride(targetFullPath));

            ItemPath realTargetPath = mapping.getOutputPath();
            evaluationRequestsBeingCollected.add(realTargetPath, inboundsSource.createMappingRequest(mapping));
        }
    }

    // FIXME brutal hack
    private PrismObject<ShadowType> getReferencedShadow(Item<V, D> currentProjectionItem) {
        if (currentProjectionItem == null || currentProjectionItem.size() != 1) {
            return null;
        }
        var value = currentProjectionItem.getValue();
        if (value instanceof ShadowReferenceAttributeValue refAttrValue) {
            return refAttrValue.getObject();
        } else if (value instanceof ShadowAssociationValue assocValue) {
            var objectRef = assocValue.getSingleObjectRefRelaxed();
            return objectRef != null ? objectRef.getObject() : null;
        } else {
            return null;
        }
    }

    private @NotNull ItemPath getTargetFullPath(AbstractMappingType mappingBean, String errorCtxDesc)
            throws ConfigurationException {

        ItemPath path = getPath(mappingBean.getTarget());
        if (path == null) {
            if (isAssociation()) {
                return AssignmentHolderType.F_ASSIGNMENT;
            } else {
                throw new ConfigurationException("No target path in " + errorCtxDesc);
            }
        }

        QName variable = path.firstToVariableNameOrNull();
        ItemPath pathAfterVariable = path.stripVariableSegment();
        if (ItemPath.isEmpty(pathAfterVariable)) {
            throw new ConfigurationException("Empty target path in " + errorCtxDesc + " (after stripping variable segment)");
        }

        String varLocalPart = variable != null ? variable.getLocalPart() : null;
        if (varLocalPart == null || VAR_TARGET.equals(varLocalPart)) {
            return inboundsTarget.getTargetPathPrefix().append(pathAfterVariable);
        } else if (VAR_USER.equals(varLocalPart) || VAR_FOCUS.equals(varLocalPart)) {
            return pathAfterVariable;
        } else {
            throw new IllegalStateException(String.format(
                    "Unsupported variable in target path '%s' in %s. Only $focus, $user, and $target are allowed here.",
                            path, errorCtxDesc));
        }
    }

    private boolean isAssociation() {
        return itemDefinition instanceof ShadowAssociationDefinition;
    }

    private PrismObjectDefinition<ShadowType> getShadowDefinition(PrismObject<ShadowType> shadowNew) {
        if (shadowNew != null && shadowNew.getDefinition() != null) {
            return shadowNew.getDefinition();
        } else {
            return beans.prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
        }
    }

    private @NotNull PrismObjectDefinition<SystemConfigurationType> getSystemConfigurationDefinition() {
        PrismObject<SystemConfigurationType> config = inboundsContext.getSystemConfiguration();
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
        if (inboundsTarget.targetPcv != null) {
            Item<?, ?> item = inboundsTarget.targetPcv.findItem(declaredTargetPath);
            return item != null && !item.isEmpty();
        } else {
            return false;
        }
    }

    boolean doesTriggerFullShadowLoading() {
        return triggersFullShadowLoading;
    }

    private void rememberItemDefinition(MappingImpl<V, D> mapping, ItemPath declaredTargetPath, ItemPath targetPathOverride)
            throws ConfigurationException {
        D outputDefinition =
                MiscUtil.configNonNull(
                        mapping.getOutputDefinition(),
                        () -> "No definition for target item " + declaredTargetPath + " in " + mapping.getContextDescription());
        inboundsTarget.addItemDefinition(declaredTargetPath, outputDefinition);
        if (targetPathOverride != null) {
            ItemDefinition<?> clone = outputDefinition.clone();
            clone.mutator().setDynamic(true); // To serialize xsi:type along with the values.
            inboundsTarget.addItemDefinition(targetPathOverride, clone);
        }
    }

    @FunctionalInterface
    interface ItemProvider<V extends PrismValue, D extends ItemDefinition<?>> {
        Item<V, D> provide() throws SchemaException;
    }
}
