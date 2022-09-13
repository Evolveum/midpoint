/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.correlation.CorrelationServiceImpl;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.sync.SynchronizationContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.SynchronizationPolicy;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Minimalistic context needed to evaluate inbound mappings outside of both {@link LensContext}
 * and {@link SynchronizationContext}.
 *
 * It is used e.g. when a correlation is invoked as part
 * of {@link MidpointFunctions#findCandidateOwners(Class, ShadowType, String, ShadowKindType, String)}
 * or {@link CorrelationServiceImpl#checkCandidateOwner(ShadowType, ResourceType, SynchronizationPolicy, FocusType, Task,
 * OperationResult)} method call.
 */
public class SimplePreInboundsContextImpl<F extends FocusType>
        implements PreInboundsContext<F> {

    @NotNull private final ShadowType shadowedResourceObject;

    @NotNull private final ResourceType resource;

    @NotNull private final F preFocus;

    @Nullable private final SystemConfigurationType systemConfiguration;

    @NotNull private final Task task;

    @NotNull private final ResourceObjectTypeDefinition objectTypeDefinition;

    @NotNull private final ModelBeans beans;

    public SimplePreInboundsContextImpl(
            @NotNull ShadowType shadowedResourceObject,
            @NotNull ResourceType resource,
            @NotNull F preFocus,
            @Nullable SystemConfigurationType systemConfiguration,
            @NotNull Task task,
            @NotNull ResourceObjectTypeDefinition objectTypeDefinition,
            @NotNull ModelBeans beans) {
        this.shadowedResourceObject = shadowedResourceObject;
        this.resource = resource;
        this.preFocus = preFocus;
        this.systemConfiguration = systemConfiguration;
        this.task = task;
        this.objectTypeDefinition = objectTypeDefinition;
        this.beans = beans;
    }

    @Override
    public @NotNull ShadowType getShadowedResourceObject() {
        return shadowedResourceObject;
    }

    public @NotNull F getPreFocus() {
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
    public @NotNull ModelBeans getBeans() {
        return beans;
    }

    @Override
    public @NotNull ResourceType getResource() {
        return resource;
    }

    @Override
    public @NotNull ResourceObjectDefinition getObjectDefinitionRequired() {
        return objectTypeDefinition;
    }

    @Override
    public @Nullable String getArchetypeOid() {
        return objectTypeDefinition.getArchetypeOid();
    }

    @Override
    public String getChannel() {
        // This is an approximation. (Normally, the channel comes as part of the resource object change information.)
        return task.getChannel();
    }

    @Override
    public String toString() {
        return "SimplePreInboundsContext for " +
                shadowedResourceObject +
                " on " + resource.getName() +
                " of " + objectTypeDefinition.getTypeIdentification();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "shadowedResourceObject", shadowedResourceObject, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "resource", resource, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "objectTypeDefinition", objectTypeDefinition, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "systemConfiguration", systemConfiguration, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "preFocus", preFocus, indent + 1);
        return sb.toString();
    }
}
