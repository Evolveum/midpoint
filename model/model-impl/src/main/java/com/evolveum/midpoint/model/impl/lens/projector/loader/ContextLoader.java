/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.loader;

import static com.evolveum.midpoint.schema.GetOperationOptions.createReadOnlyCollection;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.getOid;
import static com.evolveum.midpoint.util.DebugUtil.debugDumpLazily;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.*;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.util.ArchetypeTypeUtil;
import com.evolveum.midpoint.util.MiscUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.common.archetypes.ArchetypeManager;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.executor.FocusChangeExecution;
import com.evolveum.midpoint.model.impl.lens.projector.ProjectorProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.util.ProcessorExecution;
import com.evolveum.midpoint.model.impl.lens.projector.util.ProcessorMethod;
import com.evolveum.midpoint.model.impl.security.SecurityHelper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Context loader loads the missing parts of the context. The context enters the projector with just the minimum information.
 * Context loader gets missing data such as accounts. It gets them from the repository or provisioning as necessary. It follows
 * the account links in focus (linkRef) and focus deltas.
 *
 * @author Radovan Semancik
 */
@Component
@ProcessorExecution()
public class ContextLoader implements ProjectorProcessor {

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService cacheRepositoryService;
    @Autowired private ArchetypeManager archetypeManager;
    @Autowired private SecurityHelper securityHelper;

    private static final Trace LOGGER = TraceManager.getTrace(ContextLoader.class);

    public static final String CLASS_DOT = ContextLoader.class.getName() + ".";

    /**
     * How many times do we try to load the context: the load is repeated if the focus
     * is updated by an embedded clockwork run (presumably due to discovery process).
     */
    private static final int MAX_LOAD_ATTEMPTS = 3;

