/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.report.impl;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkflowConstants;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.schema.util.CertCampaignTypeUtil.norm;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.CLOSED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_STATE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType.F_NAME;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

public class ReportFunctions {

    private static final Trace LOGGER = TraceManager.getTrace(ReportFunctions.class);

    private final PrismContext prismContext;
    private final SchemaHelper schemaHelper;
    private final ModelService model;
    private final TaskManager taskManager;
    private final AuditService auditService;

    public ReportFunctions(PrismContext prismContext, SchemaHelper schemaHelper,
		    ModelService modelService, TaskManager taskManager, AuditService auditService) {
        this.prismContext = prismContext;
        this.schemaHelper = schemaHelper;
        this.model = modelService;
        this.taskManager = taskManager;
        this.auditService = auditService;
    }

    public <O extends ObjectType> O resolveObject(ObjectReferenceType ref) {
        Validate.notNull(ref.getOid(), "Object oid must not be null");
        Validate.notNull(ref.getType(), "Object type must not be null");

        Class type = prismContext.getSchemaRegistry().determineCompileTimeClass(ref.getType());
        return resolveObject(type, ref.getOid());
    }

    public <O extends ObjectType> O resolveObject(Class type, String oid) {
        Task task = taskManager.createTaskInstance();
        OperationResult parentResult = task.getResult();
        PrismObject<O> obj;
        try {
            obj = model.getObject(type, oid, SelectorOptions.createCollection(GetOperationOptions.createResolveNames()), task, parentResult);
            return obj.asObjectable();
        } catch (ObjectNotFoundException | SchemaException | SecurityViolationException | CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
            // TODO Auto-generated catch block
            LOGGER.error("Could not get object with oid " + oid + ". Reason: " + e.getMessage());

        }
        return null;
    }

