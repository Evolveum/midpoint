/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.loader;

import static com.evolveum.midpoint.schema.GetOperationOptions.createReadOnlyCollection;
import static com.evolveum.midpoint.util.DebugUtil.debugDumpLazily;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.model.impl.lens.executor.FocusChangeExecution;
import com.evolveum.midpoint.model.impl.lens.projector.ProjectorProcessor;
import com.evolveum.midpoint.prism.delta.ObjectDelta;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.common.ArchetypeManager;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
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
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

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
            if (focusContext != null && focusContext.getOid() != null && modifiedFocusOids.contains(focusContext.getOid())) {
                if (loadAttempt == MAX_LOAD_ATTEMPTS) {
                    LOGGER.warn("Focus was repeatedly modified during loading too many times ({}) - continuing,"
                                    + " but it's suspicious", MAX_LOAD_ATTEMPTS);
                    return;
                } else {
                    LOGGER.debug("Detected modification of the focus during 'load' operation, retrying the loading (#{})",
                            loadAttempt);
                    context.rot("focus modification during loading");
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

    <F extends ObjectType> ArchetypePolicyType determineArchetypePolicy(LensContext<F> context, OperationResult result)
            throws SchemaException, ConfigurationException {
        if (!canProcessArchetype(context)) {
            return null;
        }
        PrismObject<F> object = context.getFocusContext().getObjectAny();
        ObjectDelta<F> delta = context.getFocusContext().getPrimaryDelta();
        return archetypeManager.determineArchetypePolicy(object, delta, result);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private <O extends ObjectType> boolean canProcessArchetype(LensContext<O> context) {
        return context.getSystemConfiguration() != null && context.getFocusContext() != null;
    }

    public <F extends AssignmentHolderType> void updateArchetype(LensContext<F> context, OperationResult result)
            throws SchemaException {
        if (!canProcessArchetype(context)) {
            return;
        }

        PrismObject<F> object = context.getFocusContext().getObjectAny();

        List<PrismObject<ArchetypeType>> archetypes = archetypeManager.determineArchetypes(object, result);
        context.getFocusContext().setArchetypes(PrismObject.asObjectableList(archetypes));
    }

    public <F extends ObjectType> void updateArchetypePolicy(LensContext<F> context, OperationResult result)
            throws SchemaException, ConfigurationException {
        if (context.getFocusContext() == null) {
            return;
        }
        ArchetypePolicyType newArchetypePolicy = determineArchetypePolicy(context, result);
        if (newArchetypePolicy != context.getFocusContext().getArchetypePolicy()) {
            LOGGER.trace("Updated archetype policy configuration:\n{}", debugDumpLazily(newArchetypePolicy, 1));
            context.getFocusContext().setArchetypePolicy(newArchetypePolicy);
        }
    }

    // expects that object policy configuration is already set in focusContext
    public <F extends ObjectType> void updateFocusTemplate(LensContext<F> context, OperationResult result)
            throws ObjectNotFoundException, SchemaException {

        // 1. When this method is called after inbound processing, we might want to change the existing template
        //    (because e.g. subtype or archetype was determined and we want to move from generic to more specific template).
        // 2. On the other hand, if focus template is set up explicitly from the outside (e.g. in synchronization section)
        //    we probably do not want to change it here.
        if (context.getFocusTemplate() != null && context.isFocusTemplateExternallySet()) {
            return;
        }

        String currentOid = context.getFocusTemplate() != null ? context.getFocusTemplate().getOid() : null;
        String newOid;

        LensFocusContext<F> focusContext = context.getFocusContext();
        if (focusContext == null) {
            newOid = null;
        } else {
            ArchetypePolicyType archetypePolicy = focusContext.getArchetypePolicy();
            if (archetypePolicy == null) {
                LOGGER.trace("No default object template (no policy)");
                newOid = null;
            } else {
                ObjectReferenceType templateRef = archetypePolicy.getObjectTemplateRef();
                if (templateRef == null) {
                    LOGGER.trace("No default object template (no templateRef)");
                    newOid = null;
                } else {
                    newOid = templateRef.getOid();
                }
            }
        }

        LOGGER.trace("current focus template OID = {}, new = {}", currentOid, newOid);
        if (!java.util.Objects.equals(currentOid, newOid)) {
            ObjectTemplateType template;
            if (newOid != null) {
                template = cacheRepositoryService
                        .getObject(ObjectTemplateType.class, newOid, createReadOnlyCollection(), result)
                        .asObjectable();
            } else {
                template = null;
            }
            context.setFocusTemplate(template);
        }
    }

    /**
     * FIXME this method sometimes return repo-only shadow in the case of consistency mechanism is applied;
     *  see `TestConsistencyReaper.test150` and MID-7970.
     */
    public void loadFullShadow(@NotNull LensProjectionContext projCtx, String reason,
            Task task, OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        new ProjectionFullLoadOperation<>(projCtx.getLensContext(), projCtx, reason, false, task)
                .loadFullShadow(parentResult);
    }

    @SuppressWarnings("SameParameterValue")
    public void loadFullShadowNoDiscovery(@NotNull LensProjectionContext projCtx, String reason,
            Task task, OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        new ProjectionFullLoadOperation<>(projCtx.getLensContext(), projCtx, reason, true, task)
                .loadFullShadow(parentResult);
    }

    public <F extends FocusType> void reloadSecurityPolicyIfNeeded(@NotNull LensContext<F> context,
            @NotNull LensFocusContext<F> focusContext, Task task, OperationResult result)
            throws ExpressionEvaluationException, SchemaException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        if (focusContext.hasOrganizationalChange()) {
            loadSecurityPolicy(context, true, task, result);
        }
    }

    <F extends ObjectType> void loadSecurityPolicy(LensContext<F> context,
            Task task, OperationResult result) throws ExpressionEvaluationException,
            SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
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
        SecurityPolicyType focusSecurityPolicy = determineAndSetFocusSecurityPolicy(focusContext, focus, globalSecurityPolicy,
                forceReload, task, result);

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
            PrismObject<FocusType> focus, SecurityPolicyType globalSecurityPolicy, boolean forceReload, Task task,
            OperationResult result) throws SchemaException {
        SecurityPolicyType existingPolicy = focusContext.getSecurityPolicy();
        if (existingPolicy != null && !forceReload) {
            return existingPolicy;
        } else {
            SecurityPolicyType loadedPolicy = securityHelper.locateFocusSecurityPolicy(focus, task, result);
            SecurityPolicyType resultingPolicy;
            if (loadedPolicy != null) {
                resultingPolicy = loadedPolicy;
            } else {
                // Not very clean. In fact we should store focus security policy separate from global
                // policy to avoid confusion. But need to do this to fix MID-4793 and backport the fix.
                // Therefore avoiding big changes. TODO: fix properly later
                resultingPolicy = globalSecurityPolicy;
            }
            focusContext.setSecurityPolicy(resultingPolicy);
            return resultingPolicy;
        }
    }

}