    /**
     * Loads the whole context.
     *
     * The loading is repeated if the focus is modified during the `load` operation. See MID-7725.
     */
    @ProcessorMethod
    public <F extends ObjectType> void load(
            @NotNull LensContext<F> context,
            @NotNull String activityDescription,
            XMLGregorianCalendar ignored,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException {

        for (int loadAttempt = 1; ; loadAttempt++) {
            Set<String> modifiedFocusOids = new HashSet<>();
            FocusChangeExecution.ChangeExecutionListener listener = modifiedFocusOids::add;
            FocusChangeExecution.registerChangeExecutionListener(listener);
            try {
                new ContextLoadOperation<>(context, activityDescription, task)
                        .load(result);
            } finally {
                FocusChangeExecution.unregisterChangeExecutionListener(listener);
            }
            LOGGER.trace("Focus OID/OIDs modified during load operation in this thread: {}", modifiedFocusOids);
            LensFocusContext<F> focusContext = context.getFocusContext();
            if (focusContext != null
                    && focusContext.getOid() != null
                    && modifiedFocusOids.contains(focusContext.getOid())) {
                if (loadAttempt == MAX_LOAD_ATTEMPTS) {
                    LOGGER.warn("Focus was repeatedly modified during loading too many times ({}) - continuing,"
                                    + " but it's suspicious", MAX_LOAD_ATTEMPTS);
                    return;
                } else {
                    LOGGER.debug("Detected modification of the focus during 'load' operation, retrying the loading (#{})",
                            loadAttempt);
                    context.rot("focus modification during loading");
                    if (context.isInInitial()) {
                        // This is a partial solution to MID-9103. The problem is the inconsistency between old/new objects
                        // and the summary delta. The get out of sync when the current object is externally updated, without
                        // reflecting that in the deltas. This code does not resolve that in general; it only ensures the
                        // old-current-new consistency when the re-loading occurs in the initial clockwork state. See MID-9113.
                        focusContext.setRewriteOldObject();
                    }
                }
            } else {
                LOGGER.trace("No modification of the focus during 'load' operation, continuing");
                return;
            }
        }
    }

    /** Loads just the focus context; projections are ignored at this moment. */
    public <O extends ObjectType> void loadFocusContext(LensContext<O> context, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        new FocusLoadOperation<>(context, task)
                .load(result);
    }

    /** Updates the projection context. For exact meaning see {@link ProjectionUpdateOperation}. */
    public <F extends ObjectType> void updateProjectionContext(
            LensContext<F> context,
            LensProjectionContext projectionContext,
            Task task,
            OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        new ProjectionUpdateOperation<>(context, projectionContext, task)
                .update(result);
    }

    /**
     * Updates the following in the focus context:
     *
     * - list of archetypes,
     * - (merged) archetype policy (stemming from archetypes),
     * - focus template (stemming from archetype policy).
     *
     * @param overwrite If true, we overwrite the information (archetype policy) previously set.
     * This flag is set e.g. after inbounds. [Does not apply to externally set focus template. That one is never changed.]
     */
    public <F extends ObjectType> void updateArchetypePolicyAndRelatives(
            @NotNull LensFocusContext<F> focusContext, boolean overwrite, @NotNull Task task, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException, PolicyViolationException {
        updateArchetypesAndArchetypePolicy(focusContext, overwrite, result);
        updateFocusTemplate(focusContext, overwrite, task, result);
    }

    private void updateArchetypesAndArchetypePolicy(
            @NotNull LensFocusContext<?> focusContext, boolean overwrite, @NotNull OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, PolicyViolationException {
        if (focusContext.getArchetypePolicy() != null && !overwrite) {
            LOGGER.trace("Archetype policy is already set, not updating it as `overwrite` flag is not set");
        } else {
            ObjectType before = asObjectable(focusContext.getObjectCurrentOrOld());
            ObjectType after = asObjectable(focusContext.getObjectNew());
            ObjectType mostRecent = MiscUtil.getFirstNonNull(after, before);

            // These collections are modified by archetype enforcement method below.
            Set<String> archetypeOids =
                    new HashSet<>(
                            archetypeManager.determineArchetypeOids(before, after));
            List<ArchetypeType> archetypes =
                    new ArrayList<>(
                            archetypeManager.resolveArchetypeOids(archetypeOids, mostRecent, result));

            enforceArchetypesFromProjections(focusContext, archetypeOids, archetypes, result);

            ArchetypePolicyType newArchetypePolicy =
                    archetypeManager.determineArchetypePolicy(archetypes, mostRecent, result);
            logArchetypePolicyUpdate(focusContext, newArchetypePolicy);

            focusContext.setArchetypePolicy(newArchetypePolicy);
            focusContext.setArchetypes(
                    Collections.unmodifiableList(archetypes));
        }
    }

    /**
     * Enforces archetypes defined in projections. May modify archetypeOids/archetypes!
     *
     * Note that this code is temporary. It is expected to change in the future, e.g. we may make the handling of these
     * archetypes configurable.
     */
    private void enforceArchetypesFromProjections(
            @NotNull LensFocusContext<?> focusContext,
            @NotNull Set<String> archetypeOids,
            @NotNull List<ArchetypeType> archetypes,
            @NotNull OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, PolicyViolationException {
        for (LensProjectionContext projectionContext : focusContext.getLensContext().getProjectionContexts()) {
            String enforcedArchetypeOid = projectionContext.getConfiguredFocusArchetypeOid();
            if (enforcedArchetypeOid != null) {
                enforceArchetypeFromProjection(
                        focusContext, projectionContext, enforcedArchetypeOid, archetypeOids, archetypes, result);
            }
        }
    }

    private void enforceArchetypeFromProjection(
            @NotNull LensFocusContext<?> focusContext,
            @NotNull LensProjectionContext projectionContext, // just for informational purposes
            @NotNull String enforcedArchetypeOid,
            @NotNull Set<String> archetypeOids,
            @NotNull List<ArchetypeType> archetypes,
            @NotNull OperationResult result) throws SchemaException, ObjectNotFoundException, PolicyViolationException {
        if (archetypeOids.contains(enforcedArchetypeOid)) {
            LOGGER.trace("Enforcing archetype OID {} from {}: already present (nothing to do here)",
                    enforcedArchetypeOid, projectionContext);
            return;
        }

        ArchetypeType enforcedArchetype = archetypeManager.getArchetype(enforcedArchetypeOid, result);
        // Note that unlike during resolution of archetypes in the focus object, here we intentionally
        // fail when the archetype does not exist.

        stateCheck(ArchetypeTypeUtil.isStructural(enforcedArchetype),
                "Archetype %s enforced by %s is not a structural one", enforcedArchetype, projectionContext);

        LOGGER.trace("Going to enforce {} (by {})", enforcedArchetype, projectionContext);
        checkForArchetypeEnforcementConflicts(focusContext, projectionContext, enforcedArchetype, archetypes);

        archetypeOids.add(enforcedArchetypeOid);
        archetypes.add(enforcedArchetype);

        // We create an assignment delta, but not archetypeRef one. The reason is that we hope that the assignment evaluator
        // will be run after this code. (It should be usually so.)
        focusContext.swallowToSecondaryDelta(
                PrismContext.get().deltaFor(AssignmentHolderType.class)
                        .item(AssignmentHolderType.F_ASSIGNMENT)
                        .add(new AssignmentType()
                                .targetRef(enforcedArchetypeOid, ArchetypeType.COMPLEX_TYPE))
                        .asItemDelta());
    }

    private void checkForArchetypeEnforcementConflicts(
            @NotNull LensFocusContext<?> focusContext,
            @NotNull LensProjectionContext projectionContext,
            @NotNull ArchetypeType enforcedArchetype,
            @NotNull List<ArchetypeType> archetypes) throws SchemaException, PolicyViolationException {
        ArchetypeType existingStructural = ArchetypeTypeUtil.getStructuralArchetype(archetypes);
        if (existingStructural != null) {
            assert !enforcedArchetype.getOid().equals(existingStructural.getOid());
            throw new PolicyViolationException(
                    String.format("Trying to enforce %s on %s (because of %s); but the object has already"
                                    + " a different structural archetype: %s",
                            enforcedArchetype, focusContext.getObjectAny(), projectionContext.getHumanReadableName(),
                            existingStructural));
        } else if (!archetypes.isEmpty()) {
            // This should have been checked elsewhere. But let's double-check here. It is dangerous to set a structural
            // archetype when there are (any) auxiliary ones.
            throw new PolicyViolationException(
                    String.format("Trying to enforce %s on %s (because of %s); but the object has already"
                                    + " some auxiliary archetypes: %s",
                            enforcedArchetype, focusContext.getObjectAny(), projectionContext.getHumanReadableName(), archetypes));
        }
    }

    private <F extends ObjectType> void logArchetypePolicyUpdate(
            LensFocusContext<F> focusContext, ArchetypePolicyType newPolicy) {
        if (LOGGER.isTraceEnabled()) {
            if (Objects.equals(newPolicy, focusContext.getArchetypePolicy())) {
                LOGGER.trace("Archetype policy has not changed");
            } else {
                LOGGER.trace("Updated archetype policy configuration:\n{}", debugDumpLazily(newPolicy, 1));
            }
        }
    }

    /**
     * Updates {@link LensFocusContext#focusTemplate} property according to {@link LensFocusContext#archetypePolicy}.
     */
    private <F extends ObjectType> void updateFocusTemplate(
            @NotNull LensFocusContext<F> focusContext,
            boolean overwrite,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException {

        ObjectTemplateType current = focusContext.getFocusTemplate();
        if (current != null) {
            if (focusContext.isFocusTemplateSetExplicitly()) {
                LOGGER.trace("Focus template {} was set explicitly, not updating it", current);
                return;
            }
            if (!overwrite) {
                LOGGER.trace("Focus template is already set, not updating it as `overwrite` flag is not set: {}", current);
                return;
            }
        }

        String currentOid = getOid(current);
        String newOid = determineNewTemplateOid(focusContext);

        LOGGER.trace("current focus template OID = {}, new = {}", currentOid, newOid);
        if (!Objects.equals(currentOid, newOid)) {
            resolveAndSetTemplate(focusContext, newOid, task, result);
        }
    }

    @Nullable
    private String determineNewTemplateOid(@NotNull LensFocusContext<?> focusContext) {
        String explicitFocusTemplateOid = focusContext.getLensContext().getExplicitFocusTemplateOid();
        if (explicitFocusTemplateOid != null) {
            return explicitFocusTemplateOid;
        }

        ArchetypePolicyType archetypePolicy = focusContext.getArchetypePolicy();
        if (archetypePolicy == null) {
            LOGGER.trace("No default object template (no archetype policy)");
            return null;
        } else {
            ObjectReferenceType templateRef = archetypePolicy.getObjectTemplateRef();
            if (templateRef == null) {
                LOGGER.trace("No default object template (no templateRef in archetype policy)");
                return null;
            } else {
                return templateRef.getOid();
            }
        }
    }

    private <F extends ObjectType> void resolveAndSetTemplate(
            @NotNull LensFocusContext<F> focusContext, String newOid, @NotNull Task task, @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException {
        if (newOid != null) {
            focusContext.setFocusTemplate(
                    cacheRepositoryService
                            .getObject(ObjectTemplateType.class, newOid, createReadOnlyCollection(), result)
                            .asObjectable());
            focusContext.setExpandedFocusTemplate(
                    archetypeManager.getExpandedObjectTemplate(newOid, task.getExecutionMode(), result));
        } else {
            focusContext.setFocusTemplate(null);
            focusContext.setExpandedFocusTemplate(null);
        }
    }

    /**
     * FIXME this method sometimes return repo-only shadow in the case of consistency mechanism is applied;
     *  see `TestConsistencyReaper.test150` and MID-7970.
     */
    public void loadFullShadow(
            @NotNull LensProjectionContext projCtx, String reason, Task task, OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        new ProjectionFullLoadOperation<>(projCtx.getLensContext(), projCtx, reason, false, task)
                .loadFullShadow(result);
    }

    @SuppressWarnings("SameParameterValue")
    public void loadFullShadowNoDiscovery(
            @NotNull LensProjectionContext projCtx, String reason, Task task, OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        new ProjectionFullLoadOperation<>(projCtx.getLensContext(), projCtx, reason, true, task)
                .loadFullShadow(result);
    }

    public <F extends FocusType> void reloadSecurityPolicyIfNeeded(@NotNull LensContext<F> context,
            @NotNull LensFocusContext<F> focusContext, Task task, OperationResult result)
            throws ExpressionEvaluationException, SchemaException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        if (focusContext.hasOrganizationalChange()) {
            loadSecurityPolicy(context, true, task, result);
        }
    }

    void loadSecurityPolicy(LensContext<?> context, Task task, OperationResult result)
            throws ExpressionEvaluationException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException {
        loadSecurityPolicy(context, false, task, result);
    }

    @SuppressWarnings("unchecked")
    private <O extends ObjectType> void loadSecurityPolicy(LensContext<O> context, boolean forceReload,
            Task task, OperationResult result) throws ExpressionEvaluationException,
            SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        LensFocusContext<O> genericFocusContext = context.getFocusContext();
        if (genericFocusContext == null || !genericFocusContext.represents(FocusType.class)) {
            LOGGER.trace("Skipping load of security policy because focus is not of FocusType");
            return;
        }
        LensFocusContext<FocusType> focusContext = (LensFocusContext<FocusType>) genericFocusContext;
        PrismObject<FocusType> focus = focusContext.getObjectAny();
        SecurityPolicyType globalSecurityPolicy = determineAndSetGlobalSecurityPolicy(context, focus, task, result);
        SecurityPolicyType focusSecurityPolicy =
                determineAndSetFocusSecurityPolicy(focusContext, focus, context.getSystemConfiguration(), forceReload, task, result);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Security policies:\n  Global:\n{}\n  Focus:\n{}",
                    DebugUtil.debugDump(globalSecurityPolicy, 2),
                    DebugUtil.debugDump(focusSecurityPolicy, 2));
        } else {
            LOGGER.debug("Security policies: global: {}, focus: {}", globalSecurityPolicy, focusSecurityPolicy);
        }
    }

    @NotNull
    private <O extends ObjectType> SecurityPolicyType determineAndSetGlobalSecurityPolicy(LensContext<O> context,
            PrismObject<FocusType> focus, Task task, OperationResult result)
            throws CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        SecurityPolicyType existingPolicy = context.getGlobalSecurityPolicy();
        if (existingPolicy != null) {
            return existingPolicy;
        } else {
            SecurityPolicyType loadedPolicy =
                    securityHelper.locateGlobalSecurityPolicy(focus, context.getSystemConfiguration(), task, result);

            // using empty policy to avoid repeated lookups
            SecurityPolicyType resultingPolicy = Objects.requireNonNullElseGet(loadedPolicy, SecurityPolicyType::new);
            context.setGlobalSecurityPolicy(resultingPolicy);
            return resultingPolicy;
        }
    }

    private SecurityPolicyType determineAndSetFocusSecurityPolicy(LensFocusContext<FocusType> focusContext,
            PrismObject<FocusType> focus, PrismObject<SystemConfigurationType> systemConfiguration, boolean forceReload, Task task,
            OperationResult result) throws CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException, SchemaException {
        SecurityPolicyType existingPolicy = focusContext.getSecurityPolicy();
        if (existingPolicy != null && !forceReload) {
            return existingPolicy;
        } else {
            SecurityPolicyType loadedPolicy = securityHelper.locateSecurityPolicy(focus, null, systemConfiguration,
                    task, result);  //todo review please locateSecurityPolicy tries to load security policy from org
                                    //and archetypes at first but if no one is found, return global security policy. therefore the usage
                                    //method locateFocusSecurityPolicyFromOrgs was replaced with locateSecurityPolicy and the following
                                    //peace of code was commented
//            SecurityPolicyType resultingPolicy;
//            if (loadedPolicy != null) {
//                resultingPolicy = loadedPolicy;
//            } else {
//                // Not very clean. In fact we should store focus security policy separate from global
//                // policy to avoid confusion. But need to do this to fix MID-4793 and backport the fix.
//                // Therefore avoiding big changes. TODO: fix properly later
//                resultingPolicy = globalSecurityPolicy;
//            }
            focusContext.setSecurityPolicy(loadedPolicy);
            return loadedPolicy;
        }
    }
}
