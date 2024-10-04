/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds;

import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep.InboundMappingContextSpecification;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.processor.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.impl.correlation.CorrelationServiceImpl;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.sync.SynchronizationContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Minimalistic context needed to evaluate inbound mappings outside of both {@link LensContext}
 * and {@link SynchronizationContext}.
 *
 * It is used e.g. when a mappings evaluation is invoked as part of
 *
 * - {@link MidpointFunctions#findCandidateOwners(Class, ShadowType, String, ShadowKindType, String)},
 * - {@link CorrelationServiceImpl#checkCandidateOwner(ShadowType, ResourceType, SynchronizationPolicy, FocusType, Task,
 * OperationResult)},
 * - or association value synchronization.
 */
public class DefaultSingleShadowInboundsProcessingContextImpl<T extends Containerable>
        implements SingleShadowInboundsProcessingContext<T> {

    @NotNull private final ShadowLikeValue shadowLikeValue;

    @NotNull private final ResourceType resource;

    /** Background information for value provenance metadata for inbound mappings related to this shadow. */
    @NotNull private final InboundMappingContextSpecification mappingContextSpecification;

    @NotNull private final T preFocus;

    @Nullable private final SystemConfigurationType systemConfiguration;

    @NotNull private final Task task;

    @NotNull private final ResourceObjectDefinition objectDefinition;

    @NotNull private final ResourceObjectInboundDefinition inboundDefinition;

    private final boolean beforeCorrelation;

    public DefaultSingleShadowInboundsProcessingContextImpl(
            @NotNull ShadowLikeValue shadowLikeValue,
            @NotNull ResourceType resource,
            @NotNull InboundMappingContextSpecification mappingContextSpecification,
            @NotNull T preFocus,
            @Nullable SystemConfigurationType systemConfiguration,
            @NotNull Task task,
            @NotNull ResourceObjectDefinition objectDefinition,
            @NotNull ResourceObjectInboundDefinition inboundDefinition,
            boolean beforeCorrelation) {
        this.shadowLikeValue = shadowLikeValue;
        this.resource = resource;
        this.mappingContextSpecification = mappingContextSpecification;
        this.preFocus = preFocus;
        this.systemConfiguration = systemConfiguration;
        this.task = task;
        this.objectDefinition = objectDefinition;
        this.inboundDefinition = inboundDefinition;
        this.beforeCorrelation = beforeCorrelation;
    }

    @Override
    public @NotNull ShadowLikeValue getShadowLikeValue() {
        return shadowLikeValue;
    }

    public @NotNull T getPreFocus() {
        return preFocus;
    }

    @Override
    public @Nullable ObjectDelta<ShadowType> getResourceObjectDelta() {
        return null;
    }

    public @Nullable SystemConfigurationType getSystemConfiguration() {
        return systemConfiguration;
    }

    @Override
    public @NotNull Task getTask() {
        return task;
    }

    @Override
    public @NotNull ResourceType getResource() {
        return resource;
    }

    @Override
    public @NotNull InboundMappingContextSpecification getMappingContextSpecification() {
        return mappingContextSpecification;
    }

    @Override
    public @NotNull ResourceObjectDefinition getObjectDefinitionRequired() {
        return objectDefinition;
    }

    @Override
    public @NotNull ResourceObjectInboundDefinition getInboundDefinition() {
        return inboundDefinition;
    }

    @Override
    public @Nullable String getArchetypeOid() {
        return inboundDefinition.getFocusSpecification().getArchetypeOid();
    }

    @Override
    public String getChannel() {
        // This is an approximation. (Normally, the channel comes as part of the resource object change information.)
        return task.getChannel();
    }

    @Override
    public boolean isBeforeCorrelation() {
        return beforeCorrelation;
    }

    @Override
    public String toString() {
        return "SimplePreInboundsContext for " +
                shadowLikeValue +
                " on " + resource.getName() +
                " of " + objectDefinition.getTypeIdentification();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "shadowedResourceObject", shadowLikeValue, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "resource", resource, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "objectTypeDefinition", objectDefinition, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "systemConfiguration", systemConfiguration, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "preFocus", preFocus, indent + 1);
        return sb.toString();
    }
}
