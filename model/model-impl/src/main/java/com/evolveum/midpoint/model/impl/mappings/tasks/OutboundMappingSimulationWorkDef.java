/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.model.impl.mappings.tasks;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ObjectSetSpecificationProvider;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Work definition for outbound mappings simulation activity.
 */
class OutboundMappingSimulationWorkDef extends AbstractWorkDefinition
        implements ObjectSetSpecificationProvider, MappingSimulationWorkDef<OutboundMappingType> {
    private static final Trace LOGGER = TraceManager.getTrace(OutboundMappingSimulationWorkDef.class);

    private final OutboundMappingsSimulationWorkDefType workDefinition;
    private final BasicResourceObjectSetType resourceObjects;
    private final ObjectSetType focusObjects;

    OutboundMappingSimulationWorkDef(WorkDefinitionFactory.WorkDefinitionInfo info,
            OutboundMappingsSimulationWorkDefType workDef) {
        super(info);

        this.resourceObjects = MiscUtil.argNonNull(workDef.getResourceObjects(), "Resource objects has to be defined.");
        MiscUtil.argNonNull(this.resourceObjects.getResourceRef(), "Resource reference has to be defined.");
        MiscUtil.argCheck(workDef.getInlineMappings() != null && !workDef.getInlineMappings().isEmpty(),
                "Inline mappings are required for the outbound mappings simulation.");

        final String intent = this.resourceObjects.getIntent();
        final ShadowKindType kind = this.resourceObjects.getKind();
        if (kind == null || intent == null || intent.isBlank()) {
            LOGGER.debug("Kind and/or intent is not specified. Defaults will be used instead.");
        }

        this.focusObjects = workDef.getFocusObjects() != null
                ? workDef.getFocusObjects().clone()
                : new ObjectSetType().type(FocusType.COMPLEX_TYPE);

        this.workDefinition = workDef;
    }

    @Override
    public @NotNull ObjectSetType getObjectSetSpecification() {
        return this.focusObjects;
    }

    @Override
    public Map<ItemPath, List<OutboundMappingType>> provideMappings() {
        return this.workDefinition.getInlineMappings().stream()
                .filter(item -> !item.getOutbound().isEmpty())
                .collect(Collectors.toMap(
                        item -> item.getRef().getItemPath(), InlineOutboundMappingsDefinitionType::getOutbound));
    }

    @Override
    public boolean excludeExistingMappings() {
        // Currently we do not support inclusion of the existing mappings in outbound simulation.
        return true;
    }

    @Override
    public String resourceOid() {
        return this.resourceObjects.getResourceRef().getOid();
    }

    @Override
    public ResourceObjectTypeIdentification resolveObjectTypeId() {
        return ResourceObjectTypeIdentification.of(
                ShadowUtil.resolveDefault(this.resourceObjects.getKind()),
                ShadowUtil.resolveDefault(resourceObjects.getIntent()));
    }

    @Override
    protected void debugDumpContent(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabel(sb, "resourceObjects", this.resourceObjects, indent+1);
        DebugUtil.debugDumpWithLabel(sb, "focusObjects", getObjectSetSpecification(), indent+1);
        DebugUtil.debugDumpWithLabel(sb, "mappings", provideMappings(), indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "excludeExistingMappings", excludeExistingMappings(), indent + 1);
    }

}
