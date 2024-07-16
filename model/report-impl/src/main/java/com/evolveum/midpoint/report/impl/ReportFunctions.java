/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl;

import static com.evolveum.midpoint.repo.common.expression.ExpressionEnvironmentThreadLocalHolder.getCurrentResult;
import static com.evolveum.midpoint.repo.common.expression.ExpressionEnvironmentThreadLocalHolder.getCurrentTask;
import static com.evolveum.midpoint.schema.util.CertCampaignTypeUtil.norm;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.CLOSED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_STATE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType.F_NAME;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.repo.common.ObjectOperationPolicyHelper;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.cases.api.AuditingConstants;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.ModelAuditService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.api.simulation.ProcessedObject;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
@SuppressWarnings({ "WeakerAccess", "unused" }) // methods here are called from scripts
public class ReportFunctions {

    private static final Trace LOGGER = TraceManager.getTrace(ReportFunctions.class);

    private final PrismContext prismContext;
    private final SchemaService schemaService;
    private final ModelService model;
    private final TaskManager taskManager;
    private final ModelAuditService modelAuditService;
    private final RepositoryService repositoryService;
    private final MidpointFunctions midpointFunctions;

    public ReportFunctions(PrismContext prismContext, SchemaService schemaService,
            ModelService modelService, TaskManager taskManager, ModelAuditService modelAuditService,
            @Qualifier("cacheRepositoryService") RepositoryService repositoryService, MidpointFunctions midpointFunctions) {
        this.prismContext = prismContext;
        this.schemaService = schemaService;
        this.model = modelService;
        this.taskManager = taskManager;
        this.modelAuditService = modelAuditService;
        this.repositoryService = repositoryService;
        this.midpointFunctions = midpointFunctions;
    }

    public <O extends ObjectType> O resolveObject(ObjectReferenceType ref) {
        Validate.notNull(ref.getOid(), "Object oid must not be null");
        Validate.notNull(ref.getType(), "Object type must not be null");

        Class<O> type = prismContext.getSchemaRegistry().determineCompileTimeClass(ref.getType());
        return resolveObject(type, ref.getOid());
    }

    public <O extends ObjectType> O resolveObject(Class<O> type, String oid) {
        Task task = taskManager.createTaskInstance();
        OperationResult parentResult = task.getResult();
        PrismObject<O> obj;
        try {
            obj = model.getObject(type, oid, SelectorOptions.createCollection(GetOperationOptions.createResolveNames()),
                    task, parentResult);
            return obj.asObjectable();
        } catch (CommonException e) {
            LOGGER.error("Could not get object with oid " + oid + ". Reason: " + e.getMessage());

        }
        return null;
    }

    public <O extends ObjectType> List<PrismObject<O>> resolveLinkRefs(Collection<ObjectReferenceType> refs, Class<O> type) {

        List<PrismObject<O>> objects = new ArrayList<>();

        if (refs == null) {
            return objects;
        }

        for (ObjectReferenceType ref : refs) {
            Class<O> clazz = getClassForType(ref.getType());
            if (!clazz.equals(type)) {
                continue;
            }
            Task task = taskManager.createTaskInstance();
            OperationResult parentResult = task.getResult();
            try {
                PrismObject<O> obj = model.getObject(
                        type,
                        ref.getOid(),
                        SelectorOptions.createCollection(GetOperationOptions.createResolveNames()),
                        task,
                        parentResult);
                objects.add(obj);
            } catch (CommonException e) {
                LOGGER.error("Could not get object with oid " + ref.getOid() + ". Reason: " + e.getMessage());

            }

        }
        return objects;
    }

    public String resolveRefName(ObjectReferenceType ref) {
        if (ref == null) {
            return null;
        }
        PrismReferenceValue refValue = ref.asReferenceValue();
        String name = refValue.getTargetName() != null ? ref.getTargetName().getOrig() : null;
        if (name == null) {
            LOGGER.error("Couldn't resolve object name");
        }
        return name;
    }

    public List<PrismObject<RoleType>> resolveRoles(Collection<AssignmentType> assignments) {
        return resolveAssignments(assignments, RoleType.class);
    }

