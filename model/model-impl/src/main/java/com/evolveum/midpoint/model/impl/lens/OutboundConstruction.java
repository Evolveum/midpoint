/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.impl.lens.projector.mappings.NextRecompute;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.refinery.*;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingEvaluator;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ReferentialIntegrityType;

/**
 * Special construction subclass that represents outbound constructions.
 *
 * @author Radovan Semancik
 * <p>
 * This class is Serializable but it is not in fact serializable. It
 * implements Serializable interface only to be storable in the
 * PrismPropertyValue.
 */
public class OutboundConstruction<AH extends AssignmentHolderType> extends Construction<AH> {

    private static final Trace LOGGER = TraceManager.getTrace(OutboundConstruction.class);

    public OutboundConstruction(RefinedObjectClassDefinition rOcDef, ResourceType source) {
        super(null, source);
        setResolvedResource(new Construction.ResolvedResource(source));
        setRefinedObjectClassDefinition(rOcDef);
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
            evaluateConstructions(task, result);
            result.recordSuccess();
            return null;
        } catch (Throwable e) {
            result.recordFatalError(e);
            throw e;
        }
    }

    private void initializeDefinitions() {
        Collection<RefinedObjectClassDefinition> auxiliaryObjectClassDefinitions = getRefinedObjectClassDefinition().getAuxiliaryObjectClassDefinitions();
        for (RefinedObjectClassDefinition auxiliaryObjectClassDefinition: auxiliaryObjectClassDefinitions) {
            addAuxiliaryObjectClassDefinition(auxiliaryObjectClassDefinition);
        }
    }

    @Override
    protected EvaluatedConstructionImpl<AH> createEvaluatedConstruction(ResourceShadowDiscriminator rsd) {
        return new EvaluatedOutboundConstructionImpl<>(this, rsd);
    }

}
