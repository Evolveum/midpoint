/**
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.roles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.query.QueryFactory;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntry;
import org.apache.wicket.ajax.AjaxRequestTarget;

import com.evolveum.midpoint.gui.api.component.ChooseMemberPopup;
import com.evolveum.midpoint.gui.api.component.ChooseOrgMemberPopup;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.page.admin.roles.AbstractRoleMemberPanel.QueryScope;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionParameterValueType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ScriptingExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.SearchExpressionType;
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
            LoggingUtils.logUnexpectedException(LOGGER, "Can not aply definition " + def, e);
            operationalTask.getResult().recordFatalError(pageBase.createStringResource("MemberOperationsHelper.message.unassignMembersPerformed.fatalError", def).getString(), e);
        }
        expression.parameter(new ActionParameterValueType().name(ROLE_PARAMETER).value(
                new RawType(value, DOMUtil.XSD_STRING, pageBase.getPrismContext())));
        if(relations != null) {
            relations.forEach(relation -> {
                expression.parameter(new ActionParameterValueType().name(RELATION_PARAMETER).value(QNameUtil.qNameToUri(relation)));
            });
        }
        script.setScriptingExpression(new JAXBElement<ActionExpressionType>(SchemaConstants.S_ACTION,
                ActionExpressionType.class, expression));

//        try {
//            script.setQuery(pageBase.getQueryConverter().createQueryType(query));
//        } catch (SchemaException e) {
//            LoggingUtils.logUnexpectedException(LOGGER, "Can not create ObjectQuery from " + query, e);
//            operationalTask.getResult().recordFatalError("Can not create ObjectQuery from " + query, e);
//        }

        executeMemberOperation(pageBase, operationalTask, type, query, script, null, target);

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
        script.setScriptingExpression(new JAXBElement<ActionExpressionType>(SchemaConstants.S_ACTION,
                ActionExpressionType.class, expression));

//        try {
//            script.setQuery(pageBase.getQueryConverter().createQueryType(query));
//        } catch (SchemaException e) {
//            LoggingUtils.logUnexpectedException(LOGGER, "Can not create ObjectQuery from " + query, e);
//            operationalTask.getResult().recordFatalError("Can not create ObjectQuery from " + query, e);
//        }

        executeMemberOperation(pageBase, operationalTask, type, query, script, null, target);
    }

    public static <R extends AbstractRoleType> void deleteMembersPerformed(PageBase pageBase, QueryScope scope,
            ObjectQuery query, QName type, AjaxRequestTarget target) {
        recomputeOrDeleteMembersPerformed(pageBase, scope, query, target, "delete", DELETE_OPERATION);
    }

    public static <R extends AbstractRoleType> void recomputeMembersPerformed(PageBase pageBase, QueryScope scope,
            ObjectQuery query, AjaxRequestTarget target) {
        recomputeOrDeleteMembersPerformed(pageBase, scope, query, target, "recompute", RECOMPUTE_OPERATION);
    }

    private static <R extends AbstractRoleType> void recomputeOrDeleteMembersPerformed(PageBase pageBase, QueryScope scope,
            ObjectQuery query, AjaxRequestTarget target, String operation, String displayNameOfOperation) {
        QName defaultType = AssignmentHolderType.COMPLEX_TYPE;
        Task operationalTask = pageBase.createSimpleTask(getTaskName(displayNameOfOperation, scope));

        ExecuteScriptType script = new ExecuteScriptType();
        ActionExpressionType expression = new ActionExpressionType();
        expression.setType(operation);

        script.setScriptingExpression(new JAXBElement<ActionExpressionType>(SchemaConstants.S_ACTION,
                ActionExpressionType.class, expression));

//        try {
//            script.setQuery(pageBase.getQueryConverter().createQueryType(query));
//        } catch (SchemaException e) {
//            LoggingUtils.logUnexpectedException(LOGGER, "Can not create ObjectQuery from " + query, e);
//            operationalTask.getResult().recordFatalError("Can not create ObjectQuery from " + query, e);
//        }

        executeMemberOperation(pageBase, operationalTask, defaultType, query, script, SelectorOptions.createCollection(GetOperationOptions.createDistinct()), target);

    }

    public static <O extends ObjectType, R extends AbstractRoleType> void assignMembers(PageBase pageBase, R targetRefObject, AjaxRequestTarget target,
            AvailableRelationDto availableRelationList, List<QName> objectTypes) {
        assignMembers(pageBase, targetRefObject, target, availableRelationList, objectTypes, true);

    }

    public static <O extends ObjectType, R extends AbstractRoleType> void assignMembers(PageBase pageBase, R targetRefObject, AjaxRequestTarget target,
            AvailableRelationDto availableRelationList, List<QName> objectTypes, boolean isOrgTreePanelVisible) {

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
            protected boolean isOrgTreeVisible(){
                return isOrgTreePanelVisible;
            }
        };
        browser.setOutputMarkupId(true);
        pageBase.showMainPopup(browser, target);
    }

    public static <O extends ObjectType> void assignOrgMembers(PageBase pageBase, OrgType targetRefObject, AjaxRequestTarget target,
            AvailableRelationDto availableRelationList, List<QName> objectTypes) {
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
        QueryFactory queryFactory = prismContext.queryFactory();
        return queryFactory.createQuery(queryFactory.createInOid(oids));
    }

    public static <O extends ObjectType> Set<String> getFocusOidToRecompute(List<O> selectedObjects) {
        Set<String> oids = new HashSet<>();
        selectedObjects.stream().forEach(f -> oids.add(f.getOid()));
        return oids;
    }

    private static String getTaskName(String operation, QueryScope scope) {
        return getTaskName(operation, scope, false);
    }

    private static String getTaskName(String operation, QueryScope scope, boolean managers) {
        StringBuilder nameBuilder = new StringBuilder(operation);
        nameBuilder.append(".");
        if (scope != null) {
            nameBuilder.append(scope.name());
            nameBuilder.append(".");
        }
        if (managers) {
            nameBuilder.append("managers");
        } else {
            nameBuilder.append("members");
        }
        return nameBuilder.toString();
    }

    protected static <R extends AbstractRoleType> ObjectDelta getDeleteAssignmentDelta(R targetObject, Collection<QName> relations, Class classType, PrismContext prismContext) throws SchemaException {
        if (relations == null || relations.isEmpty()) {
            return prismContext.deltaFactory().object().createModificationDeleteContainer(classType, "fakeOid",
                    FocusType.F_ASSIGNMENT,
                    createAssignmentToModify(targetObject, null, prismContext));
        }

        ObjectDelta delta =  prismContext.deltaFactory().object().createEmptyModifyDelta(classType, "fakeOid"
        );

        for (QName relation : relations) {
            delta.addModificationDeleteContainer(FocusType.F_ASSIGNMENT, createAssignmentToModify(targetObject, relation, prismContext));
        }

        return delta;

    }

    protected static <R extends AbstractRoleType> ObjectDelta getDeleteParentOrgDelta(R targetObject,  Collection<QName> relations, PrismContext prismContext) throws SchemaException {
        if (relations == null || relations.isEmpty()) {
            return prismContext.deltaFactory().object().createModificationDeleteReference(ObjectType.class, "fakeOid",
                    ObjectType.F_PARENT_ORG_REF,
                    ObjectTypeUtil.createObjectRef(targetObject, prismContext).asReferenceValue());
        }

        ObjectDelta delta =  prismContext.deltaFactory().object().createEmptyModifyDelta(ObjectType.class, "fakeOid"
        );

        for (QName relation : relations) {
            delta.addModificationDeleteReference(ObjectType.F_PARENT_ORG_REF, MemberOperationsHelper.createReference(targetObject, relation).asReferenceValue());
        }

        return delta;

    }

    public static <R extends AbstractRoleType> AssignmentType createAssignmentToModify(R targetObject, QName relation, PrismContext prismContext) throws SchemaException {
        AssignmentType assignmentToModify = new AssignmentType();
        assignmentToModify.setTargetRef(createReference(targetObject, relation));
        prismContext.adopt(assignmentToModify);
        return assignmentToModify;
    }

    public static <R extends AbstractRoleType> ObjectReferenceType createReference(R targetObject, QName relation) {
        ObjectReferenceType ref = ObjectTypeUtil.createObjectRef(targetObject, relation);
        return ref;
    }

    protected static void executeMemberOperation(PageBase modelServiceLocator, Task operationalTask, QName type, ObjectQuery memberQuery,
            ExecuteScriptType script, Collection<SelectorOptions<GetOperationOptions>> option, AjaxRequestTarget target) {

        OperationResult parentResult = operationalTask.getResult();
        try {
            WebComponentUtil.executeMemberOperation(operationalTask, type, memberQuery, script, option, parentResult, modelServiceLocator);
        } catch (SchemaException e) {
            parentResult.recordFatalError(parentResult.getOperation(), e);
            LoggingUtils.logUnexpectedException(LOGGER,
                    "Failed to execute operation " + parentResult.getOperation(), e);
            target.add(modelServiceLocator.getFeedbackPanel());
        }

        target.add(modelServiceLocator.getFeedbackPanel());
    }

}