    public List<PrismObject<RoleType>> resolveRoles(Collection<AssignmentType> assignments,
            Collection<String> filterOids) {

        Collection<AssignmentType> toResolve = assignments;
        if (CollectionUtils.isNotEmpty(filterOids) && CollectionUtils.isNotEmpty(assignments)) {
            toResolve = assignments.stream().filter(Objects::nonNull)
                    .filter(as -> as.getTargetRef() != null && as.getTargetRef().getOid() != null
                            && filterOids.contains(as.getTargetRef().getOid()))
                    // filter to default relation only - ignores approvers etc
                    .filter(as -> prismContext.isDefaultRelation(as.getTargetRef().getRelation()))
                    .collect(Collectors.toList());
        }

        return resolveRoles(toResolve);
    }

    public List<PrismObject<OrgType>> resolveOrgs(Collection<AssignmentType> assignments) {
        return resolveAssignments(assignments, OrgType.class);
    }

    public List<PrismObject<RoleType>> resolveRoles(AssignmentType assignments) {
        return resolveAssignments(assignments, RoleType.class);
    }

    public List<PrismObject<OrgType>> resolveOrgs(AssignmentType assignments) {
        return resolveAssignments(assignments, OrgType.class);
    }

    public <O extends ObjectType> List<PrismObject<O>> resolveAssignments(AssignmentType assignment, Class<O> type) {
        List<AssignmentType> assignments = new ArrayList<>();
        assignments.add(assignment);
        return resolveAssignments(assignments, type);
    }

    public <O extends ObjectType> List<PrismObject<O>> resolveAssignments(Collection<AssignmentType> assignments, Class<O> type) {
        List<PrismObject<O>> resolvedAssignments = new ArrayList<>();
        if (assignments == null) {
            return resolvedAssignments;
        }
        for (AssignmentType assignment : assignments) {
            Class<O> clazz = null;
            String oid = null;
            if (assignment.getTargetRef() != null) {
                clazz = getClassForType(assignment.getTargetRef().getType());
                oid = assignment.getTargetRef().getOid();
            } else if (assignment.getTenantRef() != null) {
                clazz = getClassForType(assignment.getTenantRef().getType());
                oid = assignment.getTenantRef().getOid();
            }

            if (clazz == null && assignment.getConstruction() != null) {
                continue;
            } else {
                LOGGER.debug("Could not resolve assignment for type {}. No target type defined.", type);
            }

            if (clazz == null || !clazz.equals(type)) {
                continue;
            }

            if (assignment.getTargetRef() != null && assignment.getTargetRef().asReferenceValue().getObject() != null) {
                resolvedAssignments.add(assignment.getTargetRef().asReferenceValue().getObject());
                continue;
            }

            Task task = taskManager.createTaskInstance();
            try {
                PrismObject<O> obj = model.getObject(type, oid, null, task, task.getResult());
                resolvedAssignments.add(obj);
            } catch (CommonException e) {
                LOGGER.error("Could not get object with oid " + oid + ". Reason: " + e.getMessage());

            }

        }

        return resolvedAssignments;
    }

    public List<AuditEventRecordType> searchAuditRecords(ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> option,
            Task task, OperationResult result) throws CommonException {
        return modelAuditService.searchObjects(query, option, task, result);
    }

    public List<AuditEventRecordType> searchAuditRecordsAsWorkflows(
            ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> option,
            Task task, OperationResult result) throws CommonException {
        return transformToWorkflows(searchAuditRecords(query, option, task, result));
    }

    private List<AuditEventRecordType> transformToWorkflows(List<AuditEventRecordType> auditEvents) {
        if (auditEvents == null || auditEvents.isEmpty()) {
            return auditEvents;
        }

        // group all records by property/wf.processInstanceId
        Map<String, List<AuditEventRecordType>> workflows = auditEvents.stream().collect(Collectors.groupingBy(event -> {
            List<String> processInstanceIds = new ArrayList<>();
            event.getProperty().stream()
                    .filter(property ->
                            AuditingConstants.AUDIT_PROCESS_INSTANCE_ID.equals(property.getName())
                                    || AuditingConstants.AUDIT_CASE_OID.equals(property.getName()))
                    .findFirst().ifPresent(property -> processInstanceIds.addAll(property.getValue()));

            Iterator<String> it = processInstanceIds.iterator();
            return it.hasNext() ? it.next() : "default workflow";
        }));

        // map of workflows in order of first request timestamp
        Map<Long, List<AuditEventRecordType>> workflowsFiltered = new TreeMap<>();
        workflows.forEach((key, wf) -> {
            // leave only the first request in each workflow
            List<AuditEventRecordType> filtered = new ArrayList<>();
            wf.stream().filter(record -> AuditEventStageType.REQUEST.equals(record.getEventStage())).findFirst()
                    .ifPresent(filtered::add);
            // and all executions with decision
            wf.stream().filter(record -> AuditEventStageType.EXECUTION.equals(record.getEventStage()))
                    .filter(record -> record.getMessage() == null || !record.getMessage().contains("(no decision)"))
                    .forEach(filtered::add);

            wf.stream().findFirst().ifPresent(record -> workflowsFiltered.put(
                    record.getTimestamp().toGregorianCalendar().getTime().getTime(), filtered));
        });

        return workflowsFiltered.values().stream().flatMap(List::stream)
                .collect(Collectors.toList());
    }

