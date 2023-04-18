/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.focus;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.model.impl.lens.projector.Components;
import com.evolveum.midpoint.model.impl.lens.projector.ProjectorProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.Projector;
import com.evolveum.midpoint.model.impl.lens.projector.credentials.CredentialsProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.util.ProcessorExecution;
import com.evolveum.midpoint.model.impl.lens.projector.util.ProcessorMethod;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Handles everything about AssignmentHolder-typed focus:
 * <ol>
 *  <li>inbounds (for FocusType),</li>
 *  <li>activation before object template (for FocusType),</li>
 *  <li>object template before assignments,</li>
 *  <li>activation after object template (for FocusType),</li>
 *  <li>assignments (including processing orgs, membership/delegate refs, conflicts),</li>
 *  <li>focus lifecycle,</li>
 *  <li>object template after assignments,</li>
 *  <li>activation after second object template (for FocusType),</li>
 *  <li>credentials (for FocusType),</li>
 *  <li>focus policy rules.</li>
 * </ol>
 *
 * All of this is executed with regard to iteration. See also {@link IterationHelper}.
 * Also takes care for minor things like item limitations
 *
 * @author Radovan Semancik
 *
 */
@Component
@ProcessorExecution(focusRequired = true, focusType = AssignmentHolderType.class)
public class AssignmentHolderProcessor implements ProjectorProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentHolderProcessor.class);

    @Autowired private InboundProcessor inboundProcessor;
    @Autowired private AssignmentProcessor assignmentProcessor;
    @Autowired private ObjectTemplateProcessor objectTemplateProcessor;
    @Autowired private PrismContext prismContext;
    @Autowired private ExpressionFactory expressionFactory;
    @Autowired private PolicyRuleProcessor policyRuleProcessor;
    @Autowired private FocusLifecycleProcessor focusLifecycleProcessor;
    @Autowired private ClockworkMedic medic;
    @Autowired private CacheConfigurationManager cacheConfigurationManager;
    @Autowired private CredentialsProcessor credentialsProcessor;
    @Autowired private ItemLimitationsChecker itemLimitationsChecker;
    @Autowired private FocusActivationProcessor focusActivationProcessor;

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService cacheRepositoryService;

    @ProcessorMethod
    public <AH extends AssignmentHolderType> void processFocus(
            LensContext<AH> context, String activityDescription, XMLGregorianCalendar now, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, ConfigurationException,
            CommunicationException, SecurityViolationException, PolicyViolationException, ObjectAlreadyExistsException,
            ConflictDetectedException {

        LensFocusContext<AH> focusContext = context.getFocusContext();
        PartialProcessingOptionsType partialProcessingOptions = context.getPartialProcessingOptions();

        IterationHelper<AH> iterationHelper = new IterationHelper<>(this, context, focusContext);

        while (true) {

            ArchetypePolicyType archetypePolicy = focusContext.getArchetypePolicy();
            LensUtil.applyObjectPolicyConstraints(focusContext, archetypePolicy, prismContext);

            iterationHelper.onIterationStart(task, result);

            if (iterationHelper.doesPreIterationConditionHold(task, result)) {

                // INBOUND

                context.checkConsistenceIfNeeded();

                boolean inboundRun =
                        medic.partialExecute(Components.INBOUND, inboundProcessor,
                                inboundProcessor::processInbounds,
                                partialProcessingOptions::getInbound,
                                Projector.class, context, activityDescription, now, task, result);

                if (inboundRun && !focusContext.isDelete() && iterationHelper.didIterationSpecificationChange()) {
                    iterationHelper.restoreContext();
                    continue;
                }

                // ACTIVATION

                medic.partialExecute(
                        Components.FOCUS_ACTIVATION, focusActivationProcessor,
                        focusActivationProcessor::processActivationBeforeObjectTemplate,
                        partialProcessingOptions::getFocusActivation,
                        Projector.class, context, now, task, result);

                // OBJECT TEMPLATE (before assignments)

                medic.partialExecute(
                        Components.OBJECT_TEMPLATE_BEFORE_ASSIGNMENTS, objectTemplateProcessor,
                        objectTemplateProcessor::processTemplateBeforeAssignments,
                        partialProcessingOptions::getObjectTemplateBeforeAssignments,
                        Projector.class, context, now, task, result);

                // Process activation again. Object template might have changed it.

                medic.partialExecute(
                        Components.FOCUS_ACTIVATION, focusActivationProcessor,
                        focusActivationProcessor::processActivationAfterObjectTemplate,
                        partialProcessingOptions::getFocusActivation,
                        Projector.class, context, now, task, result);

                // ASSIGNMENTS

                focusContext.clearPendingPolicyStateModifications();

                medic.partialExecute(
                        Components.ASSIGNMENTS, assignmentProcessor,
                        assignmentProcessor::processAssignments,
                        partialProcessingOptions::getAssignments,
                        Projector.class, context, now, task, result);

                medic.partialExecute(
                        Components.ASSIGNMENTS_ORG, assignmentProcessor,
                        assignmentProcessor::processOrgAssignments,
                        partialProcessingOptions::getAssignmentsOrg,
                        Projector.class, context, now, task, result);

                medic.partialExecute(
                        Components.ASSIGNMENTS_MEMBERSHIP_AND_DELEGATE, assignmentProcessor,
                        assignmentProcessor::processMembershipAndDelegatedRefs,
                        partialProcessingOptions::getAssignmentsMembershipAndDelegate,
                        Projector.class, context, now, task, result);

                medic.partialExecute(
                        Components.ASSIGNMENTS_CONFLICTS, assignmentProcessor,
                        assignmentProcessor::checkForAssignmentConflicts,
                        partialProcessingOptions::getAssignmentsConflicts,
                        Projector.class, context, now, task, result);

                medic.partialExecute(
                        Components.FOCUS_LIFECYCLE, focusLifecycleProcessor,
                        focusLifecycleProcessor::process,
                        partialProcessingOptions::getFocusLifecycle,
                        Projector.class, context, now, task, result);

                // OBJECT TEMPLATE (after assignments)

                medic.partialExecute(
                        Components.OBJECT_TEMPLATE_AFTER_ASSIGNMENTS, objectTemplateProcessor,
                        objectTemplateProcessor::processTemplateAfterAssignments,
                        partialProcessingOptions::getObjectTemplateAfterAssignments,
                        Projector.class, context, now, task, result);

                // Process activation again. Second pass through object template might have changed it.
                // We also need to apply assignment activation if needed.

                medic.partialExecute(
                        Components.FOCUS_ACTIVATION, focusActivationProcessor,
                        focusActivationProcessor::processActivationAfterAssignments,
                        partialProcessingOptions::getFocusActivation,
                        Projector.class, context, now, task, result);

                // CREDENTIALS (including PASSWORD POLICY)

                medic.partialExecute(
                        Components.FOCUS_CREDENTIALS, credentialsProcessor,
                        credentialsProcessor::processFocusCredentials,
                        partialProcessingOptions::getFocusCredentials,
                        Projector.class, context, now, task, result);

                // We need to evaluate this as a last step. We need to make sure we have all the
                // focus deltas so we can properly trigger the rules.

                medic.partialExecute(
                        Components.FOCUS_POLICY_RULES, policyRuleProcessor,
                        policyRuleProcessor::evaluateAndRecordFocusPolicyRules,
                        partialProcessingOptions::getFocusPolicyRules,
                        Projector.class, context, now, task, result);

                // Processing done, check for success

                if (iterationHelper.didResetOnRenameOccur()) {
                    iterationHelper.restoreContext();
                    continue;
                }

                PrismObject<AH> objectNew = focusContext.getObjectNew();
                if (objectNew == null) {
                    LOGGER.trace("Object delete: skipping name presence and uniqueness check");
                    break;
                }

                if (iterationHelper.isIterationOk(objectNew, task, result)) {
                    break;
                }

                if (iterationHelper.shouldResetOnConflict()) {
                    iterationHelper.restoreContext();
                    continue;
                }
            }

            iterationHelper.incrementIterationCounter();
            iterationHelper.restoreContext();
        }

        iterationHelper.createIterationTokenDeltas();

        context.recomputeFocus();
        itemLimitationsChecker.checkItemsLimitations(focusContext);

        medic.partialExecute(
                Components.POLICY_RULE_COUNTERS,
                policyRuleProcessor,
                policyRuleProcessor::updateCounters,
                partialProcessingOptions::getPolicyRuleCounters,
                Projector.class, context, now, task, result);

        context.checkConsistenceIfNeeded();

        medic.traceContext(LOGGER, activityDescription, "focus processing", false, context, false);
        LensUtil.checkContextSanity(context, "focus processing", result);
    }

    ExpressionFactory getExpressionFactory() {
        return expressionFactory;
    }

    PrismContext getPrismContext() {
        return prismContext;
    }

    RepositoryService getCacheRepositoryService() {
        return cacheRepositoryService;
    }

    CacheConfigurationManager getCacheConfigurationManager() {
        return cacheConfigurationManager;
    }
}
