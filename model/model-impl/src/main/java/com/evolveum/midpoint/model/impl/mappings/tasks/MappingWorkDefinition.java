/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.model.impl.mappings.tasks;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectSetQueryApplicationModeType.*;

import java.util.List;

import com.evolveum.midpoint.model.impl.sync.tasks.ResourceSetTaskWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.schema.util.task.work.ResourceObjectSetUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Work definition for mapping simulation activity.
 */
public class MappingWorkDefinition extends ResourceSetTaskWorkDefinition {

    private final SimulatedMappingsType mappingsToUse;

    public MappingWorkDefinition(WorkDefinitionFactory.WorkDefinitionInfo info) {
        super(info);

        final var workDefBean = info.getBean();
        if (!(workDefBean instanceof MappingWorkDefinitionType workDef)) {
            throw new IllegalArgumentException("Expected " + MappingWorkDefinitionType.class.getSimpleName()
                    + " but got: " + workDefBean.getClass());
        }

        ResourceObjectSetUtil.setDefaultQueryApplicationMode(getResourceObjectSetSpecification(), APPEND);

        this.mappingsToUse = workDef.getMappings();
    }

    public List<InlineMappingDefinitionType> provideMappings() {
        return this.mappingsToUse.getInlineMappings();
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        super.debugDumpContent(sb, indent);
        DebugUtil.debugDumpWithLabel(sb, "mappings", this.mappingsToUse, indent + 1);
    }
}