    public FocusType getShadowOwner(String shadowOid) {
        Task task = taskManager.createTaskInstance();
        try {
            PrismObject<? extends FocusType> owner = model.searchShadowOwner(shadowOid, null, task, task.getResult());
            return owner.asObjectable();
        } catch (CommonException e) {
            LOGGER.error("Could not find owner for shadow with oid " + shadowOid + ". Reason: " + e.getMessage());
        }

        return null;

    }

    private <O extends ObjectType> Class<O> getClassForType(QName type) {
        return prismContext.getSchemaRegistry().determineCompileTimeClass(type);
    }

    <T extends ObjectType> List<T> searchObjects(Class<T> type, ObjectQuery query) {
        List<T> ret = new ArrayList<>();
        Task task = taskManager.createTaskInstance();
        try {
            List<PrismObject<T>> list = model.searchObjects(type, query, null, task, task.getResult());
            for (PrismObject<T> po : list) {
                ret.add(po.asObjectable());
            }
        } catch (CommonException e) {
            LOGGER.error("Could not search objects of type: " + type + " with query " + query + ". Reason: " + e.getMessage());
        }
        return ret;
    }

    <C extends Containerable, T> ObjectFilter createEqualFilter(QName propertyName, Class<C> type, T realValue) {
        return prismContext.queryFor(type)
                .item(propertyName).eq(realValue)
                .buildFilter();
    }

    <C extends Containerable, T> ObjectFilter createEqualFilter(ItemPath propertyPath, Class<C> type, T realValue) {
        return prismContext.queryFor(type)
                .item(propertyPath).eq(realValue)
                .buildFilter();
    }

    public List<PrismContainerValue<CaseWorkItemType>> searchApprovalWorkItems()
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException, DatatypeConfigurationException {
        return searchApprovalWorkItems(0, null);
    }

    /*
     * @param days - return only workitems with createTimestamp older than (now-days), 0 to return all
     * @sortColumn - optionally AbstractWorkItemType QName to asc sort results (e.g. AbstractWorkItemType.F_CREATE_TIMESTAMP)
     */
    public List<PrismContainerValue<CaseWorkItemType>> searchApprovalWorkItems(int days, QName sortColumn)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException, DatatypeConfigurationException {
        Task task = taskManager.createTaskInstance();
        OperationResult result = task.getResult();
        ObjectQuery query = prismContext.queryFor(AbstractWorkItemType.class).build();

        if (days > 0) {
            XMLGregorianCalendar since = (new Clock()).currentTimeXMLGregorianCalendar();
            DatatypeFactory df = DatatypeFactory.newInstance();
            since.add(df.newDuration(false, 0, 0, days, 0, 0, 0));

            query.addFilter(prismContext.queryFor(AbstractWorkItemType.class)
                    .item(AbstractWorkItemType.F_CREATE_TIMESTAMP).lt(since).buildFilter());
        }

        if (sortColumn != null) {
            query.addFilter(prismContext.queryFor(AbstractWorkItemType.class)
                    .asc(sortColumn)
                    .buildFilter());
        }
        Object[] itemsToResolve = { CaseWorkItemType.F_ASSIGNEE_REF,
                ItemPath.create(PrismConstants.T_PARENT, CaseType.F_OBJECT_REF),
                ItemPath.create(PrismConstants.T_PARENT, CaseType.F_TARGET_REF),
                ItemPath.create(PrismConstants.T_PARENT, CaseType.F_REQUESTOR_REF) };
        SearchResultList<CaseWorkItemType> workItems = model.searchContainers(CaseWorkItemType.class, query,
                schemaService.getOperationOptionsBuilder().items(itemsToResolve).resolve().build(), task, result);
        return PrismContainerValue.toPcvList(workItems);
    }