    public <O extends ObjectType> List<PrismObject<O>> resolveLinkRefs(Collection<ObjectReferenceType> refs, Class type) {

        List<PrismObject<O>> objects = new ArrayList<>();

        for (ObjectReferenceType ref : refs) {
            Class clazz = getClassForType(ref.getType());
            if (!clazz.equals(type)) {
                continue;
            }
            Task task = taskManager.createTaskInstance();
            OperationResult parentResult = task.getResult();
            try {
                PrismObject<O> obj = model.getObject(type, ref.getOid(), SelectorOptions.createCollection(GetOperationOptions.createResolveNames()), task, parentResult);
                objects.add(obj);
            } catch (ObjectNotFoundException | SchemaException | SecurityViolationException | CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
                // TODO Auto-generated catch block
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
        Object name = refValue.getTargetName() != null ? ref.getTargetName().getOrig() : null;
        if (!(name instanceof String)) {
            LOGGER.error("Couldn't resolve object name");
        }

        return (String) name;
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
            Class clazz = null;
            String oid = null;
            if (assignment.getTargetRef() != null) {
                clazz = getClassForType(assignment.getTargetRef().getType());
                oid = assignment.getTargetRef().getOid();
            } else if (assignment.getTarget() != null) {
                clazz = assignment.getTarget().getClass();
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

            if (assignment.getTarget() != null) {
                resolvedAssignments.add((PrismObject<O>) assignment.getTarget().asPrismObject());
                continue;
            }

            Task task = taskManager.createTaskInstance();
            try {
                PrismObject<O> obj = model.getObject(type, oid, null, task, task.getResult());
                resolvedAssignments.add(obj);
            } catch (ObjectNotFoundException | SchemaException | SecurityViolationException | CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
                LOGGER.error("Could not get object with oid " + oid + ". Reason: " + e.getMessage());

            }

        }

        return resolvedAssignments;
    }

    public List<AuditEventRecord> searchAuditRecords(String query, Map<String, Object> params) {

        if (StringUtils.isBlank(query)) {
            return new ArrayList<>();
        }

        Map<String, Object> resultSet = new HashMap<>();
        Set<Entry<String, Object>> paramSet = params.entrySet();
        for (Entry<String, Object> p : paramSet) {
            if (p.getValue() instanceof AuditEventTypeType) {
                resultSet.put(p.getKey(), AuditEventType.toAuditEventType((AuditEventTypeType) p.getValue()));
            } else if (p.getValue() instanceof AuditEventStageType) {
                resultSet.put(p.getKey(), AuditEventStage.toAuditEventStage((AuditEventStageType) p.getValue()));
            } else {
                resultSet.put(p.getKey(), p.getValue());
            }
        }
        return auditService.listRecords(query, resultSet);
    }

    public List<AuditEventRecord> searchAuditRecordsAsWorkflows(String query, Map<String, Object> params) {
        return transformToWorkflows(searchAuditRecords(query, params));
    }

    private List<AuditEventRecord> transformToWorkflows(List<AuditEventRecord> auditEvents) {
        if (auditEvents == null || auditEvents.isEmpty()) {
            return auditEvents;
        }

        // group all records by property/wf.processInstanceId
        Map<String, List<AuditEventRecord>> workflows = auditEvents.stream().collect(Collectors.groupingBy(event -> {
            Set<String> processInstanceIds = event.getPropertyValues(WorkflowConstants.AUDIT_PROCESS_INSTANCE_ID);

            Iterator<String> it = processInstanceIds.iterator();
            return it.hasNext() ? it.next() : "default workflow";
        }));

        // map of workflows in order of first request timestamp
        Map<Long, List<AuditEventRecord>> workflowsFiltered = new TreeMap<>();
        workflows.entrySet().stream().forEach(entry -> {
            List<AuditEventRecord> wf = entry.getValue();
            // leave only the first request in each workflow
            List<AuditEventRecord> filtered = new ArrayList<>();
            wf.stream().filter(record -> record.getEventStage() == AuditEventStage.REQUEST).findFirst()
                    .ifPresent(filtered::add);
            // and all executions with decision
            wf.stream().filter(record -> record.getEventStage() == AuditEventStage.EXECUTION)
                    .filter(record -> record.getMessage() == null || !record.getMessage().contains("(no decision)"))
                    .forEach(filtered::add);

            wf.stream().findFirst().ifPresent(record -> {

                workflowsFiltered.put(record.getTimestamp(), filtered);
            });
        });

        return workflowsFiltered.entrySet().stream().map(Entry::getValue).flatMap(List::stream)
                .collect(Collectors.toList());
    }

    public UserType getShadowOwner(String shadowOid) {
        Task task = taskManager.createTaskInstance();
        try {
            PrismObject<UserType> owner = model.findShadowOwner(shadowOid, task, task.getResult());
            return owner.asObjectable();
        } catch (ObjectNotFoundException | SecurityViolationException | SchemaException | ConfigurationException | ExpressionEvaluationException | CommunicationException e) {
            // TODO Auto-generated catch block
            LOGGER.error("Could not find owner for shadow with oid " + shadowOid + ". Reason: " + e.getMessage());
        }

        return null;

    }

    private Class getClassForType(QName type) {
        return prismContext.getSchemaRegistry().determineCompileTimeClass(type);
    }

    <T extends ObjectType> List<T> searchObjects(Class<T> type, ObjectQuery query) {
        List<T> ret = new ArrayList<>();
        Task task = taskManager.createTaskInstance();
        try {
            List<PrismObject<T>> list = model.searchObjects(type, query, null, task, task.getResult()).getList();
            for (PrismObject<T> po : list) {
                ret.add(po.asObjectable());
            }
        } catch (SchemaException | ObjectNotFoundException | SecurityViolationException | CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
            LOGGER.error("Could not search objects of type: " + type + " with query " + query + ". Reason: " + e.getMessage());
        }
        return ret;
    }

    <C extends Containerable, T> ObjectFilter createEqualFilter(QName propertyName, Class<C> type, T realValue) throws SchemaException {
        return QueryBuilder.queryFor(type, prismContext)
                .item(propertyName).eq(realValue)
                .buildFilter();
    }

    <C extends Containerable, T> ObjectFilter createEqualFilter(UniformItemPath propertyPath, Class<C> type, T realValue) throws SchemaException {
        return QueryBuilder.queryFor(type, prismContext)
                .item(propertyPath).eq(realValue)
                .buildFilter();
    }

    // TODO implement if needed
//    <O extends Containerable> RefFilter createReferenceEqualFilter(QName propertyName, Class<O> type, String... oids) {
//        return RefFilter.createReferenceEqual(propertyName, type, prismContext, oids);
//    }

//    <O extends Containerable> RefFilter createReferenceEqualFilter(ItemPath propertyPath, Class<O> type, String... oids) throws SchemaException {
//        return RefFilter.createReferenceEqual(propertyPath, type, prismContext, oids);
//    }

//    Object parseObjectFromXML (String xml) throws SchemaException {
//        return prismContext.parserFor(xml).xml().parseAnyData();
//    }
    public List<PrismContainerValue<WorkItemType>> searchApprovalWorkItems()
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException, DatatypeConfigurationException {
        return searchApprovalWorkItems(0, null);
    }
    
    /*
     * @param days - return only workitems with createTimestamp older than (now-days), 0 to return all
     * @sortColumn - optionally AbstractWorkItemType QName to asc sort results (e.g. AbstractWorkItemType.F_CREATE_TIMESTAMP)
    */
    public List<PrismContainerValue<WorkItemType>> searchApprovalWorkItems(int days, QName sortColumn)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException, DatatypeConfigurationException {
        Task task = taskManager.createTaskInstance();
        OperationResult result = task.getResult();
        ObjectQuery query = QueryBuilder.queryFor(AbstractWorkItemType.class, prismContext).build();
        
        if (days > 0) {
            XMLGregorianCalendar since = (new Clock()).currentTimeXMLGregorianCalendar();
            DatatypeFactory df = DatatypeFactory.newInstance();        
            since.add (df.newDuration(false, 0, 0, days, 0, 0, 0));

            query.addFilter(QueryBuilder.queryFor(AbstractWorkItemType.class, prismContext)
                         .item(AbstractWorkItemType.F_CREATE_TIMESTAMP).lt(since).buildFilter());
        } 
        
        if (sortColumn != null) {
            query.addFilter(QueryBuilder.queryFor(AbstractWorkItemType.class, prismContext)
                        .asc(sortColumn)
                        .buildFilter());
        }
	    Object[] itemsToResolve = { WorkItemType.F_ASSIGNEE_REF,
                ItemPath.create(PrismConstants.T_PARENT, WfContextType.F_OBJECT_REF),
                ItemPath.create(PrismConstants.T_PARENT, WfContextType.F_TARGET_REF),
                ItemPath.create(PrismConstants.T_PARENT, WfContextType.F_REQUESTER_REF) };
	    SearchResultList<WorkItemType> workItems = model.searchContainers(WorkItemType.class, query,
		        schemaHelper.getOperationOptionsBuilder().items(itemsToResolve).resolve().build(), task, result);
        return PrismContainerValue.toPcvList(workItems);
    }

    /**
     * Retrieves all definitions.
     * Augments them by count of campaigns (all + open ones).
     *
     * TODO query parameters, customizable sorting
     * definitions and campaigns counts are expected to be low, so we can afford to go through all of them here
     */
    public Collection<PrismObject<AccessCertificationDefinitionForReportType>> searchCertificationDefinitions() throws ConfigurationException, SchemaException, ObjectNotFoundException, CommunicationException, SecurityViolationException, ExpressionEvaluationException {

        Task task = taskManager.createTaskInstance();
        OperationResult result = task.getResult();
        Collection<SelectorOptions<GetOperationOptions>> options =
                SelectorOptions.createCollection(GetOperationOptions.createResolveNames());
        List<PrismObject<AccessCertificationDefinitionType>> definitions = model.searchObjects(AccessCertificationDefinitionType.class, null, options, task, result);
        final Map<String,PrismObject<AccessCertificationDefinitionForReportType>> definitionsForReportMap = new HashMap<>();
        for (PrismObject<AccessCertificationDefinitionType> definition : definitions) {
            // create subclass with the values copied from the superclass
            PrismObject<AccessCertificationDefinitionForReportType> definitionForReport = prismContext.createObjectable(AccessCertificationDefinitionForReportType.class).asPrismObject();
            for (Item<?,?> item : definition.getValue().getItems()) {
                definitionForReport.getValue().add(item.clone());
            }
            definitionsForReportMap.put(definition.getOid(), definitionForReport);
        }

        ResultHandler<AccessCertificationCampaignType> handler = (campaignObject, parentResult) -> {
			AccessCertificationCampaignType campaign = campaignObject.asObjectable();
			if (campaign.getDefinitionRef() != null) {
				String definitionOid = campaign.getDefinitionRef().getOid();
				PrismObject<AccessCertificationDefinitionForReportType> definitionObject = definitionsForReportMap.get(definitionOid);
				if (definitionObject != null) {
					AccessCertificationDefinitionForReportType definition = definitionObject.asObjectable();
					int campaigns = definition.getCampaigns() != null ? definition.getCampaigns() : 0;
					definition.setCampaigns(campaigns+1);
					AccessCertificationCampaignStateType state = campaign.getState();
					if (state != AccessCertificationCampaignStateType.CREATED && state != CLOSED) {
						int openCampaigns = definition.getOpenCampaigns() != null ? definition.getOpenCampaigns() : 0;
						definition.setOpenCampaigns(openCampaigns+1);
					}
				}
			}
			return true;
		};
        model.searchObjectsIterative(AccessCertificationCampaignType.class, null, handler, null, task, result);

        List<PrismObject<AccessCertificationDefinitionForReportType>> rv = new ArrayList<>(definitionsForReportMap.values());
        Collections.sort(rv, (o1, o2) -> {
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

    public List<PrismContainerValue<AccessCertificationCaseType>> getCertificationCampaignCases(String campaignName) throws SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException {
        List<AccessCertificationCaseType> cases = getCertificationCampaignCasesAsBeans(campaignName);
        return PrismContainerValue.toPcvList(cases);
    }

    private List<AccessCertificationCaseType> getCertificationCampaignCasesAsBeans(String campaignName) throws SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException {
        Task task = taskManager.createTaskInstance();
        ObjectQuery query;
        if (StringUtils.isEmpty(campaignName)) {
            //query = null;
            return new ArrayList<>();
        } else {
            query = QueryBuilder.queryFor(AccessCertificationCaseType.class, prismContext)
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

    private List<AccessCertificationCaseType> getCertificationCampaignNotRespondedCasesAsBeans(String campaignName) throws SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException {
        Task task = taskManager.createTaskInstance();
        ObjectQuery query;
        if (StringUtils.isEmpty(campaignName)) {
            //query = null;
            return new ArrayList<>();
        } else {
            query = QueryBuilder.queryFor(AccessCertificationCaseType.class, prismContext)
                    .item(PrismConstants.T_PARENT, F_NAME).eqPoly(campaignName, "").matchingOrig()
                    .and().item(AccessCertificationCaseType.F_CURRENT_STAGE_OUTCOME).eq(SchemaConstants.MODEL_CERTIFICATION_OUTCOME_NO_RESPONSE)
                    // TODO first by object/target type then by name (not supported by the repository as of now)
                    .asc(AccessCertificationCaseType.F_OBJECT_REF, PrismConstants.T_OBJECT_REFERENCE, ObjectType.F_NAME)
                    .asc(AccessCertificationCaseType.F_TARGET_REF, PrismConstants.T_OBJECT_REFERENCE, ObjectType.F_NAME)
                    .build();
        }
        Collection<SelectorOptions<GetOperationOptions>> options =
                SelectorOptions.createCollection(GetOperationOptions.createResolveNames());
        return model.searchContainers(AccessCertificationCaseType.class, query, options, task, task.getResult());
    }

    public List<PrismContainerValue<AccessCertificationWorkItemType>> getCertificationCampaignDecisions(String campaignName, Integer stageNumber)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        return getCertificationCampaignDecisions(campaignName, stageNumber, null);
    }

    public List<PrismContainerValue<AccessCertificationWorkItemType>> getCertificationCampaignDecisions(String campaignName, Integer stageNumber, Integer iteration)
            throws SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException {
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
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        Task task = taskManager.createTaskInstance();
        if (StringUtils.isEmpty(campaignName)) {
            return null;
        }
        ObjectQuery query = QueryBuilder.queryFor(AccessCertificationCampaignType.class, prismContext)
                .item(AccessCertificationCampaignType.F_NAME).eqPoly(campaignName).matchingOrig()
                .build();
        List<PrismObject<AccessCertificationCampaignType>> objects = model
                .searchObjects(AccessCertificationCampaignType.class, query, null, task, task.getResult());
        if (objects.isEmpty()) {
            return null;
        } else if (objects.size() == 1) {
            return objects.get(0).asObjectable();
        } else {
            throw new IllegalStateException("More than one certification campaign found by name '" + campaignName + "': " + objects);
        }
    }

    @SuppressWarnings("unused")
    public List<PrismContainerValue<AccessCertificationWorkItemType>> getCertificationCampaignNonResponders(String campaignName, Integer stageNumber)
            throws SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException {
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

    public List<PrismObject<AccessCertificationCampaignType>> getCertificationCampaigns(Boolean alsoClosedCampaigns) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, SecurityViolationException, ExpressionEvaluationException {
        Task task = taskManager.createTaskInstance();

        ObjectQuery query = QueryBuilder.queryFor(AccessCertificationCampaignType.class, prismContext)
                                .asc(F_NAME)
                                .build();
        if (!Boolean.TRUE.equals(alsoClosedCampaigns)) {
            query.addFilter(
                    QueryBuilder.queryFor(AccessCertificationCampaignType.class, prismContext)
                        .not().item(F_STATE).eq(CLOSED)
                        .buildFilter()
            );
        }

        Collection<SelectorOptions<GetOperationOptions>> options = schemaHelper.getOperationOptionsBuilder()
                .root().resolveNames()
                .item(AccessCertificationCampaignType.F_CASE).retrieve()
                .build();
        return model.searchObjects(AccessCertificationCampaignType.class, query, options, task, task.getResult());
    }

}
