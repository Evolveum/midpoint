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
 * Special construction subclass that represents outbound constructions.
 *
 * @author Radovan Semancik
 * <p>
 * This class is Serializable but it is not in fact serializable. It
 * implements Serializable interface only to be storable in the
 * PrismPropertyValue.
 */
public class OutboundConstruction<AH extends AssignmentHolderType> extends Construction<AH, EvaluatedOutboundConstructionImpl<AH>> {

    private static final Trace LOGGER = TraceManager.getTrace(OutboundConstruction.class);

    @NotNull private final LensProjectionContext projectionContext;

    public OutboundConstruction(@NotNull LensProjectionContext projectionContext) {
        super(null, projectionContext.getResource());
        this.projectionContext = projectionContext;
        setResolvedResource(new Construction.ResolvedResource(projectionContext.getResource()));
    }

    @Override
    public ResourceType getResource() {
        return (ResourceType)getSource();
    }

    @Override
    public NextRecompute evaluate(Task task, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, SecurityViolationException, ConfigurationException, CommunicationException {
        // Subresult is needed here. If something fails here, this needs to be recorded as a subresult of
        // AssignmentProcessor.processAssignments. Otherwise partial error won't be propagated properly.
        OperationResult result = parentResult.createMinorSubresult(OP_EVALUATE);
        try {
            initializeDefinitions();
            createEvaluatedConstructions(task, result);
            NextRecompute nextRecompute = evaluateConstructions(task, result);
            result.recordSuccess();
            return nextRecompute;
        } catch (Throwable e) {
            result.recordFatalError(e);
            throw e;
        }
    }

    private void initializeDefinitions() throws SchemaException {
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
