/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import java.util.List;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Manages correlation cases.
 */
@Component
public class CorrelationCaseManager {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationCaseManager.class);

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired private PrismContext prismContext;

    /**
     * Creates or updates a correlation case for given correlation operation that finished in "uncertain" state.
     *
     * @param resourceObject Shadowed resource object we correlate. Must have an OID.
     */
    public void createOrUpdateCase(
            @NotNull ShadowType resourceObject,
            @NotNull AbstractCorrelationContextType correlatorSpecificContext,
            @NotNull OperationResult result)
            throws SchemaException {
        checkOid(resourceObject);
        CaseType aCase = findCorrelationCase(resourceObject, result);
        if (aCase == null) {
            createCase(resourceObject, correlatorSpecificContext, result);
        } else {
            updateCase(aCase, correlatorSpecificContext, result);
        }
    }

    private void checkOid(@NotNull ShadowType resourceObject) {
        argCheck(resourceObject.getOid() != null, "OID-less resource object %s", resourceObject);
    }

    private void createCase(
            ShadowType resourceObject, AbstractCorrelationContextType correlatorSpecificContext, OperationResult result)
            throws SchemaException {
        String assigneeOid = SystemObjectsType.USER_ADMINISTRATOR.value(); // temporary
        CaseType newCase = new CaseType(prismContext)
                .name("Correlate " + resourceObject.getName())
                .objectRef(ObjectTypeUtil.createObjectRef(resourceObject))
                .archetypeRef(createArchetypeRef())
                .assignment(new AssignmentType(prismContext)
                        .targetRef(createArchetypeRef()))
                .correlationContext(new CorrelationContextType(prismContext)
                        .correlatorPart(correlatorSpecificContext))
                .state(SchemaConstants.CASE_STATE_OPEN)
                .workItem(new CaseWorkItemType(PrismContext.get())
                        .assigneeRef(assigneeOid, UserType.COMPLEX_TYPE));
        try {
            repositoryService.addObject(newCase.asPrismObject(), null, result);
        } catch (ObjectAlreadyExistsException e) {
            throw new SystemException("Unexpected exception: " + e.getMessage(), e);
        }
    }

    private ObjectReferenceType createArchetypeRef() {
        return new ObjectReferenceType()
                .oid(SystemObjectsType.ARCHETYPE_CORRELATION_CASE.value())
                .type(ArchetypeType.COMPLEX_TYPE);
    }

    private void updateCase(CaseType aCase, AbstractCorrelationContextType correlatorSpecificContext, OperationResult result)
            throws SchemaException {
        if (aCase.getCorrelationContext() != null &&
                java.util.Objects.equals(aCase.getCorrelationContext().getCorrelatorPart(), correlatorSpecificContext)) {
            LOGGER.trace("No need to update the case {}", aCase);
            return;
        }
        List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(CaseType.class)
                .item(CaseType.F_CORRELATION_CONTEXT, CorrelationContextType.F_CORRELATOR_PART)
                .replace(correlatorSpecificContext)
                .asItemDeltas();
        modifyCase(aCase, itemDeltas, result);
    }

    private void modifyCase(CaseType aCase, List<ItemDelta<?, ?>> itemDeltas, OperationResult result) throws SchemaException {
        try {
            repositoryService.modifyObject(CaseType.class, aCase.getOid(), itemDeltas, result);
        } catch (ObjectNotFoundException e) {
            throw new SystemException("Unexpected exception (maybe the case was deleted in the meanwhile): " + e.getMessage(), e);
        } catch (ObjectAlreadyExistsException e) {
            throw new SystemException("Unexpected exception: " + e.getMessage(), e);
        }
    }

    public @Nullable CaseType findCorrelationCase(ShadowType resourceObject, OperationResult result) throws SchemaException {
        checkOid(resourceObject);
        LOGGER.trace("Looking for correlation case for {}", resourceObject);
        ObjectQuery query = prismContext.queryFor(CaseType.class)
                .item(CaseType.F_OBJECT_REF).ref(resourceObject.getOid())
                .and().item(CaseType.F_ARCHETYPE_REF).ref(SystemObjectsType.ARCHETYPE_CORRELATION_CASE.value())
                .build();
        List<PrismObject<CaseType>> cases = repositoryService.searchObjects(CaseType.class, query, null, result);
        LOGGER.trace("Found cases:\n{}", DebugUtil.debugDumpLazily(cases));
        if (cases.size() > 1) {
            PrismObject<CaseType> toKeep = cases.get(0);
            LOGGER.warn("Multiple correlation cases for {}. This could be a result of a race condition. "
                    + "Keeping only a single one: {}", resourceObject, toKeep);
            throw new UnsupportedOperationException("TODO implement deletion of extra cases - or not?");
        } else if (cases.size() == 1) {
            return cases.get(0).asObjectable();
        } else {
            return null;
        }
    }

    /**
     * Closes a correlation case - if there's any - if it's no longer needed (e.g. because the uncertainty is gone).
     *
     * @param resourceObject Shadowed resource object we correlate. Must have an OID.
     */
    public void closeCaseIfExists(
            @NotNull ShadowType resourceObject,
            @NotNull OperationResult result) throws SchemaException {
        checkOid(resourceObject);
        CaseType aCase = findCorrelationCase(resourceObject, result);
        if (aCase != null) {
            LOGGER.debug("Marking correlation case as closed: {}", aCase);
            List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(CaseType.class)
                    .item(CaseType.F_STATE)
                    .replace(SchemaConstants.CASE_STATE_CLOSED)
                    .asItemDeltas();
            modifyCase(aCase, itemDeltas, result);
        }
    }
}
