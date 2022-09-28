/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component;

import java.util.*;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component.AbstractRoleMemberPanel.QueryScope;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.util.GetOperationOptionsUtil;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.*;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntry;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

/**
 * Creates and optionally submits tasks for member operations on abstract roles. Does not include pure GUI aspects.
 *
 * @see MemberOperationsGuiHelper
 */
public class MemberOperationsHelper {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractRoleMemberPanel.class);

    // TODO why these discrepancies in operation key names?
    private static final String OP_KEY_UNASSIGN = "Remove";
    private static final String OP_KEY_ASSIGN = "Add";
    private static final String OP_KEY_DELETE = "delete";
    private static final String OP_KEY_RECOMPUTE = "Recompute";

    private static final String OP_SUBMIT_TASK = MemberOperationsHelper.class.getName() + ".submitMemberOperationTask";

    //region Unassigning members

    /**
     * Creates and executes (i.e. submits) member unassign task: an iterative scripting task that un-assigns members
     * of a given abstract role
     *
     * @param targetObject Role whose members are to be unassigned
     * @param scope What members should be processed (selected / all / all deeply), seemingly used only for task name creation
     * @param memberType Type of members to be processed
     * @param memberQuery Query selecting members that are to be processed
     * @param relations Relations to unassign. Not null, not empty.
     *
     */
    public static void createAndSubmitUnassignMembersTask(AbstractRoleType targetObject, QueryScope scope, QName memberType,
            ObjectQuery memberQuery, Collection<QName> relations, AjaxRequestTarget target, PageBase pageBase) {
        Task task = createUnassignMembersTask(targetObject, scope, memberType, memberQuery, relations, target, pageBase);
        submitTaskIfPossible(task, target, pageBase);
    }

    /**
     * Creates the member unassignment task.
     *
     * @see #createAndSubmitUnassignMembersTask(AbstractRoleType, QueryScope, QName, ObjectQuery, Collection,
     * AjaxRequestTarget, PageBase)
     *
     * @return null if there's an error; the error is shown in such a case
     */
    public static @Nullable Task createUnassignMembersTask(AbstractRoleType targetObject, QueryScope scope, QName memberType,
            ObjectQuery memberQuery, @NotNull Collection<QName> relations, AjaxRequestTarget target, PageBase pageBase) {

        String targetOid = targetObject.getOid();

        argCheck(!relations.isEmpty(), "No relations provided");
        argCheck(targetOid != null, "Target object without OID: %s", targetObject);

        ExecuteScriptType script = createUnassignBulkAction(targetOid, relations);

        String operationKey = getOperationKey(OP_KEY_UNASSIGN, scope, targetObject, "from");
        PolyStringType taskName = createTaskNameFromKey(operationKey, targetObject, pageBase);

        return createScriptingMemberOperationTask(
                new MemberOpTaskSpec(
                        getOperationName(taskName),
                        taskName,
                        memberType,
                        memberQuery,
                        null),
                script,
                pageBase, target);
    }

    /** Creates a bulk action (script) that un-assigns given role with given relations from provided objects. */
    @NotNull
    private static ExecuteScriptType createUnassignBulkAction(@NotNull String targetOid, @NotNull Collection<QName> relations) {
        List<PrismReferenceValue> matchingTargetReferences = relations.stream()
                .map(relation -> new ObjectReferenceType()
                        .oid(targetOid)
                        .relation(relation)
                        .asReferenceValue())
                .collect(Collectors.toList());

        ObjectFilter assignmentFilter = PrismContext.get().queryFor(AssignmentType.class)
                .item(AssignmentType.F_TARGET_REF)
                .ref(matchingTargetReferences, true)
                .buildFilter();

        SearchFilterType assignmentFilterBean;
        try {
            assignmentFilterBean = PrismContext.get().getQueryConverter().createSearchFilterType(assignmentFilter);
        } catch (SchemaException e) {
            throw new SystemException("Unexpected schema exception: " + e.getMessage(), e);
        }

        UnassignActionExpressionType unassignAction = new UnassignActionExpressionType()
                .filter(assignmentFilterBean);

        ExecuteScriptType script = new ExecuteScriptType();
        script.setScriptingExpression(
                new JAXBElement<>(SchemaConstantsGenerated.SC_UNASSIGN, UnassignActionExpressionType.class, unassignAction));
        return script;
    }
    //endregion

    //region Assigning members
    public static void createAndSubmitAssignMembersTask(AbstractRoleType targetObject, QName memberType, ObjectQuery memberQuery,
            @NotNull QName relation, AjaxRequestTarget target, PageBase pageBase) {

        String targetOid = targetObject.getOid();
        argCheck(targetOid != null, "Target object without OID: %s", targetObject);

        ExecuteScriptType script = createAssignBulkAction(targetObject, relation);

        String operationKey = getOperationKey(OP_KEY_ASSIGN, null, targetObject, "to");
        PolyStringType taskName = createTaskNameFromKey(operationKey, targetObject, pageBase);

        Task task = createScriptingMemberOperationTask(
                new MemberOpTaskSpec(
                        getOperationName(taskName),
                        taskName,
                        memberType,
                        memberQuery,
                        null),
                script,
                pageBase, target);

        submitTaskIfPossible(task, target, pageBase);
    }

    /** Creates a bulk action (script) that assigns given role with given relation to an input object. */
    private static @NotNull ExecuteScriptType createAssignBulkAction(AbstractRoleType targetObject, @NotNull QName relation) {
        AssignActionExpressionType assignAction = new AssignActionExpressionType()
                .targetRef(ObjectTypeUtil.createObjectRef(targetObject, relation));

        ExecuteScriptType script = new ExecuteScriptType();
        script.setScriptingExpression(
                new JAXBElement<>(SchemaConstantsGenerated.SC_ASSIGN, AssignActionExpressionType.class, assignAction));
        return script;
    }
    //endregion

    //region Deleting members
    public static void createAndSubmitDeleteMembersTask(AbstractRoleType targetObject, QueryScope scope, ObjectQuery memberQuery,
            AjaxRequestTarget target, PageBase pageBase) {

        ExecuteScriptType script = createDeleteBulkAction();

        String operationKey = getOperationKey(OP_KEY_DELETE, scope, targetObject, "of");
        PolyStringType taskName = createTaskNameFromKey(operationKey, targetObject, pageBase);

        Task task = createScriptingMemberOperationTask(
                new MemberOpTaskSpec(
                        getOperationName(taskName),
                        taskName,
                        AssignmentHolderType.COMPLEX_TYPE,
                        memberQuery,
                        null),
                script,
                pageBase, target);

        submitTaskIfPossible(task, target, pageBase);
    }

    @NotNull
    private static ExecuteScriptType createDeleteBulkAction() {
        ExecuteScriptType script = new ExecuteScriptType();
        DeleteActionExpressionType deleteAction = new DeleteActionExpressionType();
        script.setScriptingExpression(
                new JAXBElement<>(SchemaConstantsGenerated.SC_DELETE, DeleteActionExpressionType.class, deleteAction));
        return script;
    }
    //endregion

    //region Recomputing members
    /** Creates and submits a task that recomputes the role members. */
    public static void createAndSubmitRecomputeMembersTask(AbstractRoleType targetObject, QueryScope scope,
            ObjectQuery memberQuery, AjaxRequestTarget target, PageBase pageBase) {
        Task task = createRecomputeMembersTask(targetObject, scope, memberQuery, target, pageBase);
        submitTaskIfPossible(task, target, pageBase);
    }

    /** Creates a task that recomputes the role members. */
    public static Task createRecomputeMembersTask(AbstractRoleType targetObject, QueryScope scope, ObjectQuery memberQuery,
            AjaxRequestTarget target, PageBase pageBase) {

        String operationKey = getOperationKey(OP_KEY_RECOMPUTE, scope, targetObject, "of");
        PolyStringType taskName = createTaskNameFromKey(operationKey, targetObject, pageBase);

        return createRecomputeMembersTask(
                new MemberOpTaskSpec(
                        getOperationName(taskName),
                        taskName,
                        AssignmentHolderType.COMPLEX_TYPE,
                        memberQuery,
                        null),
                target, pageBase);
    }
    //endregion

    //region Query formulation
    /**
     * Creates a query covering all direct (assigned) members of an abstract role.
     *
     * @param targetObject The role.
     * @param memberType Type of members to be looked for.
     * @param relations Relations (of member->target assignment) to be looked for.
     * Should not be empty (although it is not guaranteed now).
     * @param tenant Tenant to be looked for (assignment/tenantRef)
     * @param project Org to be looked for (assignment/orgRef)
     */
    public static <R extends AbstractRoleType> @NotNull ObjectQuery createDirectMemberQuery(R targetObject,
            @NotNull QName memberType, Collection<QName> relations, ObjectReferenceType tenant, ObjectReferenceType project) {
        // We assume tenantRef.relation and orgRef.relation are always default ones (see also MID-3581)
        S_FilterEntry q0 = PrismContext.get().queryFor(AssignmentHolderType.class);
        if (!AssignmentHolderType.COMPLEX_TYPE.equals(memberType)) {
            q0 = q0.type(memberType);
        }

        // Use exists filter to build a query like this:
        // $a/targetRef = oid1 and $a/tenantRef = oid2 and $a/orgRef = oid3
        S_FilterExit q = q0.exists(AssignmentHolderType.F_ASSIGNMENT)
                .block()
                .item(AssignmentType.F_TARGET_REF)
                .ref(createReferenceValuesList(targetObject, relations));

        if (tenant != null && StringUtils.isNotEmpty(tenant.getOid())) {
            q = q.and().item(AssignmentType.F_TENANT_REF).ref(tenant.getOid());
        }

        if (project != null && StringUtils.isNotEmpty(project.getOid())) {
            q = q.and().item(AssignmentType.F_ORG_REF).ref(project.getOid());
        }

        ObjectQuery query = q.endBlock().build();
        LOGGER.trace("Searching members of role {} with query:\n{}", targetObject.getOid(), query.debugDumpLazily());
        return query;
    }

    /**
     * Creates reference values pointing to given target with given relations.
     *
     * @param relations The relations. Must be at least one, otherwise the resulting list (to be used in a query, presumably)
     * will be empty, making the query wrong.
     */
    public static @NotNull List<PrismReferenceValue> createReferenceValuesList(@NotNull AbstractRoleType targetObject,
            @NotNull Collection<QName> relations) {
        argCheck(!relations.isEmpty(), "At least one relation must be specified");
        return relations.stream()
                .map(relation -> ObjectTypeUtil.createObjectRef(targetObject, relation).asReferenceValue())
                .collect(Collectors.toList());
    }

    /**
     * Creates reference values pointing to given target with given relations.
     *
     * @param relations The relations. Must be at least one, otherwise the resulting list (to be used in a query, presumably)
     * will be empty, making the query wrong.
     */
    public static @NotNull List<PrismReferenceValue> createReferenceValuesList(@NotNull ObjectReferenceType targetObjectRef,
            @NotNull Collection<QName> relations) {
        argCheck(!relations.isEmpty(), "At least one relation must be specified");
        return relations.stream()
                .map(relation -> targetObjectRef.relation(relation).asReferenceValue().clone())
                .collect(Collectors.toList());
    }

    /**
     * Creates a query covering all selected objects (converts list of objects to a multivalued "OID" query).
     */
    public static @NotNull ObjectQuery createSelectedObjectsQuery(@NotNull List<? extends ObjectType> selectedObjects) {
        Set<String> oids = new HashSet<>(ObjectTypeUtil.getOids(selectedObjects));
        return PrismContext.get().queryFor(AssignmentHolderType.class)
                .id(oids.toArray(new String[0]))
                .build();
    }
    //endregion

    //region Task preparation and submission for execution
    /**
     * Creates a localization key for task name in the form of `operation.OPERATION.SCOPE.members.PREPOSITION.TARGET-TYPE`.
     * It is also the operation name for tasks being created.
     */
    private static <R extends AbstractRoleType> String getOperationKey(String operation, QueryScope scope,
            R targetObject, String preposition) {
        StringBuilder nameBuilder = new StringBuilder("operation.");
        nameBuilder.append(operation);
        nameBuilder.append(".");
        if (scope != null) {
            nameBuilder.append(scope.name());
            nameBuilder.append(".");
        }
        nameBuilder.append("members").append(".");
        if (targetObject != null) {
            nameBuilder.append(preposition != null ? preposition : "").append(preposition != null ? "." : "");
            String objectType = targetObject.getClass().getSimpleName();
            if (objectType.endsWith("Type")) {
                objectType = objectType.substring(0, objectType.indexOf("Type"));
            }
            nameBuilder.append(objectType);
        }
        return nameBuilder.toString().toLowerCase();
    }

    /** Creates a task name from the localization key. */
    private static @NotNull PolyStringType createTaskNameFromKey(String operationKey, AbstractRoleType targetObject,
            PageBase pageBase) {
        String nameOrig = pageBase
                .getString(operationKey, WebComponentUtil.getDisplayNameOrName(targetObject.asPrismObject()));
        return WebComponentUtil.createPolyFromOrigString(nameOrig);
    }

    /**
     * Creates a task that will execute given script on all abstract role members.
     *
     * @param taskSpec Specification of the member operation task
     * @param script Script to be executed on individual members
     *
     * @return null if there's an error
     */
    private static @Nullable Task createScriptingMemberOperationTask(@NotNull MemberOpTaskSpec taskSpec,
            @NotNull ExecuteScriptType script, PageBase pageBase, AjaxRequestTarget target) {

        try {

            // @formatter:off
            ActivityDefinitionType activityDefinition = new ActivityDefinitionType(PrismContext.get())
                    .beginWork()
                        .beginIterativeScripting()
                            .objects(createObjectSetBean(taskSpec))
                            .scriptExecutionRequest(script)
                        .<WorkDefinitionsType>end()
                    .end();
            // @formatter:on

            Task task = createTaskForActivity(taskSpec, activityDefinition, pageBase);

            // Must be executed after task is created (to have task + its operation result)
            pageBase.getSecurityEnforcer().authorize(ModelAuthorizationAction.EXECUTE_SCRIPT.getUrl(),
                    null, AuthorizationParameters.EMPTY, null, task, task.getResult());

            return task;

        } catch (Exception e) {
            processTaskCreationException(e, taskSpec.operationName, target, pageBase);
            return null;
        }
    }

    private static ObjectSetType createObjectSetBean(@NotNull MemberOpTaskSpec taskSpec) throws SchemaException {
        return new ObjectSetType(PrismContext.get())
                .type(taskSpec.memberType)
                .query(PrismContext.get().getQueryConverter().createQueryType(taskSpec.memberQuery))
                .searchOptions(GetOperationOptionsUtil.optionsToOptionsBeanNullable(taskSpec.options));
    }

    /**
     * Creates a task that recomputes the members. We do not use scripting for backwards-compatibility
     * reasons (this task does not require #executeScript authorization).
     *
     * The method does exist just for the sake of symmetry with {@link #createScriptingMemberOperationTask(MemberOpTaskSpec,
     * ExecuteScriptType, PageBase, AjaxRequestTarget)}.
     *
     * @param taskSpec Specification of the member operation task
     *
     * @return null if there's an error
     */
    @SuppressWarnings("SameParameterValue")
    private static @Nullable Task createRecomputeMembersTask(@NotNull MemberOpTaskSpec taskSpec,
            AjaxRequestTarget target, PageBase pageBase) {

        try {

            // @formatter:off
            ActivityDefinitionType activityDefinition = new ActivityDefinitionType(PrismContext.get())
                    .beginWork()
                        .beginRecomputation()
                            .objects(createObjectSetBean(taskSpec))
                        .<WorkDefinitionsType>end()
                    .end();
            // @formatter:on

            Task task = createTaskForActivity(taskSpec, activityDefinition, pageBase);

            // Must be executed after task is created (to have task + its operation result)
            pageBase.getSecurityEnforcer().authorize(ModelAuthorizationAction.RECOMPUTE.getUrl(),
                    null, AuthorizationParameters.EMPTY, null, task, task.getResult());

            return task;

        } catch (Exception e) {
            processTaskCreationException(e, taskSpec.operationName, target, pageBase);
            return null;
        }
    }

    private static void processTaskCreationException(@NotNull Exception e, @NotNull String operationKey,
            AjaxRequestTarget target, PageBase pageBase) {
        LoggingUtils.logUnexpectedException(LOGGER, "Couldn't create member processing task", e);
        OperationResult dummyResult = new OperationResult(operationKey);
        dummyResult.recordFatalError(pageBase.getString("WebComponentUtil.message.startPerformed.fatalError.createTask"), e);
        pageBase.showResult(dummyResult);
        target.add(pageBase.getFeedbackPanel());
    }

    /**
     * Creates a task that will execute given script on all abstract role members.
     *
     * @param taskSpec Specification of the member operation task
     * @param activityDefinition Activity that should be put into the task
     */
    private static @NotNull Task createTaskForActivity(@NotNull MemberOpTaskSpec taskSpec,
            @NotNull ActivityDefinitionType activityDefinition, @NotNull PageBase pageBase) throws SchemaException {

        Task task = pageBase.createSimpleTask(taskSpec.operationName);
        task.setName(taskSpec.taskName);
        MidPointPrincipal owner = AuthUtil.getPrincipalUser();
        task.setOwner(owner.getFocus().asPrismObject());
        task.setInitiallyRunnable();
        task.setThreadStopAction(ThreadStopActionType.RESTART);
        task.setSchedule(
                new ScheduleType(PrismContext.get())
                        .misfireAction(MisfireActionType.EXECUTE_IMMEDIATELY));
        task.setRootActivityDefinition(activityDefinition);
        return task;
    }

    private static void submitTaskIfPossible(@Nullable Task task, AjaxRequestTarget target, PageBase pageBase) {
        if (task != null) {
            OperationResult taskResult = task.getResult();
            OperationResult result = taskResult.createSubresult(OP_SUBMIT_TASK);
            try {
                pageBase.getModelInteractionService().switchToBackground(task, result);
            } catch (Throwable t) {
                result.recordFatalError(t);
                throw t;
            } finally {
                result.close();
            }
            taskResult.setInProgress();
            taskResult.setBackgroundTaskOid(task.getOid());
            pageBase.showResult(taskResult);
            target.add(pageBase.getFeedbackPanel());
        } else {
            // we assume there's an error shown in the feedback panel
        }
    }
    //endregion

    /**
     * This is a hack. Normally, operation names in operation result should be the localization keys. In this class,
     * however, the keys (like `operation.recompute.all_direct.members.of.org`) expect a parameter to be resolved.
     * So we use the translated names instead.
     */
    private static String getOperationName(PolyStringType taskName) {
        return taskName.getOrig();
    }

    /** Specification of member operation. Created to avoid lengthy parameters lists. */
    private static class MemberOpTaskSpec {
        @NotNull private final String operationName;
        @NotNull private final PolyStringType taskName;
        @NotNull private final QName memberType;
        @Nullable private final ObjectQuery memberQuery;
        @Nullable private final Collection<SelectorOptions<GetOperationOptions>> options;

        private MemberOpTaskSpec(@NotNull String operationName, @NotNull PolyStringType taskName, @NotNull QName memberType,
                @Nullable ObjectQuery memberQuery, @Nullable Collection<SelectorOptions<GetOperationOptions>> options) {
            this.operationName = operationName;
            this.taskName = taskName;
            this.memberType = memberType;
            this.memberQuery = memberQuery;
            this.options = options;
        }
    }
}
