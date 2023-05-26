/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.impl.engine;

import java.util.List;
import java.util.Optional;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.casemgmt.api.CaseCreationListener;
import com.evolveum.midpoint.casemgmt.api.CaseEventDispatcher;
import com.evolveum.midpoint.cases.api.CaseEngine;
import com.evolveum.midpoint.cases.api.CaseEngineOperation;
import com.evolveum.midpoint.cases.api.extensions.EngineExtension;
import com.evolveum.midpoint.cases.api.request.OpenCaseRequest;
import com.evolveum.midpoint.cases.api.request.Request;
import com.evolveum.midpoint.cases.impl.engine.extension.DefaultEngineExtension;
import com.evolveum.midpoint.cases.impl.engine.helpers.TriggerHelper;
import com.evolveum.midpoint.cases.impl.engine.helpers.WorkItemHelper;
import com.evolveum.midpoint.cases.impl.helpers.AuthorizationHelper;
import com.evolveum.midpoint.cases.impl.helpers.CaseExpressionEvaluationHelper;
import com.evolveum.midpoint.cases.impl.helpers.CaseMiscHelper;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.common.archetypes.ArchetypeManager;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Manages lifecycle of case objects.
 *
 * There are multiple kinds of cases, for example:
 *
 * - approval cases,
 * - manual provisioning operation cases,
 * - correlation cases,
 * - ...and more are expected in the future.
 *
 * Currently, manual provisioning and correlation cases are managed without any pre-defined "process":
 * they just contain (usually single) work item, that is to be completed. It may be delegated, escalated,
 * claimed, released. But no process is there.
 *
 * For approval cases, this class serves as a replacement of `Activiti` software that was used from
 * the beginning of history of approvals in midPoint, but was removed in version 4.0. The lifecycle
 * of approval case objects is therefore no longer governed by BPMN processes, but by configuration
 * embodied in the case objects themselves.
 *
 * Specific responsibilities of this class (and related ones):
 *
 * 1. Managing the content of the case object, including e.g.
 *  - case state (created, open, closing, closed),
 *  - work items - creating, delegating/escalating, closing them,
 *  - case event history.
 *
 * 2. Auditing operations on the case object.
 * 3. Sending notifications about operations on the case object.
 *
 * TODO
 *  - stages
 *  - timed actions
 *
 * == Handling of concurrency issues
 *
 * Each request is carried out solely "in memory". Only when committing it the case object version is checked in repository.
 * If it was changed in the meanwhile the request is aborted and repeated.
 *
 * @see CaseEngineOperation
 * @see CaseEngineOperationImpl
 */
@Component
public class CaseEngineImpl implements CaseCreationListener, CaseEngine {

    private static final Trace LOGGER = TraceManager.getTrace(CaseEngineImpl.class);
    private static final String OP_EXECUTE_REQUEST = CaseEngineImpl.class.getName() + ".executeRequest";

    @Autowired public CaseBeans beans;
    @Autowired public Clock clock;
    @Autowired @Qualifier("cacheRepositoryService") public RepositoryService repositoryService;
    @Autowired public PrismContext prismContext;
    @Autowired public SecurityEnforcer securityEnforcer;
    @Autowired public CaseMiscHelper miscHelper;
    @Autowired public TriggerHelper triggerHelper;
    @Autowired public CaseExpressionEvaluationHelper expressionEvaluationHelper;
    @Autowired public WorkItemHelper workItemHelper;
    @Autowired public AuthorizationHelper authorizationHelper;
    @Autowired private CaseEventDispatcher caseEventDispatcher;
    @Autowired private ArchetypeManager archetypeManager;
    @Autowired private List<EngineExtension> engineExtensions;

    @PostConstruct
    public void init() {
        caseEventDispatcher.registerCaseCreationEventListener(this);
    }

    @PreDestroy
    public void destroy() {
        caseEventDispatcher.unregisterCaseCreationEventListener(this);
    }

    private static final int MAX_ATTEMPTS = 10;

