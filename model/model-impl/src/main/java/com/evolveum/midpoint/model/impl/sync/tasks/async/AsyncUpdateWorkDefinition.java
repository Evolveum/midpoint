/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.async;

import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ResourceObjectSetSpecificationProvider;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory.WorkDefinitionInfo;
import com.evolveum.midpoint.schema.util.task.work.ResourceObjectSetUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AsyncUpdateWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectSetType;

import org.jetbrains.annotations.NotNull;

public class AsyncUpdateWorkDefinition extends AbstractWorkDefinition implements ResourceObjectSetSpecificationProvider {

    @NotNull private final ResourceObjectSetType resourceObjects;

    AsyncUpdateWorkDefinition(@NotNull WorkDefinitionInfo info) {
        super(info);
        var typedDefinition = (AsyncUpdateWorkDefinitionType) info.getBean();
        resourceObjects = ResourceObjectSetUtil.fromConfiguration(typedDefinition.getUpdatedResourceObjects());
        ResourceObjectSetUtil.removeQuery(resourceObjects);
    }

    @Override
    public @NotNull ResourceObjectSetType getResourceObjectSetSpecification() {
        return resourceObjects;
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabel(sb, "resourceObjects", resourceObjects, indent+1);
    }
}
