/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asPrismObject;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.identities.IdentityItemConfiguration;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.InboundMappingInContext;
import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.StopProcessingProjectionException;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.processor.PropertyLimitations;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * The resource object being processed plus the necessary surroundings,
 * like lens/projection context in the case of clockwork processing.
 *
 * There are a lot of abstract methods here, dealing with e.g. determining if the full shadow is (or has to be) loaded,
 * methods for fetching the entitlements, and so on.
 *
 * Note that the name means "mapping source" and it's there to distinguish from {@link Source} (to avoid ugly qualified names).
 * TODO come with something more sensible
 */
abstract class MSource implements DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(MSource.class);

    /**
     * Current shadow object (in case of clockwork processing it may be full or repo-only, or maybe even null
     * - e.g. when currentObject is null in projection context).
     */
    @Nullable PrismObject<ShadowType> currentShadow;

    /**
     * A priori delta is a delta that was executed in a previous "step".
     * That means it is either delta from a previous wave or a sync delta (in wave 0).
     */
    @Nullable final ObjectDelta<ShadowType> aPrioriDelta;

    /**
     * Definition of resource object (object type or object class). Must not be null if the mappings are to be processed.
     * See {@link #checkResourceObjectDefinitionPresent()}.
     */
    final ResourceObjectDefinition resourceObjectDefinition;

    MSource(
            @Nullable ShadowType currentShadow,
            @Nullable ObjectDelta<ShadowType> aPrioriDelta,
            ResourceObjectDefinition resourceObjectDefinition) {
        this.currentShadow = asPrismObject(currentShadow);
        this.aPrioriDelta = aPrioriDelta;
        this.resourceObjectDefinition = resourceObjectDefinition;
    }

    /**
     * Checks if we should process mappings from this source. This is mainly to avoid the cost of loading
     * from resource if there's no explicit reason to do so. See the implementation for details.
     */
    abstract boolean isEligibleForInboundProcessing() throws SchemaException, ConfigurationException;

    /**
     * Resource object definition is absolutely necessary for mappings to be processed.
     * We might consider even checking this at the very beginning (even before checking if we can process mappings),
     * but let's be a bit forgiving.
     */
    void checkResourceObjectDefinitionPresent() {
        if (resourceObjectDefinition == null) {
            // Logging things here to log the context dump (it's not in the exception)
            LOGGER.error("Definition for projection {} not found in the context, but it " +
                    "should be there, dumping context:\n{}", getProjectionHumanReadableName(), getContextDump());
            throw new IllegalStateException("Definition for projection " + getProjectionHumanReadableName()
                    + " not found in the context, but it should be there");
        }
    }

    /**
     * Returns the resource object.
     */
    @NotNull abstract ResourceType getResource();

    /**
     * Dumps the current processing context, e.g. lens context.
     */
    abstract Object getContextDump();

    /** Returns human-readable name of the context, for logging/reporting purposes. */
    abstract String getProjectionHumanReadableName();

    /** Returns human-readable name of the context, for logging/reporting purposes. */
    Object getProjectionHumanReadableNameLazy() {
        return DebugUtil.lazy(this::getProjectionHumanReadableName);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "projection on", getProjectionHumanReadableName(), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "current shadow", currentShadow, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "a priori delta", aPrioriDelta, indent + 1);
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

    /**
     * Do we have the absolute state (i.e. full shadow) available?
     */
    abstract boolean isAbsoluteStateAvailable();

    /**
     * Adds value metadata to values in the current item and in a-priori delta.
     * Currently relevant only for clockwork processing.
     */
    abstract <V extends PrismValue, D extends ItemDefinition<?>> void setValueMetadata(
            Item<V, D> currentProjectionItem, ItemDelta<V, D> itemAPrioriDelta)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException;

    /** TODO clarify */
    abstract PrismObject<ShadowType> getResourceObjectNew();

    abstract String getChannel();

    /**
     * Determines processing mode: delta, full state, or that the mapping(s) should be ignored.
     *
     * There are complex rules written to eliminate needless shadow fetch operations that are applied
     * in the clockwork execution mode.
     *
     * TODO maybe we should rename this method to something like "is shadow loading requested"?
     */
    abstract @NotNull ProcessingMode getItemProcessingMode(
            String itemDescription, ItemDelta<?, ?> itemAPrioriDelta,
            List<? extends MappingType> mappingBeans,
            boolean ignored,
            PropertyLimitations limitations) throws SchemaException, ConfigurationException;

    /**
     * Returns true if the mapping(s) for given item on this source should be skipped.
     */
    boolean shouldBeMappingSkipped(String itemDescription, boolean ignored, PropertyLimitations limitations) {
        if (ignored) {
            LOGGER.trace("Mapping(s) for {} will be skipped because the item is ignored", itemDescription);
            return true;
        }
        if (limitations != null && !limitations.canRead()) {
            LOGGER.warn("Skipping inbound mapping(s) for {} in {} because it is not readable",
                    itemDescription, getProjectionHumanReadableName());
            return true;
        }
        return false;
    }

    /**
     * Loads the full shadow, if appropriate conditions are fulfilled. See the implementation for details.
     *
     * Currently relevant only for clockwork-based execution.
     */
    abstract void loadFullShadowIfNeeded(boolean fullStateRequired, @NotNull Context context)
            throws SchemaException, StopProcessingProjectionException;

    /**
     * Resolves the entitlements in the input data (a priori delta, current object).
     * Used in request creator, called from mappings creator.
     */
    abstract void resolveInputEntitlements(
            ItemDelta<PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>> associationAPrioriDelta,
            Item<PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>> currentAssociation);

    /**
     * Provides the `entitlement` variable for mapping evaluation.
     * Used in request creator, called from mapping evaluator (!).
     */
    abstract void getEntitlementVariableProducer(
            @NotNull Source<?, ?> source, @Nullable PrismValue value, @NotNull VariablesMap variables);

    /**
     * Creates {@link InboundMappingInContext} object by providing the appropriate context to the mapping.
     */
    abstract <V extends PrismValue, D extends ItemDefinition<?>> InboundMappingInContext<V,D> createInboundMappingInContext(
            MappingImpl<V, D> mapping);

    /**
     * Selects mappings appropriate for the current evaluation phase.
     * (The method is in this class, because here we have {@link #resourceObjectDefinition} where defaults are defined.)
     *
     * @param resourceItemLocalCorrelatorDefined Is the correlator defined "locally" for particular resource object item
     * (currently attribute)?
     * @param correlationItemPaths What (focus) items are referenced by `items` correlators?
     */
    @NotNull List<InboundMappingType> selectMappingBeansForEvaluationPhase(
            @NotNull List<InboundMappingType> beans,
            boolean resourceItemLocalCorrelatorDefined,
            @NotNull Collection<ItemPath> correlationItemPaths) {
        InboundMappingEvaluationPhaseType currentPhase = getCurrentEvaluationPhase();
        List<InboundMappingType> filtered =
                new ApplicabilityEvaluator(
                        getDefaultEvaluationPhases(), resourceItemLocalCorrelatorDefined, correlationItemPaths, currentPhase)
                        .filterApplicableMappingBeans(beans);
        if (filtered.size() < beans.size()) {
            LOGGER.trace("{} out of {} mapping(s) for this item were filtered out because of evaluation phase '{}'",
                    beans.size() - filtered.size(), beans.size(), currentPhase);
        }
        return filtered;
    }

    private @Nullable DefaultInboundMappingEvaluationPhasesType getDefaultEvaluationPhases() {
        return resourceObjectDefinition.getDefaultInboundMappingEvaluationPhases();
    }

    abstract @NotNull InboundMappingEvaluationPhaseType getCurrentEvaluationPhase();

    /** Computes focus identity source information for given projection. Not applicable to pre-mappings. */
    abstract @Nullable FocusIdentitySourceType getFocusIdentitySource();

    abstract @Nullable IdentityItemConfiguration getIdentityItemConfiguration(@NotNull ItemPath itemPath) throws ConfigurationException;

    abstract ItemPath determineTargetPathOverride(ItemPath targetItemPath) throws ConfigurationException, SchemaException;
}