    /**
     * Retrieves all definitions.
     * Augments them by count of campaigns (all + open ones).
     *
     * definitions and campaigns counts are expected to be low, so we can afford to go through all of them here
     */
    public Collection<PrismObject<AccessCertificationDefinitionForReportType>> searchCertificationDefinitions()
            throws ConfigurationException, SchemaException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException {

        Task task = taskManager.createTaskInstance();
        OperationResult result = task.getResult();
        Collection<SelectorOptions<GetOperationOptions>> options =
                SelectorOptions.createCollection(GetOperationOptions.createResolveNames());
        List<PrismObject<AccessCertificationDefinitionType>> definitions = model.searchObjects(
                AccessCertificationDefinitionType.class, null, options, task, result);
        final Map<String, PrismObject<AccessCertificationDefinitionForReportType>> definitionsForReportMap = new HashMap<>();
        for (PrismObject<AccessCertificationDefinitionType> definition : definitions) {
            // create subclass with the values copied from the superclass
            PrismObject<AccessCertificationDefinitionForReportType> definitionForReport = prismContext.createObjectable(
                    AccessCertificationDefinitionForReportType.class).asPrismObject();
            for (Item<?, ?> item : definition.getValue().getItems()) {
                definitionForReport.getValue().add(item.clone());
            }
            definitionsForReportMap.put(definition.getOid(), definitionForReport);
        }

        ResultHandler<AccessCertificationCampaignType> handler = (campaignObject, parentResult) -> {
            AccessCertificationCampaignType campaign = campaignObject.asObjectable();
            if (campaign.getDefinitionRef() != null) {
                String definitionOid = campaign.getDefinitionRef().getOid();
                PrismObject<AccessCertificationDefinitionForReportType> definitionObject =
                        definitionsForReportMap.get(definitionOid);
                if (definitionObject != null) {
                    AccessCertificationDefinitionForReportType definition = definitionObject.asObjectable();
                    int campaigns = definition.getCampaigns() != null ? definition.getCampaigns() : 0;
                    definition.setCampaigns(campaigns + 1);
                    AccessCertificationCampaignStateType state = campaign.getState();
                    if (state != AccessCertificationCampaignStateType.CREATED && state != CLOSED) {
                        int openCampaigns = definition.getOpenCampaigns() != null ? definition.getOpenCampaigns() : 0;
                        definition.setOpenCampaigns(openCampaigns + 1);
                    }
                }
            }
            return true;
        };
        model.searchObjectsIterative(AccessCertificationCampaignType.class, null, handler, null, task, result);

        List<PrismObject<AccessCertificationDefinitionForReportType>> rv = new ArrayList<>(definitionsForReportMap.values());
        rv.sort((o1, o2) -> {
            String n1 = o1.asObjectable().getName().getOrig();
            String n2 = o2.asObjectable().getName().getOrig();
            if (n1 == null) {
                n1 = "";
            }
            return n1.compareTo(n2);
        });
        for (PrismObject<AccessCertificationDefinitionForReportType> defObject : rv) {
            AccessCertificationDefinitionForReportType def = defObject.asObjectable();
            if (def.getCampaigns() == null) {
                def.setCampaigns(0);
            }
            if (def.getOpenCampaigns() == null) {
                def.setOpenCampaigns(0);
            }
        }
        return rv;
    }

    public List<PrismContainerValue<AccessCertificationCaseType>> getCertificationCampaignCases(String campaignName)
            throws CommonException {
        List<AccessCertificationCaseType> cases = getCertificationCampaignCasesAsBeans(campaignName);
        return PrismContainerValue.toPcvList(cases);
    }

    private List<AccessCertificationCaseType> getCertificationCampaignCasesAsBeans(String campaignName) throws CommonException {
        Task task = taskManager.createTaskInstance();
        ObjectQuery query;
        if (StringUtils.isEmpty(campaignName)) {
            //query = null;
            return new ArrayList<>();
        } else {
            query = prismContext.queryFor(AccessCertificationCaseType.class)
                    .item(PrismConstants.T_PARENT, F_NAME).eqPoly(campaignName, "").matchingOrig()
                    // TODO first by object/target type then by name (not supported by the repository as of now)
                    .asc(AccessCertificationCaseType.F_OBJECT_REF, PrismConstants.T_OBJECT_REFERENCE, ObjectType.F_NAME)
                    .asc(AccessCertificationCaseType.F_TARGET_REF, PrismConstants.T_OBJECT_REFERENCE, ObjectType.F_NAME)
                    .build();
        }
        Collection<SelectorOptions<GetOperationOptions>> options =
                SelectorOptions.createCollection(GetOperationOptions.createResolveNames());
        return model.searchContainers(AccessCertificationCaseType.class, query, options, task, task.getResult());
    }

