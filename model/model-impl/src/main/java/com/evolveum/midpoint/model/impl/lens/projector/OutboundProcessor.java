/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.lens.construction.OutboundConstruction;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingEvaluator;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.NextRecompute;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Processor that evaluates values of the outbound mappings. It does not create the deltas yet. It just collects the
 * evaluated mappings in account context.
 *
 * @author Radovan Semancik
 */
@Component
public class OutboundProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(OutboundProcessor.class);

    private PrismContainerDefinition<ShadowAssociationType> associationContainerDefinition;

    @Autowired private PrismContext prismContext;
    @Autowired private MappingFactory mappingFactory;
    @Autowired private MappingEvaluator mappingEvaluator;
    @Autowired private ContextLoader contextLoader;
    @Autowired private Clock clock;
    @Autowired private ModelObjectResolver objectResolver;

    <AH extends AssignmentHolderType> void processOutbound(LensContext<AH> context, LensProjectionContext projCtx, Task task, OperationResult result) throws SchemaException,
            ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {

        ResourceShadowDiscriminator discr = projCtx.getResourceShadowDiscriminator();
        if (projCtx.isDelete()) {
            LOGGER.trace("Processing outbound expressions for {} skipped, DELETE account delta", discr);
            // No point in evaluating outbound
            return;
        }

        LOGGER.trace("Processing outbound expressions for {} starting", discr);

        // Each projection is evaluated in a single wave only. So we take into account all changes of focus from wave 0 to this one.
        ObjectDeltaObject<AH> focusOdoAbsolute = context.getFocusContext().getObjectDeltaObjectAbsolute();

        OutboundConstruction<AH> outboundConstruction = new OutboundConstruction<>(projCtx);
        outboundConstruction.setFocusOdoAbsolute(focusOdoAbsolute);
        outboundConstruction.setLensContext(context);
        outboundConstruction.setObjectResolver(objectResolver);
        outboundConstruction.setPrismContext(prismContext);
        outboundConstruction.setMappingFactory(mappingFactory);
        outboundConstruction.setMappingEvaluator(mappingEvaluator);
        outboundConstruction.setNow(clock.currentTimeXMLGregorianCalendar());
        outboundConstruction.setContextLoader(contextLoader);
        outboundConstruction.setOriginType(OriginType.ASSIGNMENTS);
        outboundConstruction.setChannel(context.getChannel());
        outboundConstruction.setValid(true);

        NextRecompute nextRecompute = outboundConstruction.evaluate(task, result);

        projCtx.setOutboundConstruction(outboundConstruction);
        if (nextRecompute != null) {
            nextRecompute.createTrigger(context.getFocusContext());
        }

        context.recompute();
        context.checkConsistenceIfNeeded();
    }


    private PrismContainerDefinition<ShadowAssociationType> getAssociationContainerDefinition() {
        if (associationContainerDefinition == null) {
            PrismObjectDefinition<ShadowType> shadowDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
            associationContainerDefinition = shadowDefinition.findContainerDefinition(ShadowType.F_ASSOCIATION);
        }
        return associationContainerDefinition;
    }
}