    /**
     * Executes a request. This is the main entry point.
     */
    public void executeRequest(
            @NotNull Request request,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException,
            ExpressionEvaluationException, ConfigurationException, CommunicationException {
        int attempt = 1;
        for (;;) {
            OperationResult result = parentResult.subresult(OP_EXECUTE_REQUEST)
                    .setMinor()
                    .addParam("attempt", attempt)
                    .addArbitraryObjectAsParam("request", request)
                    .build();
            try {
                CaseEngineOperationImpl operation = createOperation(request.getCaseOid(), task, result);
                try {
                    operation.executeRequest(request, result);
                    return;
                } catch (PreconditionViolationException e) {
                    boolean repeat = attempt < MAX_ATTEMPTS;
                    String handling = repeat ? "retried" : "aborted";
                    LOGGER.info("Approval commit conflict detected; operation will be {} (this was attempt {} of {})",
                            handling, attempt, MAX_ATTEMPTS);
                    if (repeat) {
                        attempt++;
                    } else {
                        throw new SystemException(
                                "Couldn't execute " + request.getClass() + " in " + MAX_ATTEMPTS + " attempts", e);
                    }
                }
            } catch (Throwable t) {
                result.recordFatalError(t);
                throw t;
            } finally {
                result.close();
            }
        }
    }

    private CaseEngineOperationImpl createOperation(String caseOid, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException {
        CaseType aCase =
                repositoryService
                        .getObject(CaseType.class, caseOid, null, result)
                        .asObjectable();
        return new CaseEngineOperationImpl(
                aCase,
                determineEngineExtension(aCase, result),
                task,
                beans,
                SecurityUtil.getPrincipalRequired());
    }

    private @NotNull EngineExtension determineEngineExtension(@NotNull CaseType aCase, OperationResult result)
            throws SchemaException {
        String archetypeOid = getCaseArchetypeOid(aCase, result);
        LOGGER.trace("Going to determine engine extension for {}, archetype: {}", aCase, archetypeOid);
        if (archetypeOid != null) {
            Optional<EngineExtension> found = findRegisteredExtension(archetypeOid);
            LOGGER.trace("Information from the collection of registered extensions: {}", found);
            if (found.isPresent()) {
                return found.get();
            }
        }
        return new DefaultEngineExtension(beans);
    }

    private Optional<EngineExtension> findRegisteredExtension(@NotNull String archetypeOid) {
        return engineExtensions.stream()
                .filter(e -> e.getArchetypeOids().contains(archetypeOid))
                .findFirst();
    }

    private String getCaseArchetypeOid(@NotNull CaseType aCase, OperationResult result) throws SchemaException {
        ArchetypeType structuralArchetype = archetypeManager.determineStructuralArchetype(aCase, result);
        if (structuralArchetype != null) {
            LOGGER.trace("Structural archetype found: {}", structuralArchetype);
            return structuralArchetype.getOid();
        }

        // If there's exactly single archetypeRef value, but the archetype object does not exist
        // (e.g. in some tests), let's try that.
        List<ObjectReferenceType> archetypeRef = aCase.getArchetypeRef();
        if (archetypeRef.size() == 1) {
            String fromRef = archetypeRef.get(0).getOid();
            LOGGER.trace("Using an OID from (single) archetypeRef: {}", fromRef);
            return fromRef;
        }

        LOGGER.trace("No structural archetype could be determined (archetypeRef values: {})", archetypeRef.size());
        return null;
    }

    /**
     * Here we collect case creation events generated by lower layers, e.g. the built-in manual connector.
     *
     * It is in order to process them by e.g. sending the notifications or auditing the case creation.
     */
    @Override
    public void onCaseCreation(CaseType aCase, Task task, OperationResult result) {
        try {
            executeRequest(
                    new OpenCaseRequest(aCase.getOid()), task, result);
        } catch (CommonException e) {
            throw new SystemException("Couldn't open the case: " + aCase, e);
        }
    }
}
