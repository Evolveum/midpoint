/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRefWithFullObject;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.util.logging.LoggingUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.casemgmt.api.CaseEventDispatcher;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.correlator.CorrelationService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.builder.S_ItemEntry;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
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
    @Autowired private ProvisioningService provisioningService;
    @Autowired private CaseEventDispatcher caseEventDispatcher;

    /**
     * Creates or updates a correlation case for given correlation operation that finished in "uncertain" state.
     *
     * @param resourceObject Shadowed resource object we correlate. Must have an OID.
     */
    public void createOrUpdateCase(
            @NotNull ShadowType resourceObject,
            @NotNull FocusType preFocus,
            @NotNull PotentialOwnersType correlatorSpecificContext,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException {
        checkOid(resourceObject);
        CaseType aCase = findCorrelationCase(resourceObject, true, result);
        if (aCase == null) {
            createCase(resourceObject, preFocus, correlatorSpecificContext, task, result);
        } else {
            updateCase(aCase, preFocus, correlatorSpecificContext, result);
        }
    }

    private void checkOid(@NotNull ShadowType resourceObject) {
        argCheck(resourceObject.getOid() != null, "OID-less resource object %s", resourceObject);
    }

    private void createCase(
            ShadowType resourceObject,
            FocusType preFocus,
            PotentialOwnersType potentialOwners,
            Task task,
            OperationResult result)
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
                .state(SchemaConstants.CASE_STATE_CREATED)
                .workItem(new CaseWorkItemType(PrismContext.get())
                        .name(getWorkItemName(resourceObject))
                        .assigneeRef(assigneeOid, UserType.COMPLEX_TYPE));
        try {
            repositoryService.addObject(newCase.asPrismObject(), null, result);
        } catch (ObjectAlreadyExistsException e) {
            throw new SystemException("Unexpected exception: " + e.getMessage(), e);
        }

        caseEventDispatcher.dispatchCaseCreationEvent(newCase, task, result);
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
     * Preconditions:
     *
     * - case is freshly fetched,
     * - case is a correlation one
     *
     * Returns true if the case can be closed.
     */
    public boolean completeCorrelationCase(
            @NotNull CaseType aCase,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {

        String outcomeUri = aCase.getOutcome();
        if (outcomeUri == null) {
            LOGGER.warn("Correlation case {} has no outcome", aCase);
            return false;
        }

        OperationResult subResult = performCompletionInCorrelator(aCase, outcomeUri, task, result);
        if (!subResult.isSuccess()) {
            LOGGER.warn("Not closing the case {} because completion in correlator was not successful", aCase);
            return false;
        }

        String shadowOid = aCase.getObjectRef().getOid();
        try {
            // We try to re-import the object. This should be made configurable.
            modelService.importFromResource(shadowOid, task, result);
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't import shadow {} from resource", e, shadowOid);
            // Not throwing the exception higher (import itself is not prerequisite for completing the correlation case
        }

        return true;
    }

    private OperationResult performCompletionInCorrelator(
            @NotNull CaseType aCase, @NotNull String outcomeUri, Task task, OperationResult parentResult)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {
        OperationResult result = parentResult.createSubresult(OP_PERFORM_COMPLETION_IN_CORRELATOR);
        try {
            applyShadowDefinition(aCase, task, result);
            correlationService
                    .instantiateCorrelator(aCase.asPrismObject(), task, result)
                    .resolve(aCase, outcomeUri, task, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
        return result;
    }

    /**
     * Applies the correct definition to the shadow embedded in the case.objectRef.
     */
    private void applyShadowDefinition(CaseType aCase, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
            ObjectNotFoundException {
        ShadowType shadow =
                MiscUtil.requireNonNull(
                        MiscUtil.castSafely(
                                ObjectTypeUtil.getObjectFromReference(aCase.getObjectRef()),
                                ShadowType.class),
                        () -> "No embedded shadow in " + aCase);
        provisioningService.applyDefinition(shadow.asPrismObject(), task, result);
    }
}
