/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRefWithFullObject;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.correlator.CorrelationService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.builder.S_ItemEntry;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Manages correlation cases.
 */
@Component
public class CorrelationCaseManager {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationCaseManager.class);

    private static final String OP_PERFORM_COMPLETION_IN_CORRELATOR =
            CorrelationCaseManager.class.getName() + ".performCompletionInCorrelator";

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired private ModelService modelService;
    @Autowired private PrismContext prismContext;
    @Autowired private Clock clock;
    @Autowired private CorrelationService correlationService;

    /**
     * Creates or updates a correlation case for given correlation operation that finished in "uncertain" state.
     *
     * @param resourceObject Shadowed resource object we correlate. Must have an OID.
     */
    public void createOrUpdateCase(
            @NotNull ShadowType resourceObject,
            @NotNull FocusType preFocus,
            @NotNull PotentialOwnersType correlatorSpecificContext,
            @NotNull OperationResult result)
            throws SchemaException {
        checkOid(resourceObject);
        CaseType aCase = findCorrelationCase(resourceObject, true, result);
        if (aCase == null) {
            createCase(resourceObject, preFocus, correlatorSpecificContext, result);
        } else {
            updateCase(aCase, preFocus, correlatorSpecificContext, result);
        }
    }

    private void checkOid(@NotNull ShadowType resourceObject) {
        argCheck(resourceObject.getOid() != null, "OID-less resource object %s", resourceObject);
    }

    private void createCase(
            ShadowType resourceObject, FocusType preFocus, PotentialOwnersType potentialOwners, OperationResult result)
            throws SchemaException {
        String assigneeOid = SystemObjectsType.USER_ADMINISTRATOR.value(); // temporary
        CaseType newCase = new CaseType(prismContext)
                .name(getCaseName(resourceObject))
                .objectRef(createObjectRefWithFullObject(resourceObject))
                .archetypeRef(createArchetypeRef())
                .assignment(new AssignmentType(prismContext)
                        .targetRef(createArchetypeRef()))
                .correlationContext(new CorrelationContextType(prismContext)
                        .preFocusRef(ObjectTypeUtil.createObjectRefWithFullObject(preFocus))
                        .potentialOwners(potentialOwners))
                .state(SchemaConstants.CASE_STATE_OPEN)
                .workItem(new CaseWorkItemType(PrismContext.get())
                        .name(getWorkItemName(resourceObject))
                        .assigneeRef(assigneeOid, UserType.COMPLEX_TYPE));
        try {
            repositoryService.addObject(newCase.asPrismObject(), null, result);
        } catch (ObjectAlreadyExistsException e) {
            throw new SystemException("Unexpected exception: " + e.getMessage(), e);
        }
    }

    // Temporary implementation
    private String getCaseName(ShadowType resourceObject) {
        return "Correlation of " + getKindLabel(resourceObject) + " '" + resourceObject.getName() + "'";
    }

    // Temporary implementation
    private String getKindLabel(ShadowType resourceObject) {
        ShadowKindType kind = resourceObject.getKind();
        if (kind == null) {
            return "object";
        } else switch (kind) {
            case ACCOUNT:
                return "account";
            case ENTITLEMENT:
                return "entitlement";
            case GENERIC:
            default:
                return "object";
        }
    }

    private String getWorkItemName(ShadowType resourceObject) {
        return "Correlate " + getKindLabel(resourceObject) + " '" + resourceObject.getName() + "'";
    }

    private ObjectReferenceType createArchetypeRef() {
        return new ObjectReferenceType()
                .oid(SystemObjectsType.ARCHETYPE_CORRELATION_CASE.value())
                .type(ArchetypeType.COMPLEX_TYPE);
    }

    private void updateCase(
            CaseType aCase, FocusType preFocus, PotentialOwnersType correlatorSpecificContext, OperationResult result)
            throws SchemaException {
        CorrelationContextType ctx = aCase.getCorrelationContext();
        ObjectReferenceType preFocusRef = createObjectRefWithFullObject(preFocus);
        if (ctx != null
                && java.util.Objects.equals(ctx.getPotentialOwners(), correlatorSpecificContext)
                && java.util.Objects.equals(ctx.getPreFocusRef(), preFocusRef)) { // TODO is this comparison correct?
            LOGGER.trace("No need to update the case {}", aCase);
            return;
        }
        List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(CaseType.class)
                .item(CaseType.F_CORRELATION_CONTEXT, CorrelationContextType.F_POTENTIAL_OWNERS)
                .replace(correlatorSpecificContext)
                .item(CaseType.F_CORRELATION_CONTEXT, CorrelationContextType.F_PRE_FOCUS_REF)
                .replace(preFocusRef)
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

    // TODO fix this method
    public @Nullable CaseType findCorrelationCase(ShadowType resourceObject, boolean mustBeOpen, OperationResult result)
            throws SchemaException {
        checkOid(resourceObject);
        LOGGER.trace("Looking for correlation case for {}", resourceObject);
        S_AtomicFilterExit q = prismContext.queryFor(CaseType.class)
                .item(CaseType.F_OBJECT_REF).ref(resourceObject.getOid())
                .and().item(CaseType.F_ARCHETYPE_REF).ref(SystemObjectsType.ARCHETYPE_CORRELATION_CASE.value());
        if (mustBeOpen) {
            q = q.and().item(CaseType.F_STATE).eq(SchemaConstants.CASE_STATE_OPEN); // what about namespaces?
        }
        ObjectQuery query = q.build();
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
        CaseType aCase = findCorrelationCase(resourceObject, true, result);
        if (aCase != null) {
            LOGGER.debug("Marking correlation case as closed: {}", aCase);
            S_ItemEntry builder = prismContext.deltaFor(CaseType.class)
                    .item(CaseType.F_STATE)
                    .replace(SchemaConstants.CASE_STATE_CLOSED);
            XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
            for (CaseWorkItemType workItem : aCase.getWorkItem()) {
                if (workItem.getCloseTimestamp() == null) {
                    builder = builder
                            .item(CaseType.F_WORK_ITEM, workItem.getId(), CaseWorkItemType.F_CLOSE_TIMESTAMP)
                            .replace(now);
                }
            }
            modifyCase(aCase, builder.asItemDeltas(), result);
        }
    }

    /**
     * Temporary implementation. We should merge this code with CompleteWorkItemsAction (in workflow-impl module).
     *
     * Preconditions:
     *
     * - case is freshly fetched,
     * - case is a correlation one
     */
    public void completeWorkItem(
            @NotNull WorkItemId workItemId,
            @NotNull PrismObject<CaseType> aCase,
            @NotNull AbstractWorkItemOutputType output,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SecurityViolationException, SchemaException, ObjectNotFoundException,
            ConfigurationException, ExpressionEvaluationException, CommunicationException {

        // TODO authorization
        // TODO already-completed check

        OperationResult subResult = performCompletionInCorrelator(aCase, output, task, result);
        if (!subResult.isSuccess()) {
            LOGGER.trace("Not closing the work item and the case because completion in correlator was not successful");
            return;
        }

        // Preliminary code
        ObjectReferenceType performerRef = createObjectRef(SecurityUtil.getPrincipalRequired().getFocus());
        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();

        List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(CaseType.class)
                .item(CaseType.F_WORK_ITEM, workItemId.id, CaseWorkItemType.F_OUTPUT).replace(output.clone())
                .item(CaseType.F_WORK_ITEM, workItemId.id, CaseWorkItemType.F_PERFORMER_REF).replace(performerRef)
                .item(CaseType.F_WORK_ITEM, workItemId.id, CaseWorkItemType.F_CLOSE_TIMESTAMP).replace(now)
                .item(CaseType.F_STATE).replace(SchemaConstants.CASE_STATE_CLOSED)
                .asItemDeltas();

        try {
            repositoryService.modifyObject(CaseType.class, workItemId.caseOid, itemDeltas, result);
        } catch (ObjectAlreadyExistsException e) {
            throw new SystemException("Unexpected exception: " + e.getMessage(), e);
        }

        // We try to re-import the object. This should be made configurable.
        String shadowOid = aCase.asObjectable().getObjectRef().getOid();
        modelService.importFromResource(shadowOid, task, result);
    }

    private OperationResult performCompletionInCorrelator(
            PrismObject<CaseType> aCase, AbstractWorkItemOutputType output, Task task, OperationResult parentResult)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {
        OperationResult result = parentResult.createSubresult(OP_PERFORM_COMPLETION_IN_CORRELATOR);
        try {
            correlationService
                    .instantiateCorrelator(aCase, task, result)
                    .resolve(aCase, output, task, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
        return result;
    }
}
