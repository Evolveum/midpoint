/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.construction;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

/**
 * Special construction subclass that represents resource object constructions as defined in the `schemaHandling`
 * section of the resource definition.
 *
 * The main difference from {@link AssignedResourceObjectConstruction} is that here we have the projection context,
 * so resource and definitions resolution is much easier. It also has no {@link #constructionBean}.
 *
 * TODO consider better name
 *
 * @author Radovan Semancik
 */
public class PlainResourceObjectConstruction<AH extends AssignmentHolderType>
        extends ResourceObjectConstruction<AH, EvaluatedPlainResourceObjectConstructionImpl<AH>> {

    private static final Trace LOGGER = TraceManager.getTrace(PlainResourceObjectConstruction.class);

    @NotNull private final LensProjectionContext projectionContext;

    PlainResourceObjectConstruction(PlainResourceObjectConstructionBuilder<AH> builder) {
        super(builder);
        this.projectionContext = builder.projectionContext;
    }

    @Override
    protected void resolveResource(Task task, OperationResult result) {
        setResolvedResource(
                new ResolvedConstructionResource(projectionContext.getResourceRequired()));
    }

    protected void initializeDefinitions() throws SchemaException, ConfigurationException {
        ResourceObjectDefinition rOcDef = projectionContext.getStructuralObjectDefinition();
        if (rOcDef == null) {
            LOGGER.error("Definition for {} not found in the context, but it should be there, dumping context:\n{}",
                    projectionContext.getKey(), lensContext.debugDump(1));
            throw new IllegalStateException("Definition for " + projectionContext.getKey()
                    + " not found in the context, but it should be there");
        }
        setResourceObjectDefinition(rOcDef);
        Collection<ResourceObjectDefinition> auxiliaryObjectClassDefinitions =
                getResourceObjectDefinition().getAuxiliaryDefinitions();
        for (ResourceObjectDefinition auxiliaryObjectClassDefinition: auxiliaryObjectClassDefinitions) {
            addAuxiliaryObjectClassDefinition(auxiliaryObjectClassDefinition);
        }
    }

    @Override
    protected EvaluatedPlainResourceObjectConstructionImpl<AH> createEvaluatedConstruction(@NotNull ConstructionTargetKey key) {
        return new EvaluatedPlainResourceObjectConstructionImpl<>(this, projectionContext, key);
    }
}
