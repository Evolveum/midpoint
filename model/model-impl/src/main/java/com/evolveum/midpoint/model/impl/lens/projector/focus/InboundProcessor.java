/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.ClockworkInboundsProcessing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.ClockworkMedic;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.projector.loader.ContextLoader;
import com.evolveum.midpoint.model.impl.lens.projector.ProjectorProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.util.ProcessorExecution;
import com.evolveum.midpoint.model.impl.lens.projector.util.ProcessorMethod;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * Processor that takes changes from accounts and synchronization deltas and updates user attributes if necessary
 * (by creating secondary user object delta {@link ObjectDelta}).
 *
 * @author lazyman
 * @author Radovan Semancik
 */
@Component
@ProcessorExecution(focusRequired = true, focusType = FocusType.class, skipWhenFocusDeleted = true)
public class InboundProcessor implements ProjectorProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(InboundProcessor.class);

    @Autowired private ContextLoader contextLoader;
    @Autowired private ClockworkMedic medic;
    @Autowired private ModelBeans beans;

    @ProcessorMethod
    <F extends FocusType> void processInbounds(
            LensContext<F> context, String activityDescription, XMLGregorianCalendar now, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, ConfigurationException,
            CommunicationException, SecurityViolationException, PolicyViolationException {

        MappingEvaluationEnvironment env = new MappingEvaluationEnvironment(activityDescription, now, task);

        ClockworkInboundsProcessing<F> processing = new ClockworkInboundsProcessing<>(context, beans, env, result);
        processing.collectAndEvaluateMappings();

        context.checkConsistenceIfNeeded();
        context.recomputeFocus();
        medic.traceContext(LOGGER, activityDescription, "inbound", false, context, false);

        // It's actually a bit questionable if such cross-components interactions should be treated like this
        // or in some higher-level component. But let's try this approach until something nicer is found.
        contextLoader.updateArchetypePolicyAndRelatives(
                context.getFocusContextRequired(), true, task, result);
        context.checkConsistenceIfNeeded();
    }
}
