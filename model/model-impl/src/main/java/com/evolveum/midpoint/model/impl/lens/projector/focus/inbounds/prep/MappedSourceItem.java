/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep;

import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.InboundSourceData;
import com.evolveum.midpoint.model.common.mapping.MappingBuilder;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.ModelBeans;
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
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;

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
     * [EP:M:IM] DONE These mappings must come from {@link InboundsSource#resource} in {@link #inboundsSource}.
     * Currently it seems so.
     */
    @NotNull private final Collection<? extends AbstractMappingConfigItem<?>> mappingsCIs;

    /** Path of the source item, like `attributes/ri:firstName`. */
    @NotNull private final ItemPath itemPath;

    /**
     * Item delta that represents the change of the item from the state at the beginning of the clockwork execution
     * to the current state.
     *
     * @see InboundSourceData#getEffectiveItemDelta(ItemPath)
     */
    @Nullable private final ItemDelta<V, D> effectiveItemDelta;

    /** The (most current) source item definition. */
    @NotNull private final D itemDefinition;

    /** Does the situation require that the (fresh or cached) value for this item be known? */
    private final boolean requiringCurrentValue;

    @NotNull private final ModelBeans beans = ModelBeans.get();

    MappedSourceItem(
            @NotNull InboundsSource inboundsSource,
            @NotNull InboundsTarget<T> inboundsTarget,
            @NotNull InboundsContext inboundsContext,
            @NotNull Collection<? extends AbstractMappingConfigItem<?>> mappingsCIs,
            @NotNull ItemPath itemPath,
            @NotNull D itemDefinition) throws SchemaException, ConfigurationException {
        this.inboundsSource = inboundsSource;
        this.inboundsTarget = inboundsTarget;
        this.inboundsContext = inboundsContext;
        this.mappingsCIs = mappingsCIs;
        this.itemPath = itemPath;
        this.effectiveItemDelta = inboundsSource.sourceData.getEffectiveItemDelta(itemPath);
        this.itemDefinition = itemDefinition;
        this.requiringCurrentValue = computeRequiringCurrentValue();
    }

    boolean isRequiringCurrentValue() {
        return requiringCurrentValue;
    }

    private boolean computeRequiringCurrentValue() throws SchemaException, ConfigurationException {
        if (effectiveItemDelta != null) {
            // This is the legacy (pre-4.9) behavior.
            // TODO is it still valid? Maybe we should try to get the value even if we have a-priori delta?
            LOGGER.trace("{}: Delta existence for it indicates that we do not need to know the current value", itemPath);
            return false;
        }
        for (var mappingsCI : mappingsCIs) {
            if (mappingsCI.isStrong()) {
                LOGGER.trace("{}: Strong inbound mapping {} for it indicates that we need to know its current value"
                        + " (fresh or cached, depending on other options)", itemPath, mappingsCI.getName());
                return true;
            }
        }
        if (inboundsSource.hasDependentContext()) {
            // TODO reconsider this ugly hack
            LOGGER.trace("{}: There is a depending context (not necessarily for this item, though), we need to know"
                    + " the current value of it", itemPath);
            return true;
        }
        return false;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean isLoaded() throws SchemaException, ConfigurationException {
        return inboundsSource.isItemLoaded(itemPath);
    }

    /**
     * Creates the respective mapping(s) and puts them into `evaluationRequestsBeingCollected` parameter.
     */
    void createMappings(@NotNull MappingEvaluationRequestsMap evaluationRequestsBeingCollected, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {

        if (!isLoaded()) {
            if (effectiveItemDelta != null) {
                LOGGER.trace(
                        "{}: Item is not loaded; but proceeding with its inbound mapping(s) because of the delta",
                        itemPath);
            } else {
                if (inboundsSource.getCachedShadowsUse() == CachedShadowsUseType.USE_CACHED_OR_FAIL) {
                    throw new ExpressionEvaluationException(
                            "Inbound mapping(s) for %s could not be evaluated, because the item is not loaded".formatted(
                                    itemPath));
                } else {
                    // The loading might not be requested, or it could simply fail
                    LOGGER.trace("{}: Item is not loaded; its inbound mapping(s) evaluation will be skipped", itemPath);
                    return;
                }
            }
        }

        Item<V, D> itemOld = inboundsSource.sourceData.getItemOld(itemPath);

        // TODO reconsider if this is still needed
        if (isAssociation()) {
            //noinspection unchecked
            inboundsSource.resolveInputEntitlements(
                    (ContainerDelta<ShadowAssociationValueType>) effectiveItemDelta,
                    (ShadowAssociation) itemOld,
                    result);
        }

        LOGGER.trace("""
                        Creating {} inbound mapping(s) for {} in {}. Relevant values are:
                        - item delta:
                        {}
                        - item old:
                        {}""",
                mappingsCIs.size(),
                itemPath,
                inboundsSource.getProjectionHumanReadableName(),
                DebugUtil.debugDumpLazily(effectiveItemDelta, 1),
                DebugUtil.debugDumpLazily(itemOld, 1));

        if (itemOld != null && itemOld.hasRaw()) {
            throw new SystemException("Item " + itemOld + " has raw parsing state,"
                    + " such property cannot be used in inbound expressions");
        }

        // We cannot set the value metadata at this place, as all the structures are frozen now.
        // By disabling this code the majority of the processing introduced in midPrivacy Phase 1 will be disabled.
        // (As there won't be any metadata flowing from the resources.)
        // Hence, the TestValueMetadata will be disabled as well. See MID-10514.
        //inboundsSource.setValueMetadata(itemOld, effectiveItemDelta, result);

        ResourceType resource = inboundsSource.getResource();

        // Value for the $shadow ($projection, $account) variable.
        // Bear in mind that the value might not contain the full shadow (for example)
        PrismObject<ShadowType> shadowVariableValue = inboundsSource.sourceData.getShadowVariableValue();
        PrismObjectDefinition<ShadowType> shadowVariableDef = getShadowDefinition(shadowVariableValue);

        Source<V, D> defaultSource = new Source<>(
                itemOld,
                effectiveItemDelta,
                null,
                ExpressionConstants.VAR_INPUT_QNAME,
                itemDefinition);

        defaultSource.recompute();

        for (AbstractMappingConfigItem<?> mappingCI : mappingsCIs) {

            AbstractMappingType mappingBean = mappingCI.value();

            String channel = inboundsSource.getChannel();
            if (!MappingImpl.isApplicableToChannel(mappingBean, channel)) {
                LOGGER.trace("Mapping '{}' is not applicable to channel {}", mappingCI.getName(), channel);
                continue;
            }
            if (!inboundsContext.env.task.canSee(mappingBean)) {
                LOGGER.trace("Mapping '{}' is not applicable to the task execution mode", mappingCI.getName());
                continue;
            }

            String contextDescription = "inbound expression for " + itemPath + " in " + resource;

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
                    .addVariableDefinition(ExpressionConstants.VAR_OBJECT, getReferencedShadow(itemOld), shadowVariableDef)
                    .addVariableDefinition(ExpressionConstants.VAR_ASSOCIATION, inboundsSource.sourceData.getAssociationVariableValue(), ShadowAssociationValueType.class)
                    .addVariableDefinition(ExpressionConstants.VAR_RESOURCE, resource, resource.asPrismObject().getDefinition())
                    .addVariableDefinition(ExpressionConstants.VAR_CONFIGURATION,
                            inboundsContext.getSystemConfiguration(), getSystemConfigurationDefinition())
                    .addVariableDefinition(ExpressionConstants.VAR_OPERATION, inboundsContext.getOperation(), String.class)
                    .variableProducer(isAssociation() ? inboundsSource::getEntitlementVariableProducer : null)
                    .valuePolicySupplier(inboundsContext.createValuePolicySupplier())
                    .originType(OriginType.INBOUND)
                    .originObject(resource)
                    .mappingSpecification(inboundsSource.createMappingSpec(mappingCI.getName(), itemDefinition))
                    .now(inboundsContext.env.now);

            if (isComplexAttribute(defaultSource)) {
                // "<asIs>" won't work for complex attributes -> the reasonable default is "<complexAttributeSynchronization>"
                builder.defaultExpressionSupplier(
                        () -> new ExpressionType()
                                .expressionEvaluator(
                                        new ObjectFactory().createComplexAttributeSynchronization(
                                                new ComplexAttributeSynchronizationExpressionEvaluatorType())));
            }

            if (!inboundsTarget.isFocusBeingDeleted()) {
                builder.originalTargetValues(
                        ExpressionUtil.computeTargetValues(
                                inboundsSource.determineTargetPathExecutionOverride(targetFullPath) != null ?
                                        inboundsSource.determineTargetPathExecutionOverride(targetFullPath) : targetFullPath,
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
                LOGGER.trace(
                        "Skipping mapping '{}' because it is weak and focus property has already a value",
                        mappingCI.getName());
                continue;
            }

            rememberItemDefinition(mapping, targetFullPath, inboundsSource.determineTargetPathExecutionOverride(targetFullPath));

            ItemPath realTargetPath = mapping.getOutputPath();
            evaluationRequestsBeingCollected.add(realTargetPath, inboundsSource.createMappingRequest(mapping));
        }
    }

    /**
     * HACK. After complex attributes are distinguished from genuine reference attributes and complex associations
     * right at the level of definition, we will remove this hack.
     */
    private boolean isComplexAttribute(Source<V, D> defaultSource) {
        if (!(itemDefinition instanceof ShadowReferenceAttributeDefinition)) {
            return false;
        }
        var item = defaultSource.getAnyItem();
        var itemValue = item != null ? item.getAnyValue() : null;
        if (!(itemValue instanceof ShadowReferenceAttributeValue refAttrValue)) {
            return false;
        }
        var shadow = refAttrValue.getShadowIfPresent();
        if (shadow == null) {
            return false;
        }
        var identification = shadow.getTypeIdentification();
        // This may be the case for association definition. But in that case, the item definition would be
        // ShadowAssociationDefinition, not ShadowReferenceAttributeDefinition.
        return identification != null && identification.getKind() == ShadowKindType.ASSOCIATION;
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

    @Override
    public String toString() {
        return "MappedSourceItem{" +
                "itemPath=" + itemPath +
                ", mappings: " + mappingsCIs.size() +
                '}';
    }
}
