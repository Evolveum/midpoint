/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.roles;

import java.util.*;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PolyStringUtils;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.wicket.ajax.AjaxRequestTarget;

import com.evolveum.midpoint.gui.api.component.ChooseArchetypeMemberPopup;
import com.evolveum.midpoint.gui.api.component.ChooseMemberPopup;
import com.evolveum.midpoint.gui.api.component.ChooseOrgMemberPopup;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntry;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskBinding;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.page.admin.roles.AbstractRoleMemberPanel.QueryScope;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionParameterValueType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

public class MemberOperationsHelper {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractRoleMemberPanel.class);

    public static final String UNASSIGN_OPERATION = "unassign";
    public static final String ASSIGN_OPERATION = "assign";
    public static final String DELETE_OPERATION = "delete";
    public static final String RECOMPUTE_OPERATION = "recompute";
    public static final String ROLE_PARAMETER = "role";
    public static final String RELATION_PARAMETER = "relation";

    public static <R extends AbstractRoleType> void unassignMembersPerformed(PageBase pageBase, R targetObject, QueryScope scope,
            ObjectQuery query, Collection<QName> relations, QName type, AjaxRequestTarget target) {
        Task operationalTask = pageBase.createSimpleTask(getTaskName("Remove", scope));

        ExecuteScriptType script = new ExecuteScriptType();
        ActionExpressionType expression = new ActionExpressionType();
        expression.setType(UNASSIGN_OPERATION);

        //hack using fake definition because of type
        PrismPropertyDefinition<Object> def = pageBase.getPrismContext().definitionFactory().createPropertyDefinition(
                AbstractRoleType.F_NAME, DOMUtil.XSD_STRING);
        PrismValue value = pageBase.getPrismContext().itemFactory().createValue(targetObject.getOid());
        try {
            value.applyDefinition(def);
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Can not apply definition " + def, e);
            operationalTask.getResult().recordFatalError(pageBase.createStringResource("MemberOperationsHelper.message.unassignMembersPerformed.fatalError", def).getString(), e);
        }
        expression.parameter(new ActionParameterValueType().name(ROLE_PARAMETER).value(
                new RawType(value, DOMUtil.XSD_STRING, pageBase.getPrismContext())));
        if(relations != null) {
            relations.forEach(relation -> expression.parameter(new ActionParameterValueType().name(RELATION_PARAMETER).value(QNameUtil.qNameToUri(relation))));
        }
        script.setScriptingExpression(new JAXBElement<>(SchemaConstants.S_ACTION,
                ActionExpressionType.class, expression));

        executeMemberOperation(pageBase, operationalTask, type, query, script, target);

    }

    public static void assignMembersPerformed(AbstractRoleType targetObject, ObjectQuery query,
            QName relation, QName type, AjaxRequestTarget target, PageBase pageBase) {
        Task operationalTask = pageBase.createSimpleTask("Add.members");

        ExecuteScriptType script = new ExecuteScriptType();
        ActionExpressionType expression = new ActionExpressionType();
        expression.setType(ASSIGN_OPERATION);

        PrismReferenceValue value = pageBase.getPrismContext().itemFactory()
                .createReferenceValue(targetObject.getOid(), WebComponentUtil.classToQName(pageBase.getPrismContext(), targetObject.getClass()));
        expression.parameter(new ActionParameterValueType().name(ROLE_PARAMETER).value(
                new RawType(value, ObjectReferenceType.COMPLEX_TYPE, pageBase.getPrismContext())));
        if(relation != null) {
            expression.parameter(new ActionParameterValueType().name(RELATION_PARAMETER).value(QNameUtil.qNameToUri(relation)));
        }
        script.setScriptingExpression(new JAXBElement<>(SchemaConstants.S_ACTION,
                ActionExpressionType.class, expression));

        executeMemberOperation(pageBase, operationalTask, type, query, script, target);
    }

    public static void deleteMembersPerformed(
            PageBase pageBase, QueryScope scope, ObjectQuery query, AjaxRequestTarget target) {
        Task task = createDeleteMembersTask(pageBase, scope, query, target);
        if (task != null) {
            executeMemberOperation(pageBase, task, target);
        }
    }

    public static void recomputeMembersPerformed(
            PageBase pageBase, QueryScope scope, ObjectQuery query, AjaxRequestTarget target) {
        Task task = createRecomputeMembersTask(pageBase, scope, query, target);
        if (task != null) {
            executeMemberOperation(pageBase, task, target);
        }
    }

    public static Task createRecomputeMembersTask(PageBase pageBase, QueryScope scope,
            ObjectQuery query, AjaxRequestTarget target) {
        Task operationalTask = pageBase.createSimpleTask(getTaskName(RECOMPUTE_OPERATION, scope));
        OperationResult parentResult = operationalTask.getResult();
            operationalTask.getClonedTaskObject().asObjectable().getAssignment()
                    .add(ObjectTypeUtil.createAssignmentTo(SystemObjectsType.ARCHETYPE_RECOMPUTATION_TASK.value(), ObjectTypes.ARCHETYPE, pageBase.getPrismContext()));
            operationalTask.getClonedTaskObject().asObjectable().getArchetypeRef()
                    .add(ObjectTypeUtil.createObjectRef(SystemObjectsType.ARCHETYPE_RECOMPUTATION_TASK.value(), ObjectTypes.ARCHETYPE));
            return createRecomputeMemberOperationTask(operationalTask, AssignmentHolderType.COMPLEX_TYPE, query,
                    null, parentResult, pageBase, target);

    }

    private static Task createDeleteMembersTask(PageBase pageBase, QueryScope scope,
            ObjectQuery query, AjaxRequestTarget target) {
        QName defaultType = AssignmentHolderType.COMPLEX_TYPE;
        Task operationalTask = pageBase.createSimpleTask(getTaskName(DELETE_OPERATION, scope));

        ExecuteScriptType script = new ExecuteScriptType();
        ActionExpressionType expression = new ActionExpressionType();
        expression.setType("delete");

        script.setScriptingExpression(new JAXBElement<>(SchemaConstants.S_ACTION,
                ActionExpressionType.class, expression));

        return createMemberOperationTask(pageBase, operationalTask, defaultType, query, script, SelectorOptions.createCollection(GetOperationOptions.createDistinct()), target);

    }

    public static <R extends AbstractRoleType> void assignMembers(PageBase pageBase, R targetRefObject, AjaxRequestTarget target,
            AvailableRelationDto availableRelationList, List<QName> objectTypes) {
        assignMembers(pageBase, targetRefObject, target, availableRelationList, objectTypes, true);

    }

    public static <R extends AbstractRoleType> void assignMembers(PageBase pageBase, R targetRefObject, AjaxRequestTarget target,
            AvailableRelationDto availableRelationList, List<QName> objectTypes, boolean isOrgTreePanelVisible) {
        assignMembers(pageBase, targetRefObject, target, availableRelationList, objectTypes, new ArrayList<>(), isOrgTreePanelVisible);
    }

    public static <O extends ObjectType, R extends AbstractRoleType> void assignMembers(PageBase pageBase, R targetRefObject, AjaxRequestTarget target,
            AvailableRelationDto availableRelationList, List<QName> objectTypes, List<ObjectReferenceType> archetypeRefList, boolean isOrgTreePanelVisible) {

        ChooseMemberPopup<O, R> browser = new ChooseMemberPopup<O, R>(pageBase.getMainPopupBodyId(), availableRelationList) {
            private static final long serialVersionUID = 1L;

            @Override
            protected R getAssignmentTargetRefObject(){
                return targetRefObject;
            }

            @Override
            protected List<QName> getAvailableObjectTypes(){
                return objectTypes;
            }

            @Override
            protected List<ObjectReferenceType> getArchetypeRefList(){
                return archetypeRefList;
            }

            @Override
            protected boolean isOrgTreeVisible(){
                return isOrgTreePanelVisible;
            }
        };
        browser.setOutputMarkupId(true);
        pageBase.showMainPopup(browser, target);
    }

    public static <O extends ObjectType> void assignOrgMembers(PageBase pageBase, OrgType targetRefObject, AjaxRequestTarget target,
            AvailableRelationDto availableRelationList, List<QName> objectTypes, List<ObjectReferenceType> archetypeRefList) {
        ChooseOrgMemberPopup<O> browser = new ChooseOrgMemberPopup<O>(pageBase.getMainPopupBodyId(), availableRelationList) {

            private static final long serialVersionUID = 1L;

            @Override
            protected OrgType getAssignmentTargetRefObject(){
                return targetRefObject;
            }

            @Override
            protected List<QName> getAvailableObjectTypes(){
                return objectTypes;
            }

            @Override
            protected List<ObjectReferenceType> getArchetypeRefList(){
                return archetypeRefList;
            }
        };

        browser.setOutputMarkupId(true);
        pageBase.showMainPopup(browser, target);
    }

    public static <O extends AssignmentHolderType> void assignArchetypeMembers(PageBase pageBase, ArchetypeType targetRefObject, AjaxRequestTarget target,
            AvailableRelationDto availableRelationList, List<QName> objectTypes, List<ObjectReferenceType> archetypeRefList) {
        ChooseArchetypeMemberPopup<O> browser = new ChooseArchetypeMemberPopup<O>(pageBase.getMainPopupBodyId(), availableRelationList) {

            private static final long serialVersionUID = 1L;

            @Override
            protected ArchetypeType getAssignmentTargetRefObject(){
                return targetRefObject;
            }

            @Override
            protected List<QName> getAvailableObjectTypes(){
                return objectTypes;
            }

            @Override
            protected List<ObjectReferenceType> getArchetypeRefList(){
                return archetypeRefList;
            }
        };

        browser.setOutputMarkupId(true);
        pageBase.showMainPopup(browser, target);
    }

    public static <R extends AbstractRoleType> ObjectQuery createDirectMemberQuery(R targetObject, QName objectType, Collection<QName> relations, ObjectViewDto<OrgType> tenant, ObjectViewDto<OrgType> project, PrismContext prismContext) {
        // We assume tenantRef.relation and orgRef.relation are always default ones (see also MID-3581)
        S_FilterEntry q0;
        if (objectType == null || FocusType.COMPLEX_TYPE.equals(objectType)) {
            q0 = prismContext.queryFor(FocusType.class);
        } else {
            q0 = prismContext.queryFor(FocusType.class)
                    .type(objectType);
        }

        // Use exists filter to build a query like this:
        // $a/targetRef = oid1 and $a/tenantRef = oid2 and $a/orgRef = oid3
        S_AtomicFilterExit q = q0.exists(FocusType.F_ASSIGNMENT)
                .block()
                .item(AssignmentType.F_TARGET_REF)
                .ref(createReferenceValuesList(targetObject, relations));

        if (tenant != null && tenant.getObjectType() != null) {
            q = q.and().item(AssignmentType.F_TENANT_REF).ref(ObjectTypeUtil.createObjectRef(tenant.getObjectType(),
                    prismContext).asReferenceValue());
        }

        if (project != null && project.getObjectType() != null) {
            q = q.and().item(AssignmentType.F_ORG_REF).ref(ObjectTypeUtil.createObjectRef(project.getObjectType(),
                    prismContext).asReferenceValue());
        }

        ObjectQuery query = q.endBlock().build();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Searching members of role {} with query:\n{}", targetObject.getOid(), query.debugDump());
        }
        return query;
    }

    public static <R extends AbstractRoleType> List<PrismReferenceValue> createReferenceValuesList(R targetObject, Collection<QName> relations) {
        List<PrismReferenceValue> referenceValuesList = new ArrayList<>();
        relations.forEach(relation -> referenceValuesList.add(createReference(targetObject, relation).asReferenceValue()));
        return referenceValuesList;
    }

    public static <O extends ObjectType> ObjectQuery createSelectedObjectsQuery(List<O> selectedObjects,
            PrismContext prismContext) {
        Set<String> oids = getFocusOidToRecompute(selectedObjects);
        return prismContext.queryFor(AssignmentHolderType.class).id(oids.toArray(new String[0])).build();
    }

    public static <O extends ObjectType> Set<String> getFocusOidToRecompute(List<O> selectedObjects) {
        Set<String> oids = new HashSet<>();
        selectedObjects.forEach(f -> oids.add(f.getOid()));
        return oids;
    }

    private static String getTaskName(String operation, QueryScope scope) {
        StringBuilder nameBuilder = new StringBuilder(operation);
        nameBuilder.append(".");
        if (scope != null) {
            nameBuilder.append(scope.name());
            nameBuilder.append(".");
        }
        nameBuilder.append("members");
        return nameBuilder.toString();
    }

    public static <R extends AbstractRoleType> ObjectReferenceType createReference(R targetObject, QName relation) {
        return ObjectTypeUtil.createObjectRef(targetObject, relation);
    }

    protected static Task createMemberOperationTask(PageBase modelServiceLocator, Task operationalTask, QName type, ObjectQuery memberQuery,
            ExecuteScriptType script, Collection<SelectorOptions<GetOperationOptions>> option, AjaxRequestTarget target) {

        OperationResult parentResult = operationalTask.getResult();
        return createMemberOperationTask(operationalTask, type, memberQuery, script, option, parentResult, modelServiceLocator, target);

    }

    protected static void executeMemberOperation(PageBase modelServiceLocator, Task operationalTask, QName type, ObjectQuery memberQuery,
            ExecuteScriptType script, AjaxRequestTarget target) {

        OperationResult parentResult = operationalTask.getResult();
        Task executableTask = createMemberOperationTask(operationalTask, type, memberQuery, script, null, parentResult, modelServiceLocator, target);
        if (executableTask == null) {
            target.add(modelServiceLocator.getFeedbackPanel());
            return;
        }

        TaskType executableTaskType = executableTask.getUpdatedTaskObject().asObjectable();
        executableTaskType.getAssignment()
                  .add(ObjectTypeUtil.createAssignmentTo(SystemObjectsType.ARCHETYPE_ITERATIVE_BULK_ACTION_TASK.value(), ObjectTypes.ARCHETYPE, modelServiceLocator.getPrismContext()));
        executableTaskType.getArchetypeRef()
                        .add(ObjectTypeUtil.createObjectRef(SystemObjectsType.ARCHETYPE_ITERATIVE_BULK_ACTION_TASK.value(), ObjectTypes.ARCHETYPE));
        executeMemberOperation(executableTask, parentResult, modelServiceLocator);
        target.add(modelServiceLocator.getFeedbackPanel());
    }

    protected static void executeMemberOperation(PageBase modelServiceLocator, Task operationalTask, AjaxRequestTarget target) {
        OperationResult parentResult = operationalTask.getResult();
        executeMemberOperation(operationalTask, parentResult, modelServiceLocator);
        target.add(modelServiceLocator.getFeedbackPanel());
    }

    public static Task createMemberOperationTask(Task operationalTask, QName type, ObjectQuery memberQuery,
            ExecuteScriptType script, Collection<SelectorOptions<GetOperationOptions>> option, OperationResult parentResult, PageBase pageBase, AjaxRequestTarget target) {

        try {
            createTask(operationalTask, type, memberQuery, option, parentResult, pageBase);
            pageBase.getSecurityEnforcer().authorize(ModelAuthorizationAction.EXECUTE_SCRIPT.getUrl(),
                    null, AuthorizationParameters.EMPTY, null, operationalTask, parentResult);
            operationalTask.setExtensionPropertyValue(SchemaConstants.SE_EXECUTE_SCRIPT, script);
            operationalTask.setHandlerUri(ModelPublicConstants.ITERATIVE_SCRIPT_EXECUTION_TASK_HANDLER_URI);
            return operationalTask;
        } catch (Exception e) {
            parentResult.recordFatalError(pageBase.createStringResource("WebComponentUtil.message.startPerformed.fatalError.createTask").getString(), e);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't create bulk action task", e);
            target.add(pageBase.getFeedbackPanel());
        }
        return null;
    }

    public static Task createRecomputeMemberOperationTask(Task operationalTask, QName type, ObjectQuery memberQuery,
            Collection<SelectorOptions<GetOperationOptions>> option, OperationResult parentResult, PageBase pageBase, AjaxRequestTarget target) {
        try {
            createTask(operationalTask, type, memberQuery, option, parentResult, pageBase);
            pageBase.getSecurityEnforcer().authorize(ModelAuthorizationAction.RECOMPUTE.getUrl(),
                    null, AuthorizationParameters.EMPTY, null, operationalTask, parentResult);
            operationalTask.setHandlerUri(ModelPublicConstants.RECOMPUTE_HANDLER_URI);
            return operationalTask;
        } catch (Exception e) {
            parentResult.recordFatalError(pageBase.createStringResource("WebComponentUtil.message.startPerformed.fatalError.createTask").getString(), e);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't create bulk action task", e);
            target.add(pageBase.getFeedbackPanel());
        }
        return null;
    }

    private static void createTask(Task operationalTask, QName type, ObjectQuery memberQuery, Collection<SelectorOptions<GetOperationOptions>> option,
            OperationResult parentResult, PageBase pageBase) throws SchemaException {
        MidPointPrincipal owner = SecurityUtils.getPrincipalUser();
        operationalTask.setOwner(owner.getFocus().asPrismObject());

        operationalTask.setBinding(TaskBinding.LOOSE);
        operationalTask.setInitialExecutionStatus(TaskExecutionStatus.RUNNABLE);
        operationalTask.setThreadStopAction(ThreadStopActionType.RESTART);
        ScheduleType schedule = new ScheduleType();
        schedule.setMisfireAction(MisfireActionType.EXECUTE_IMMEDIATELY);
        operationalTask.makeSingle(schedule);
        operationalTask.setName(WebComponentUtil.createPolyFromOrigString(pageBase.createStringResource(parentResult.getOperation()).getString()));

        PrismPropertyDefinition<QueryType> propertyDefQuery = pageBase.getPrismContext().getSchemaRegistry()
                .findPropertyDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_OBJECT_QUERY);
        PrismProperty<QueryType> queryProperty = propertyDefQuery.instantiate();
        QueryType queryType = pageBase.getQueryConverter().createQueryType(memberQuery);
        queryProperty.setRealValue(queryType);
        operationalTask.addExtensionProperty(queryProperty);

        PrismPropertyDefinition<QName> propertyDefType = pageBase.getPrismContext().getSchemaRegistry()
                .findPropertyDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_OBJECT_TYPE);
        PrismProperty<QName> typeProperty = propertyDefType.instantiate();
        typeProperty.setRealValue(type);
        operationalTask.addExtensionProperty(typeProperty);

        if (option != null) {
            PrismPropertyDefinition<SelectorQualifiedGetOptionsType> propertyDefOption = pageBase.getPrismContext().getSchemaRegistry()
                    .findPropertyDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_SEARCH_OPTIONS);
            PrismProperty<SelectorQualifiedGetOptionsType> optionProperty = propertyDefOption.instantiate();
            optionProperty.setRealValue(MiscSchemaUtil.optionsToOptionsType(option));
            operationalTask.addExtensionProperty(optionProperty);
        }
    }


    public static void executeMemberOperation(Task operationalTask, OperationResult parentResult, PageBase pageBase) {

        OperationResult result = parentResult.createSubresult("evaluateExpressionInBackground");
        pageBase.getTaskManager().switchToBackground(operationalTask, result);
        result.computeStatus();
        parentResult.recordInProgress();
        parentResult.setBackgroundTaskOid(operationalTask.getOid());
        pageBase.showResult(parentResult);
    }

}
