/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.model.impl.mappings.tasks;

import com.evolveum.midpoint.model.impl.sync.tasks.ResourceSetTaskWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingWorkDefinitionType;

/**
 * Work definition for mapping simulation activity.
 */
public class MappingWorkDefinition extends ResourceSetTaskWorkDefinition {

    private final MappingType inlineMapping;

    public MappingWorkDefinition(WorkDefinitionFactory.WorkDefinitionInfo info) {
        super(info);

        final var workDefBean = info.getBean();
        if (!(workDefBean instanceof MappingWorkDefinitionType workDef)) {
            throw new IllegalArgumentException("Expected " + MappingWorkDefinitionType.class.getSimpleName()
                    + " but got: " + workDefBean.getClass());
        }

        this.inlineMapping = workDef.getInlineMapping();
        if (this.inlineMapping == null) {
            throw new IllegalArgumentException("Inline mapping must be specified");
        }
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        super.debugDumpContent(sb, indent);
        DebugUtil.debugDumpWithLabel(sb, "inlineMapping", inlineMapping, indent + 1);
    }
}
