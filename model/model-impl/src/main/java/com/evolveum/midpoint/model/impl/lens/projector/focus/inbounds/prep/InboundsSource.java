/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.InboundSourceData;
import com.evolveum.midpoint.model.api.identities.IdentityItemConfiguration;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.MappingEvaluationRequest;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.StopProcessingProjectionException;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.schema.config.InboundMappingConfigItem;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Source for the whole inbounds processing.
 *
 * Contains the resource object being processed plus the necessary surroundings,
 * like lens/projection context in the case of clockwork processing.
 *
 * There are a lot of abstract methods here, dealing with e.g. determining if the full shadow is (or has to be) loaded,
 * methods for fetching the entitlements, and so on.
 */
public abstract class InboundsSource implements DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(InboundsSource.class);

    /**
     * Current shadow object (in case of clockwork processing it may be full or repo-only, or maybe even null
     * - e.g. when currentObject is null in projection context).
     */
    @NotNull InboundSourceData sourceData;

    /**
     * A priori delta is a delta that was executed in a previous "step".
     * That means it is either delta from a previous wave or a sync delta (in wave 0).
     * Taken from {@link #sourceData} just for clarity.
     */
    @Nullable final ObjectDelta<ShadowType> aPrioriDelta;

    @NotNull private final ResourceType resource;

    /** Background information for value provenance metadata for related inbound mappings. */
    @NotNull private final InboundMappingContextSpecification mappingContextSpecification;

    @NotNull final String humanReadableName;

    @NotNull final ResourceObjectInboundDefinition inboundDefinition;

    InboundsSource(
            @NotNull InboundSourceData sourceData,
            @NotNull ResourceObjectInboundDefinition inboundDefinition,
            @NotNull ResourceType resource,
            @NotNull InboundMappingContextSpecification mappingContextSpecification,
            @NotNull String humanReadableName) {
        this.sourceData = sourceData;
        this.aPrioriDelta = sourceData.getAPrioriDelta();
        this.inboundDefinition = inboundDefinition;
        this.resource = resource;
        this.mappingContextSpecification = mappingContextSpecification;
        this.humanReadableName = humanReadableName;
    }

    /**
     * Checks if we should process mappings from this source. This is mainly to avoid the cost of loading
     * from resource if there's no explicit reason to do so. See the implementation for details.
     */
    abstract boolean isEligibleForInboundProcessing(OperationResult result) throws SchemaException, ConfigurationException;

    /**
     * Returns the resource object.
     */
    @NotNull ResourceType getResource() {
        return resource;
    }

    /** Returns human-readable name of the context, for logging/reporting purposes. */
    @NotNull String getProjectionHumanReadableName() {
        return humanReadableName;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "projection on", humanReadableName, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "source data", sourceData, indent + 1);
        return sb.toString();
    }

    /**
     * True if we are running under clockwork.
     */
    abstract boolean isClockwork();

    /**
     * Is the current projection being deleted? I.e. it (may or may not) exist now, but is not supposed to exist
     * after the clockwork is finished.
     *
     * Currently relevant only for clockwork-based execution. But (maybe soon) we'll implement it also for
     * pre-mappings.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    abstract boolean isProjectionBeingDeleted();

    public abstract boolean isAttributeAvailable(ItemName itemName) throws SchemaException, ConfigurationException;
    public abstract boolean isAssociationAvailable(ItemName itemName) throws SchemaException, ConfigurationException;
    public abstract boolean isFullShadowAvailable();
    public abstract boolean isShadowGone();
    public abstract boolean isAuxiliaryObjectClassPropertyLoaded() throws SchemaException, ConfigurationException;

    /**
     * Adds value metadata to values in the current item and in a-priori delta.
     * Currently relevant only for clockwork processing.
     */
    abstract <V extends PrismValue, D extends ItemDefinition<?>> void setValueMetadata(
            Item<V, D> currentProjectionItem, ItemDelta<V, D> itemAPrioriDelta, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException;

    // TODO move to context
    abstract String getChannel();

    /**
     * Returns true if the mapping(s) for given item on this source should be skipped because of item restrictions
     * or obviously missing data.
     */
    boolean isItemNotProcessable(
            ItemPath itemPath, boolean executionModeVisible, boolean ignored, PropertyLimitations limitations) {
        if (!executionModeVisible) {
            LOGGER.trace("Inbound mapping(s) for {} will be skipped because the item is not visible in current execution mode",
                    itemPath);
            return true;
        }
        if (ignored) {
            LOGGER.trace("Inbound mapping(s) for {} will be skipped because the item is ignored", itemPath);
            return true;
        }
        if (limitations != null && !limitations.canRead()) {
            LOGGER.warn("Skipping inbound mapping(s) for {} in {} because it is not readable",
                    itemPath, getProjectionHumanReadableName());
            return true;
        }
        if (sourceData.isEmpty() && sourceData.getItemAPrioriDelta(itemPath) == null) {
            LOGGER.trace("Inbound mapping(s) for {} will be skipped because there is no shadow (not even repo version),"
                    + "and no a priori delta for the item", itemPath);
            return true;
        }
        return false;
    }

    /**
     * Loads the full shadow, if appropriate conditions are fulfilled. See the implementation for details.
     *
     * Currently relevant only for clockwork-based execution.
     */
    abstract void loadFullShadow(@NotNull InboundsContext context, OperationResult result)
            throws SchemaException, StopProcessingProjectionException;

    /**
     * Resolves the entitlements in the input data (a priori delta, current object).
     * Used in request creator, called from mappings creator.
     */
    abstract void resolveInputEntitlements(
            ContainerDelta<ShadowAssociationValueType> associationAPrioriDelta,
            ShadowAssociation currentAssociation);

    /**
     * Provides the `entitlement` variable for mapping evaluation.
     * Used in request creator, called from mapping evaluator (!).
     */
    abstract void getEntitlementVariableProducer(
            @NotNull Source<?, ?> source, @Nullable PrismValue value, @NotNull VariablesMap variables);

    /**
     * Creates {@link MappingEvaluationRequest} object by providing the appropriate context to the mapping.
     */
    abstract <V extends PrismValue, D extends ItemDefinition<?>> MappingEvaluationRequest<V, D>
    createMappingRequest(MappingImpl<V, D> mapping);

    /**
     * Selects mappings appropriate for the current evaluation phase.
     * (The method is in this class, because here we have the object definition where defaults are defined.)
     *
     * @param resourceItemLocalCorrelatorDefined Is the correlator defined "locally" for particular resource object item
     * (currently attribute)?
     * @param correlationItemPaths What (focus) items are referenced by `items` correlators?
     */
    @NotNull List<InboundMappingConfigItem> selectMappingBeansForEvaluationPhase(
            @NotNull ItemPath itemPath,
            @NotNull List<InboundMappingConfigItem> mappings,
            boolean resourceItemLocalCorrelatorDefined,
            @NotNull Collection<ItemPath> correlationItemPaths) throws ConfigurationException {
        var currentPhase = getCurrentEvaluationPhase();
        var applicabilityEvaluator = new ApplicabilityEvaluator(
                getDefaultEvaluationPhases(), resourceItemLocalCorrelatorDefined, correlationItemPaths, currentPhase);
        var filteredMappings = applicabilityEvaluator.filterApplicableMappings(mappings);
        if (filteredMappings.size() < mappings.size()) {
            LOGGER.trace("{}: {} out of {} mapping(s) were filtered out because of evaluation phase '{}'",
                    itemPath, mappings.size() - filteredMappings.size(), mappings.size(), currentPhase);
        }
        return filteredMappings;
    }

    private @Nullable DefaultInboundMappingEvaluationPhasesType getDefaultEvaluationPhases() {
        return inboundDefinition.getDefaultInboundMappingEvaluationPhases();
    }

    abstract @NotNull InboundMappingEvaluationPhaseType getCurrentEvaluationPhase();

    /** Computes focus identity source information for given projection. Not applicable to pre-mappings. */
    abstract @Nullable FocusIdentitySourceType getFocusIdentitySource();

    abstract @Nullable IdentityItemConfiguration getIdentityItemConfiguration(@NotNull ItemPath itemPath) throws ConfigurationException;

    abstract ItemPath determineTargetPathExecutionOverride(ItemPath targetItemPath) throws ConfigurationException, SchemaException;

    public @NotNull Collection<? extends ShadowSimpleAttributeDefinition<?>> getSimpleAttributeDefinitions() {
        return sourceData.getSimpleAttributeDefinitions();
    }

    @NotNull Collection<? extends ShadowReferenceAttributeDefinition> getObjectReferenceAttributeDefinitions() {
        return sourceData.getReferenceAttributeDefinitions();
    }

    @NotNull Collection<? extends ShadowAssociationDefinition> getAssociationDefinitions() {
        return sourceData.getAssociationDefinitions();
    }

    public @NotNull ResourceObjectInboundDefinition getInboundDefinition() {
        return inboundDefinition;
    }

    public @Nullable ObjectDelta<ShadowType> getAPrioriDelta() {
        return aPrioriDelta;
    }

    public @NotNull InboundSourceData getSourceData() {
        return sourceData;
    }

    /** FIXME ugly hack */
    boolean hasDependentContext() throws SchemaException, ConfigurationException {
        return false;
    }

    /** Only for full processing. */
    @NotNull CachedShadowsUseType getCachedShadowsUse() throws SchemaException, ConfigurationException {
        throw new UnsupportedOperationException("Not implemented for " + this);
    }

    public MappingSpecificationType createMappingSpec(@Nullable String mappingName, @NotNull ItemDefinition<?> sourceDefinition) {
        return new MappingSpecificationType()
                .mappingName(mappingName)
                .definitionObjectRef(ObjectTypeUtil.createObjectRef(resource))
                .objectType(mappingContextSpecification.typeIdentificationBean())
                .associationType(
                        sourceDefinition instanceof ShadowAssociationDefinition assocDef ?
                                assocDef.getAssociationTypeName() :
                                mappingContextSpecification.associationTypeName())
                .tag(mappingContextSpecification.shadowTag());
    }
}
