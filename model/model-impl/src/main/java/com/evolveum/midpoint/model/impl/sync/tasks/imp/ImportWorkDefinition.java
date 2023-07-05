/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.imp;

import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ResourceObjectSetSpecificationProvider;
import com.evolveum.midpoint.schema.util.task.work.ResourceObjectSetUtil;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionSource;
import com.evolveum.midpoint.schema.util.task.work.WorkDefinitionWrapper.TypedWorkDefinitionWrapper;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ImportWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectSetType;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectSetQueryApplicationModeType.REPLACE;

public class ImportWorkDefinition extends AbstractWorkDefinition implements ResourceObjectSetSpecificationProvider {

    /** Mutable, disconnected from the source. */
    @NotNull private final ResourceObjectSetType resourceObjects;

    ImportWorkDefinition(WorkDefinitionSource source) {
        ImportWorkDefinitionType typedDefinition = (ImportWorkDefinitionType)
                ((TypedWorkDefinitionWrapper) source).getTypedDefinition();
        resourceObjects = ResourceObjectSetUtil.fromConfiguration(typedDefinition.getResourceObjects());
        ResourceObjectSetUtil.setDefaultQueryApplicationMode(resourceObjects, REPLACE);
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
