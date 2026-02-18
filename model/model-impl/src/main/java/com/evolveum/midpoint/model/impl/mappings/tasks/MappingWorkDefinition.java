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
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.task.work.ResourceObjectSetUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Work definition for mapping simulation activity.
 */
public class MappingWorkDefinition extends ResourceSetTaskWorkDefinition {

    private final MappingWorkDefinitionType workDefinition;

    public MappingWorkDefinition(WorkDefinitionFactory.WorkDefinitionInfo info) {
        super(info);

        final var workDefBean = info.getBean();
        if (!(workDefBean instanceof MappingWorkDefinitionType workDef)) {
            throw new IllegalArgumentException("Expected " + MappingWorkDefinitionType.class.getSimpleName()
                    + " but got: " + workDefBean.getClass());
        }

        ResourceObjectSetUtil.setDefaultQueryApplicationMode(getResourceObjectSetSpecification(), APPEND);

        this.workDefinition = workDef;
    }

    public List<InlineMappingDefinitionType> provideMappings() {
        return this.workDefinition.getInlineMappings();
    }

    public boolean excludeExistingMappings() {
        return !Boolean.TRUE.equals(this.workDefinition.isIncludeExistingMappings());
    }

    public String resourceOid() {
        return this.workDefinition.getResourceObjects().getResourceRef().getOid();
    }

    public ResourceObjectTypeIdentification resolveObjectTypeId() {
        final ResourceObjectSetType resourceObjects = workDefinition.getResourceObjects();
        return ResourceObjectTypeIdentification.of(
                ShadowUtil.resolveDefault(resourceObjects.getKind()),
                ShadowUtil.resolveDefault(resourceObjects.getIntent()));
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        super.debugDumpContent(sb, indent);
        DebugUtil.debugDumpWithLabel(sb, "mappings", provideMappings(), indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "excludeExistingMappings", excludeExistingMappings(), indent + 1);
    }
}
