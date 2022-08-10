/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.focus;

import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.projector.ProjectorProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.NextRecompute;
import com.evolveum.midpoint.model.impl.lens.projector.util.ProcessorExecution;
import com.evolveum.midpoint.model.impl.lens.projector.util.ProcessorMethod;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateMappingEvaluationPhaseType.*;

/**
 * Processor to handle object template.
 *
 * @author Radovan Semancik
 */
@Component
@ProcessorExecution(focusRequired = true, focusType = FocusType.class, skipWhenFocusDeleted = true)
public class ObjectTemplateProcessor implements ProjectorProcessor {

    @Autowired private ModelBeans beans;

    @ProcessorMethod
    <AH extends AssignmentHolderType> void processTemplateBeforeAssignments(LensContext<AH> context,
            XMLGregorianCalendar now, Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, CommunicationException {
        TemplateMappingsEvaluation<AH, AH> evaluation = TemplateMappingsEvaluation.createForStandardTemplate(
                beans, context, BEFORE_ASSIGNMENTS, now, task, result);
        evaluation.computeItemDeltas();
        applyEvaluationResultsToFocus(evaluation);
    }

    @ProcessorMethod
    <AH extends AssignmentHolderType> void processTemplateAfterAssignments(LensContext<AH> context,
            XMLGregorianCalendar now, Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, CommunicationException {
        TemplateMappingsEvaluation<AH, AH> evaluation = TemplateMappingsEvaluation.createForStandardTemplate(
                beans, context, AFTER_ASSIGNMENTS, now, task, result);
        evaluation.computeItemDeltas();
        applyEvaluationResultsToFocus(evaluation);
    }

    @ProcessorMethod
    public <AH extends AssignmentHolderType> void processTemplateAfterProjections(LensContext<AH> context,
            XMLGregorianCalendar now, Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, CommunicationException {
        TemplateMappingsEvaluation<AH, AH> evaluation = TemplateMappingsEvaluation.createForStandardTemplate(
                beans, context, AFTER_PROJECTIONS, now, task, result);
        evaluation.computeItemDeltas();
        applyEvaluationResultsToFocus(evaluation);
    }

    private <AH extends AssignmentHolderType> void applyEvaluationResultsToFocus(TemplateMappingsEvaluation<AH, AH> evaluation)
            throws SchemaException {
        LensFocusContext<AH> focusContext = evaluation.getFocusContext();
        NextRecompute nextRecompute = evaluation.getNextRecompute();
        focusContext.swallowToSecondaryDelta(evaluation.getItemDeltas());
        if (nextRecompute != null) {
            nextRecompute.createTrigger(focusContext);
        }
        focusContext.recompute();
        focusContext.setItemDefinitionsMap(evaluation.getItemDefinitionsMap());
    }
}
