/*
 * Copyright (c) 2010-2015 Evolveum
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
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionForReportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.CLOSED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_STATE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_OBJECT_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_TARGET_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType.F_NAME;

public class ReportFunctions {

    private static final Trace LOGGER = TraceManager.getTrace(ReportFunctions.class);

    private PrismContext prismContext;
    private ModelService model;

    private TaskManager taskManager;

    private AuditService auditService;

    public ReportFunctions(PrismContext prismContext, ModelService modelService, TaskManager taskManager, AuditService auditService) {
        this.prismContext = prismContext;
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
        } catch (ObjectNotFoundException | SchemaException | SecurityViolationException | CommunicationException | ConfigurationException e) {
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
            } catch (ObjectNotFoundException | SchemaException | SecurityViolationException | CommunicationException | ConfigurationException e) {
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

            if (!clazz.equals(type)) {
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
            } catch (ObjectNotFoundException | SchemaException | SecurityViolationException | CommunicationException | ConfigurationException e) {
                LOGGER.error("Could not get object with oid " + oid + ". Reason: " + e.getMessage());

            }

        }

        return resolvedAssignments;
    }

    public List<AuditEventRecord> searchAuditRecords(String query, Map<String, Object> params) {

        if (StringUtils.isBlank(query)) {
            return new ArrayList<>();
        }

        Map<String, Object> resultSet = new HashMap<String, Object>();
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

    public UserType getShadowOwner(String shadowOid) {
        Task task = taskManager.createTaskInstance();
        try {
            PrismObject<UserType> owner = model.findShadowOwner(shadowOid, task, task.getResult());
            return owner.asObjectable();
        } catch (ObjectNotFoundException | SecurityViolationException | SchemaException | ConfigurationException e) {
            // TODO Auto-generated catch block
            LOGGER.error("Could not find owner for shadow with oid " + shadowOid + ". Reason: " + e.getMessage());
        }

        return null;

    }

    private Class getClassForType(QName type) {
        return prismContext.getSchemaRegistry().determineCompileTimeClass(type);
    }

    <T extends ObjectType> List<T> searchObjects(Class<T> type, ObjectQuery query) {
        List<T> ret = new ArrayList();
        Task task = taskManager.createTaskInstance();
        try {
            List<PrismObject<T>> list = model.searchObjects(type, query, null, task, task.getResult()).getList();
            for (PrismObject<T> po : list) {
                ret.add(po.asObjectable());
            }
        } catch (SchemaException | ObjectNotFoundException | SecurityViolationException | CommunicationException | ConfigurationException e) {
            LOGGER.error("Could not search objects of type: " + type + " with query " + query + ". Reason: " + e.getMessage());
        }
        return ret;
    }

    <C extends Containerable, T> ObjectFilter createEqualFilter(QName propertyName, Class<C> type, T realValue) throws SchemaException {
        return QueryBuilder.queryFor(type, prismContext)
                .item(propertyName).eq(realValue)
                .buildFilter();
    }

    <C extends Containerable, T> ObjectFilter createEqualFilter(ItemPath propertyPath, Class<C> type, T realValue) throws SchemaException {
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

    /**
     * Retrieves all definitions.
     * Augments them by count of campaigns (all + open ones).
     *
     * TODO query parameters, customizable sorting
     * definitions and campaigns counts are expected to be low, so we can afford to go through all of them here
     */
    public Collection<PrismObject<AccessCertificationDefinitionForReportType>> searchCertificationDefinitions() throws ConfigurationException, SchemaException, ObjectNotFoundException, CommunicationException, SecurityViolationException {

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

        ResultHandler<AccessCertificationCampaignType> handler = new ResultHandler<AccessCertificationCampaignType>() {
            @Override
            public boolean handle(PrismObject<AccessCertificationCampaignType> campaignObject, OperationResult parentResult) {
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
            }
        };
        model.searchObjectsIterative(AccessCertificationCampaignType.class, null, handler, null, task, result);

        List<PrismObject<AccessCertificationDefinitionForReportType>> rv = new ArrayList<>(definitionsForReportMap.values());
        Collections.sort(rv, new Comparator<PrismObject<AccessCertificationDefinitionForReportType>>() {
            @Override
            public int compare(PrismObject<AccessCertificationDefinitionForReportType> o1, PrismObject<AccessCertificationDefinitionForReportType> o2) {
                String n1 = o1.asObjectable().getName().getOrig();
                String n2 = o2.asObjectable().getName().getOrig();
                if (n1 == null) {
                    n1 = "";
                }
                return n1.compareTo(n2);
            }
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

    public List<PrismContainerValue<AccessCertificationCaseType>> getCertificationCampaignCases(String campaignName) throws SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {
        List<AccessCertificationCaseType> cases = getCertificationCampaignCasesAsBeans(campaignName);
        return PrismContainerValue.toPcvList(cases);
    }

    private List<AccessCertificationCaseType> getCertificationCampaignCasesAsBeans(String campaignName) throws SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {
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

//    public List<PrismContainerValue<AccessCertificationDecisionType>> getCertificationCampaignDecisions(String campaignName, Integer stageNumber) throws SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {
//        List<AccessCertificationCaseType> cases = getCertificationCampaignCasesAsBeans(campaignName);
//        List<AccessCertificationDecisionType> decisions = new ArrayList<>();
//        for (AccessCertificationCaseType aCase : cases) {
//            for (AccessCertificationDecisionType decision : aCase.getDecision()) {
//                if (stageNumber == null || decision.getStageNumber() == stageNumber) {
//                    decisions.add(decision);
//                }
//            }
//        }
//        return PrismContainerValue.toPcvList(decisions);
//    }

    public List<PrismObject<AccessCertificationCampaignType>> getCertificationCampaigns(Boolean alsoClosedCampaigns) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, SecurityViolationException {
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

        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createResolveNames());
        options.add(SelectorOptions.create(AccessCertificationCampaignType.F_CASE,
                GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));

        return model.searchObjects(AccessCertificationCampaignType.class, query, options, task, task.getResult());
    }

}
