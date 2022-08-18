/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlation;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRefWithFullObject;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.cases.api.CaseEngine;
import com.evolveum.midpoint.cases.api.CaseManager;
import com.evolveum.midpoint.cases.api.util.PerformerCommentsFormatter;
import com.evolveum.midpoint.model.api.correlator.Correlator;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.util.cases.CorrelationCaseUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.casemgmt.api.CaseEventDispatcher;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.correlation.CorrelationService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.builder.S_ItemEntry;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
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
 *
 * TODO difference to {@link CaseManager} / {@link CaseEngine} ?
 */
@Component
public class CorrelationCaseManager {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationCaseManager.class);

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired private ModelService modelService;
    @Autowired private PrismContext prismContext;
    @Autowired private Clock clock;
    @Autowired private CorrelationServiceImpl correlationService;
    @Autowired private CaseEventDispatcher caseEventDispatcher;
    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired(required = false) private CaseManager caseManager;

    /**
     * Creates or updates a correlation case for given correlation operation that finished in "uncertain" state.
     *
     * @param resourceObject Shadowed resource object we are correlating. Must have an OID.
     * @param preFocus The result of pre-inbounds application on the resource object.
     */
    public void createOrUpdateCase(
            @NotNull ShadowType resourceObject,
            @NotNull ResourceType resource,
            @NotNull FocusType preFocus,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        checkOid(resourceObject);
        CaseType aCase = findCorrelationCase(resourceObject, true, result);
        if (aCase == null) {
            XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
            createCase(resourceObject, resource, preFocus, now, task, result);
            recordCaseCreationInShadow(resourceObject, now, result);
        } else {
            updateCase(aCase, preFocus, result);
        }
    }

    private void checkOid(@NotNull ShadowType resourceObject) {
        argCheck(resourceObject.getOid() != null, "OID-less resource object %s", resourceObject);
    }

    private void createCase(
            ShadowType resourceObject,
            ResourceType resource,
            FocusType preFocus,
            XMLGregorianCalendar now,
            Task task,
            OperationResult result)
            throws SchemaException {
        CaseType newCase = new CaseType()
                .name(getCaseName(resourceObject, resource))
                .objectRef(resourceObject.getResourceRef().clone())
                .targetRef(createObjectRefWithFullObject(resourceObject))
                .requestorRef(task != null ? task.getOwnerRef() : null)
                .archetypeRef(createArchetypeRef())
                .assignment(new AssignmentType()
                        .targetRef(createArchetypeRef()))
                .correlationContext(new CaseCorrelationContextType()
                        .preFocusRef(ObjectTypeUtil.createObjectRefWithFullObject(preFocus))
                        .schema(createCaseSchema(resource.getBusiness())))
                .state(SchemaConstants.CASE_STATE_CREATED)
                .metadata(new MetadataType()
                    .createTimestamp(now));
        try {
            repositoryService.addObject(newCase.asPrismObject(), null, result);
        } catch (ObjectAlreadyExistsException e) {
            throw new SystemException("Unexpected exception: " + e.getMessage(), e);
        }

        caseEventDispatcher.dispatchCaseCreationEvent(newCase, task, result);
    }

    private SimpleCaseSchemaType createCaseSchema(@Nullable ResourceBusinessConfigurationType business) {
        if (business == null) {
            return null;
        }
        SimpleCaseSchemaType schema = new SimpleCaseSchemaType();
        schema.getAssigneeRef().addAll(
                CloneUtil.cloneCollectionMembers(business.getCorrelatorRef()));
        schema.setDuration(business.getCorrelatorActionMaxDuration());
        return schema;
    }

    // Temporary implementation
    private String getCaseName(ShadowType resourceObject, ResourceType resource) {
        return "Correlation of " + getKindLabel(resourceObject)
                + " '" + resourceObject.getName() + "'"
                + " on " + resource.getName().getOrig();
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

    private ObjectReferenceType createArchetypeRef() {
        return new ObjectReferenceType()
                .oid(SystemObjectsType.ARCHETYPE_CORRELATION_CASE.value())
                .type(ArchetypeType.COMPLEX_TYPE);
    }

    private void recordCaseCreationInShadow(ShadowType shadow, XMLGregorianCalendar now, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        repositoryService.modifyObject(
                ShadowType.class,
                shadow.getOid(),
                PrismContext.get().deltaFor(ShadowType.class)
                        .item(ShadowType.F_CORRELATION, ShadowCorrelationStateType.F_CORRELATION_CASE_OPEN_TIMESTAMP)
                        .replace(now)
                        .asItemDeltas(),
                result);
    }

    private void updateCase(
            CaseType aCase, FocusType preFocus, OperationResult result)
            throws SchemaException {
        CaseCorrelationContextType ctx = aCase.getCorrelationContext();
        ObjectReferenceType preFocusRef = createObjectRefWithFullObject(preFocus);
        if (ctx != null
                && java.util.Objects.equals(ctx.getPreFocusRef(), preFocusRef)) { // TODO is this comparison correct?
            LOGGER.trace("No need to update the case {}", aCase);
            return;
        }
        List<ItemDelta<?, ?>> itemDeltas = prismContext.deltaFor(CaseType.class)
                .item(CaseType.F_CORRELATION_CONTEXT, CaseCorrelationContextType.F_PRE_FOCUS_REF)
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
        S_FilterExit q = prismContext.queryFor(CaseType.class)
                .item(CaseType.F_TARGET_REF).ref(resourceObject.getOid())
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
            return cases.get(0).asObjectable(); // FIXME
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
     *
     * TODO don't look for cases if not necessary (timestamps?)
     */
    public void closeCaseIfStillOpen(
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
     */
    void completeCorrelationCase(
            @NotNull CaseType aCase,
            @NotNull CorrelationService.CaseCloser caseCloser,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {

        String outcomeUri = aCase.getOutcome();
        if (outcomeUri == null) {
            LOGGER.warn("Correlation case {} has no outcome", aCase);
            return;
        }

        recordCaseCompletionInShadow(aCase, task, result);
        caseCloser.closeCaseInRepository(result);
        correlationService.resolve(aCase, task, result);

        // As a convenience, we try to re-import the object. Technically this is not a part of the correlation case processing.
        // Whether we do this should be made configurable (in the future).

        String shadowOid = CorrelationCaseUtil.getShadowOidRequired(aCase);
        try {
            modelService.importFromResource(shadowOid, task, result);
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't import shadow {} from resource", e, shadowOid);
            // Not throwing the exception higher (import itself is not prerequisite for completing the correlation case
        }
    }

    private void recordCaseCompletionInShadow(CaseType aCase, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException {

        String shadowOid = CorrelationCaseUtil.getShadowOidRequired(aCase);
        XMLGregorianCalendar now = XmlTypeConverter.createXMLGregorianCalendar();
        ObjectReferenceType resultingOwnerRef =
                CloneUtil.clone(
                        CorrelationCaseUtil.getResultingOwnerRef(aCase));
        CorrelationSituationType situation = resultingOwnerRef != null ?
                CorrelationSituationType.EXISTING_OWNER : CorrelationSituationType.NO_OWNER;

        try {
            repositoryService.modifyObject(
                    ShadowType.class,
                    shadowOid,
                    prismContext.deltaFor(ShadowType.class)
                            .item(ShadowType.F_CORRELATION, ShadowCorrelationStateType.F_CORRELATION_CASE_CLOSE_TIMESTAMP)
                            .replace(now)
                            .item(ShadowType.F_CORRELATION, ShadowCorrelationStateType.F_CORRELATION_END_TIMESTAMP)
                            .replace(now)
                            .item(ShadowType.F_CORRELATION, ShadowCorrelationStateType.F_PERFORMER_REF)
                            .replaceRealValues(getPerformerRefs(aCase))
                            .item(ShadowType.F_CORRELATION, ShadowCorrelationStateType.F_PERFORMER_COMMENT)
                            .replaceRealValues(getPerformerComments(aCase, task, result))
                            .item(ShadowType.F_CORRELATION, ShadowCorrelationStateType.F_OWNER_OPTIONS)
                            .replace() // This might be reconsidered
                            .item(ShadowType.F_CORRELATION, ShadowCorrelationStateType.F_RESULTING_OWNER)
                            .replace(resultingOwnerRef)
                            .item(ShadowType.F_CORRELATION, ShadowCorrelationStateType.F_SITUATION)
                            .replace(situation)
                            .asItemDeltas(),
                    result);
        } catch (SchemaException | ObjectAlreadyExistsException e) {
            throw SystemException.unexpected(e, "while recording case completion in shadow");
        }
    }

    /** We select only those performers that provided the same answer as the final one. */
    private Collection<ObjectReferenceType> getPerformerRefs(CaseType aCase) {
        List<ObjectReferenceType> rv = new ArrayList<>();
        for (CaseWorkItemType workItem : aCase.getWorkItem()) {
            if (isRelevant(workItem, aCase)) {
                rv.add(workItem.getPerformerRef().clone());
            }
        }
        LOGGER.trace("Performers: {}", rv);
        return rv;
    }

    /** We select only those performers that provided the same answer as the final one. */
    private Collection<String> getPerformerComments(CaseType aCase, Task task, OperationResult result) throws SchemaException {
        // Currently we take comments formatting from workflow configuration.
        PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(result);
        PerformerCommentsFormattingType formatting = systemConfiguration != null &&
                systemConfiguration.asObjectable().getWorkflowConfiguration() != null ?
                systemConfiguration.asObjectable().getWorkflowConfiguration().getApproverCommentsFormatting() : null;

        PerformerCommentsFormatter formatter =
                caseManager != null ?
                        caseManager.createPerformerCommentsFormatter(formatting) : PerformerCommentsFormatter.EMPTY;

        List<String> rv = new ArrayList<>();
        for (CaseWorkItemType workItem : aCase.getWorkItem()) {
            if (isRelevant(workItem, aCase) && StringUtils.isNotBlank(workItem.getOutput().getComment())) {
                CollectionUtils.addIgnoreNull(rv, formatter.formatComment(workItem, task, result));
            }
        }
        LOGGER.trace("Performer comments: {}", rv);
        return rv;
    }

    private boolean isRelevant(CaseWorkItemType workItem, CaseType aCase) {
        return hasOutcomeUri(workItem, aCase.getOutcome()) && workItem.getPerformerRef() != null;
    }

    private boolean hasOutcomeUri(@NotNull CaseWorkItemType workItem, @NotNull String outcomeUri) {
        return workItem.getOutput() != null
                && outcomeUri.equals(workItem.getOutput().getOutcome());
    }
}
