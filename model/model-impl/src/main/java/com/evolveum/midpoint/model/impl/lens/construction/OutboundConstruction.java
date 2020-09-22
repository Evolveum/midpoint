/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.construction;

import java.util.Collection;

import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.NextRecompute;

import com.evolveum.midpoint.common.refinery.*;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

/**
 * Special construction subclass that represents resource object constructions in the phase
 * of resource-defined outbound mappings.
 *
 * TODO decide what to do with this class
 *
 * @author Radovan Semancik
 */
public class OutboundConstruction<AH extends AssignmentHolderType>
        extends ResourceObjectConstruction<AH, EvaluatedOutboundConstructionImpl<AH>> {

    private static final Trace LOGGER = TraceManager.getTrace(OutboundConstruction.class);

    @NotNull private final LensProjectionContext projectionContext;

    public OutboundConstruction(OutboundConstructionBuilder<AH> builder) {
        super(builder);
        this.projectionContext = builder.projectionContext;
    }

    @Override
    protected void resolveResource(Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
        // already done on initialization
        if (getResource() == null) {
            throw new IllegalStateException("No resource in construction in " + source);
        }
    }

    protected void initializeDefinitions() throws SchemaException {
        RefinedObjectClassDefinition rOcDef = projectionContext.getStructuralObjectClassDefinition();
        if (rOcDef == null) {
            LOGGER.error("Definition for {} not found in the context, but it should be there, dumping context:\n{}", projectionContext.getResourceShadowDiscriminator(), getLensContext().debugDump(1));
            throw new IllegalStateException("Definition for " + projectionContext.getResourceShadowDiscriminator() + " not found in the context, but it should be there");
        }
        setRefinedObjectClassDefinition(rOcDef);
        Collection<RefinedObjectClassDefinition> auxiliaryObjectClassDefinitions = getRefinedObjectClassDefinition().getAuxiliaryObjectClassDefinitions();
        for (RefinedObjectClassDefinition auxiliaryObjectClassDefinition: auxiliaryObjectClassDefinitions) {
            addAuxiliaryObjectClassDefinition(auxiliaryObjectClassDefinition);
        }
    }

    @Override
    protected EvaluatedOutboundConstructionImpl<AH> createEvaluatedConstruction(ResourceShadowDiscriminator rsd) {
        return new EvaluatedOutboundConstructionImpl<>(this, projectionContext);
    }
}