    private List<AccessCertificationCaseType> getCertificationCampaignNotRespondedCasesAsBeans(String campaignName)
            throws CommonException {
        Task task = taskManager.createTaskInstance();
        ObjectQuery query;
        if (StringUtils.isEmpty(campaignName)) {
            //query = null;
            return new ArrayList<>();
        } else {
            query = prismContext.queryFor(AccessCertificationCaseType.class)
                    .item(PrismConstants.T_PARENT, F_NAME).eqPoly(campaignName, "")
                    .matchingOrig()
                    .and().item(AccessCertificationCaseType.F_CURRENT_STAGE_OUTCOME)
                    .eq(SchemaConstants.MODEL_CERTIFICATION_OUTCOME_NO_RESPONSE)
                    // TODO first by object/target type then by name (not supported by the repository as of now)
                    .asc(AccessCertificationCaseType.F_OBJECT_REF, PrismConstants.T_OBJECT_REFERENCE, ObjectType.F_NAME)
                    .asc(AccessCertificationCaseType.F_TARGET_REF, PrismConstants.T_OBJECT_REFERENCE, ObjectType.F_NAME)
                    .build();
        }
        Collection<SelectorOptions<GetOperationOptions>> options =
                SelectorOptions.createCollection(GetOperationOptions.createResolveNames());
        return model.searchContainers(AccessCertificationCaseType.class, query, options, task, task.getResult());
    }

    public List<PrismContainerValue<AccessCertificationWorkItemType>> getCertificationCampaignDecisions(
            String campaignName, Integer stageNumber)
            throws CommonException {
        return getCertificationCampaignDecisions(campaignName, stageNumber, null);
    }

    public List<PrismContainerValue<AccessCertificationWorkItemType>> getCertificationCampaignDecisions(
            String campaignName, Integer stageNumber, Integer iteration) throws CommonException {
        List<AccessCertificationCaseType> cases = getCertificationCampaignCasesAsBeans(campaignName);
        List<AccessCertificationWorkItemType> workItems = new ArrayList<>();
        for (AccessCertificationCaseType aCase : cases) {
            for (AccessCertificationWorkItemType workItem : aCase.getWorkItem()) {
                if (stageNumber != null && !Objects.equals(workItem.getStageNumber(), stageNumber)) {
                    continue;
                }
                if (iteration != null && norm(workItem.getIteration()) != iteration) {
                    continue;
                }
                workItems.add(workItem);
            }
        }
        return PrismContainerValue.toPcvList(workItems);
    }

    private AccessCertificationCampaignType getCampaignByName(String campaignName)
            throws CommonException {
        Task task = taskManager.createTaskInstance();
        if (StringUtils.isEmpty(campaignName)) {
            return null;
        }
        ObjectQuery query = prismContext.queryFor(AccessCertificationCampaignType.class)
                .item(AccessCertificationCampaignType.F_NAME).eqPoly(campaignName).matchingOrig()
                .build();
        List<PrismObject<AccessCertificationCampaignType>> objects = model
                .searchObjects(AccessCertificationCampaignType.class, query, null, task, task.getResult());
        if (objects.isEmpty()) {
            return null;
        } else if (objects.size() == 1) {
            return objects.get(0).asObjectable();
        } else {
            throw new IllegalStateException("More than one certification campaign found by name '" +
                    campaignName + "': " + objects);
        }
    }

    @SuppressWarnings("unused")
    public List<PrismContainerValue<AccessCertificationWorkItemType>> getCertificationCampaignNonResponders(
            String campaignName, Integer stageNumber) throws CommonException {
        List<AccessCertificationWorkItemType> workItems = new ArrayList<>();
        AccessCertificationCampaignType campaign = getCampaignByName(campaignName);
        if (campaign != null) {
            List<AccessCertificationCaseType> cases = getCertificationCampaignNotRespondedCasesAsBeans(campaignName);
            for (AccessCertificationCaseType aCase : cases) {
                for (AccessCertificationWorkItemType workItem : aCase.getWorkItem()) {
                    if (norm(workItem.getIteration()) == norm(campaign.getIteration())
                            && (workItem.getOutput() == null || workItem.getOutput().getOutcome() == null)
                            && (stageNumber == null || Objects.equals(workItem.getStageNumber(), stageNumber))) {
                        workItems.add(workItem);
                    }
                }
            }
        } else {
            LOGGER.debug("No campaign named '{}' was found", campaignName);
        }
        return PrismContainerValue.toPcvList(workItems);
    }

    public List<PrismObject<AccessCertificationCampaignType>> getCertificationCampaigns(Boolean alsoClosedCampaigns)
            throws CommonException {
        Task task = taskManager.createTaskInstance();

        ObjectQuery query = prismContext.queryFor(AccessCertificationCampaignType.class)
                .asc(F_NAME)
                .build();
        if (!Boolean.TRUE.equals(alsoClosedCampaigns)) {
            query.addFilter(
                    prismContext.queryFor(AccessCertificationCampaignType.class)
                            .not().item(F_STATE).eq(CLOSED)
                            .buildFilter()
            );
        }

        Collection<SelectorOptions<GetOperationOptions>> options = schemaService.getOperationOptionsBuilder()
                .root().resolveNames()
                .item(AccessCertificationCampaignType.F_CASE).retrieve()
                .build();
        return model.searchObjects(AccessCertificationCampaignType.class, query, options, task, task.getResult());
    }

    public @NotNull <O extends ObjectType> ProcessedObject<O> getProcessedObject(
            @NotNull SimulationResultProcessedObjectType objectBean) throws SchemaException {
        return model.parseProcessedObject(
                objectBean,
                Objects.requireNonNull(getCurrentTask(), "no current task"),
                Objects.requireNonNull(getCurrentResult(), "no current operation result"));
    }

    /** Returns the collection of (augmented) item deltas related to given "processed object" bean. */
    public Collection<ProcessedObject.ProcessedObjectItemDelta<?, ?>> getProcessedObjectItemDeltas(
            @NotNull SimulationResultProcessedObjectType objectBean,
            @Nullable Object pathsToInclude,
            @Nullable Object pathsToExclude,
            @Nullable Boolean includeOperationalItems) throws SchemaException {
        return getProcessedObject(objectBean)
                .getItemDeltas(pathsToInclude, pathsToExclude, includeOperationalItems);
    }

    /** Returns the collection of metrics related to given "processed object" bean. */
    public Collection<ProcessedObject.Metric> getProcessedObjectMetrics(
            @NotNull SimulationResultProcessedObjectType objectBean,
            @Nullable Boolean showEventMarks,
            @Nullable Boolean showExplicitMetrics) throws SchemaException {
        return getProcessedObject(objectBean)
                .getMetrics(showEventMarks, showExplicitMetrics);
    }

    /** Returns the object marks related to object represented by "processed object" bean. */
    public Collection<ObjectReferenceType> getObjectMarks(
            @NotNull SimulationResultProcessedObjectType objectBean) throws SchemaException {
        return getProcessedObject(objectBean)
                .getEffectiveObjectMarksRefs();
    }

    public AssignmentType getRelatedAssignment(ProcessedObject.ProcessedObjectItemDelta<?, ?> itemDelta) {
        return getRelatedAssignment(itemDelta, null);
    }

    public AssignmentType getRelatedAssignment(ProcessedObject.ProcessedObjectItemDelta<?, ?> itemDelta, Object value) {
        if (itemDelta != null) {
            AssignmentType fromDelta = itemDelta.getRelatedAssignment();
            if (fromDelta != null) {
                return fromDelta;
            }
        }
        if (value instanceof AssignmentType) {
            return (AssignmentType) value;
        }

        return null;
    }

    /**
     * Takes the role membership reference and returns the list of assignment path structures.
     *
     * @since 4.7
     */
    public List<AssignmentPathRowStructure> generateAssignmentPathRows(Referencable ref) throws CommonException {
        // ref is Referencable (can be ObjectReferenceType, but also DefaultReferencableImpl).
        PrismReferenceValue prismRefValue = ref.asReferenceValue();
        // If parent is available, it's better to use it, we'll save resolutions in the loop.
        PrismObject<?> parentPrismObject = PrismValueUtil.getParentObject(prismRefValue);
        Objectable parentObject = parentPrismObject != null ? parentPrismObject.getRealValue() : null;

        ValueMetadata valueMetadataPrism = prismRefValue.getValueMetadata();
        if (valueMetadataPrism.isEmpty()) {
            return List.of();
        }

        List<AssignmentPathRowStructure> list = new ArrayList<>();
        for (Containerable valueMetadataUntyped : valueMetadataPrism.getRealValues()) {
            ValueMetadataType valueMetadata = (ValueMetadataType) valueMetadataUntyped;
            ProvenanceMetadataType p = valueMetadata.getProvenance();
            if (p != null) {
                AssignmentPathMetadataType ap = p.getAssignmentPath();
                if (ap != null) {
                    AssignmentPathRowStructure row = generateAssignmentPathRow(ap, (AssignmentHolderType) parentObject);
                    // Some fields are more convenient to fill here:
                    PrismObject<Objectable> targetObject = prismRefValue.getObject();
                    AbstractRoleType role = targetObject != null // can be null, e.g deleted
                            ? targetObject.getRealValue(AbstractRoleType.class)
                            : null;
                    row.role = role;
                    row.roleArchetype = midpointFunctions.getStructuralArchetype(role);
                    StorageMetadataType storageMetadata = valueMetadata.getStorage();
                    row.createTimestamp = storageMetadata != null ? storageMetadata.getCreateTimestamp() : null;
                    row.valueMetadata = valueMetadata;
                    list.add(row);
                }
            }
        }
        return list;
    }

    private AssignmentPathRowStructure generateAssignmentPathRow(
            AssignmentPathMetadataType assignmentPath, AssignmentHolderType owner) throws CommonException {
        List<AssignmentPathSegmentMetadataType> segments = assignmentPath.getSegment();
        Long directAssignmentId = segments.get(0).getAssignmentId();
        ObjectReferenceType ownerRef = assignmentPath.getSourceRef();
        if (owner == null) {
            owner = (AssignmentHolderType) resolveWithRepositoryIfExists(ownerRef);
        }
        AssignmentType directAssignment = owner == null ? null
                : owner.getAssignment().stream()
                .filter(a -> Objects.equals(a.getId(), directAssignmentId))
                .findFirst()
                .orElse(null);

        List<ObjectType> segmentTargets = new ArrayList<>();
        for (AssignmentPathSegmentMetadataType segment : assignmentPath.getSegment()) {
            segmentTargets.add(resolveWithRepositoryIfExists(segment.getTargetRef()));
        }

        // This one should be available (resolved) by the code calling the expression.
        AssignmentPathRowStructure row = new AssignmentPathRowStructure();
        row.assignmentPath = assignmentPath;
        row.segmentTargets = segmentTargets;
        row.owner = owner;
        row.assignment = directAssignment;
        return row;
    }

    // Must be Serializable to be acceptable return value from the subreport.
    static class AssignmentPathRowStructure implements Serializable {
        public AssignmentPathMetadataType assignmentPath;
        public AssignmentHolderType owner;
        public List<ObjectType> segmentTargets;
        public AbstractRoleType role;
        public ArchetypeType roleArchetype;
        public XMLGregorianCalendar createTimestamp;
        public AssignmentType assignment;
        public ValueMetadataType valueMetadata; // for general purpose if fields above are not sufficient

        // Minimal toString for debugging, only owner+valueMetadata to identify the data.
        @Override
        public String toString() {
            return "AssignmentPathRowStructure{" +
                    "owner=" + owner +
                    ", valueMetadata=" + valueMetadata +
                    '}';
        }
    }

    /**
     * Low-level faster version of {@link MidpointFunctions#resolveReferenceIfExists(ObjectReferenceType)}.
     * This one does not use the model reference resolution and goes around the security checks.
     * Use with care, when SchemaTransformer cost is too much.
     */
    private @Nullable ObjectType resolveWithRepositoryIfExists(@Nullable ObjectReferenceType ref) throws CommonException {
        if (ref == null) {
            return null;
        }
        try {
            PrismObjectDefinition<? extends ObjectType> typeDef =
                    prismContext.getSchemaRegistry().findObjectDefinitionByType(ref.getType());
            Class<? extends ObjectType> compileTimeClass = typeDef.getCompileTimeClass();
            return repositoryService.getObject(compileTimeClass, ref.getOid(),
                            SelectorOptions.createCollection(GetOperationOptions.createAllowNotFound()),
                            getCurrentResult())
                    .asObjectable();
        } catch (ObjectNotFoundException ignored) {
            return null;
        }
    }
}
