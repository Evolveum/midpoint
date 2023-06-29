/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;
import static com.evolveum.midpoint.util.MiscUtil.argNonNull;

import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ResourceTaskCreator;

import jakarta.xml.bind.JAXBElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component.AbstractRoleMemberPanel.QueryScope;
import com.evolveum.midpoint.model.api.ActivitySubmissionOptions;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.task.ActivityDefinitionBuilder;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.AssignActionExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.DeleteActionExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.UnassignActionExpressionType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * Creates and optionally submits tasks for member operations on abstract roles.
 *
 * @see MemberOperationsGuiHelper
 * @see ResourceTaskCreator
 */
public abstract class MemberOperationsTaskCreator {

    // TODO why these discrepancies in operation key names?
    private static final String OP_KEY_UNASSIGN = "Remove";
    private static final String OP_KEY_ASSIGN = "Add";
    private static final String OP_KEY_DELETE = "delete";
    private static final String OP_KEY_RECOMPUTE = "Recompute";

    /** The target to which we relate the operations (add/remove members, recompute members, and so on). */
    @NotNull final AbstractRoleType targetAbstractRole;

    /** Just OID of {@link #targetAbstractRole}. */
    @NotNull final String targetAbstractRoleOid;

    @NotNull private final PageBase pageBase;

    /** Type of "member-like" objects on which the operation is to be carried out. */
    @NotNull private final QName memberType;

    /** Selection (query) to find related "member-like" objects to execute the operation on. */
    @NotNull final ObjectQuery memberQuery;

    MemberOperationsTaskCreator(
            @NotNull AbstractRoleType targetAbstractRole,
            @NotNull QName memberType,
            @NotNull ObjectQuery memberQuery,
            @NotNull PageBase pageBase) {
        this.targetAbstractRole = targetAbstractRole;
        this.targetAbstractRoleOid = argNonNull(
                targetAbstractRole.getOid(),
                "Target object (abstract role) without OID: %s", targetAbstractRole);
        this.memberType = memberType;
        this.memberQuery = memberQuery;
        this.pageBase = pageBase;
    }

    /** Operations that require a scope. */
    static abstract class Scoped extends MemberOperationsTaskCreator {

        /**
         * Auxiliary information: the scope of the operation on existing "members".
         * Just for the purposes of operation name determination. Must be consistent with {@link #memberQuery}.
         */
        @NotNull final QueryScope scope;

        Scoped(
                @NotNull AbstractRoleType targetAbstractRole, @NotNull QName memberType, @NotNull ObjectQuery memberQuery,
                @NotNull QueryScope scope, @NotNull PageBase pageBase) {
            super(targetAbstractRole, memberType, memberQuery, pageBase);
            this.scope = scope;
        }
    }

    /** Helps with "member unassign" operations. */
    static class Unassign extends Scoped {

        /** Relations to unassign. Not empty. Used to find assignments to be deleted on each of the members found. */
        @NotNull private final Collection<QName> relations;

        Unassign(
                @NotNull AbstractRoleType targetAbstractRole, @NotNull QName memberType, @NotNull ObjectQuery memberQuery,
                @NotNull QueryScope scope, @NotNull Collection<QName> relations, @NotNull PageBase pageBase) {
            super(targetAbstractRole, memberType, memberQuery, scope, pageBase);

            argCheck(!relations.isEmpty(), "No relations provided");
            this.relations = relations;
        }

        @Override
        public String getOperationKey() {
            return createOperationKey(OP_KEY_UNASSIGN, scope, "from");
        }

        /**
         * Creates and executes (i.e. submits) member unassign task: an iterative scripting task that un-assigns members
         * of a given abstract role.
         */
        void createAndSubmitTask(Task task, OperationResult result) throws CommonException {
            checkScriptingAuthorization(task, result);
            submitTask(
                    createUnassignMembersActivity(), task, result);
        }

        /** Creates the member unassignment task. */
        @NotNull PrismObject<TaskType> createTask(Task task, OperationResult result) throws CommonException {
            checkScriptingAuthorization(task, result);
            return createTask(
                    createUnassignMembersActivity(), task, result);
        }

        /** Creates an activity that un-assigns given role with given relations from provided objects. */
        @NotNull ActivityDefinitionType createUnassignMembersActivity() throws SchemaException {
            List<PrismReferenceValue> targetReferenceWithRelations = relations.stream()
                    .map(relation -> new ObjectReferenceType()
                            .oid(targetAbstractRoleOid)
                            .relation(relation)
                            .asReferenceValue())
                    .toList();

            // Filters relevant assignments - that are to be deleted - on each "assignee" (e.g. user) found.
            ObjectFilter assignmentFilter = PrismContext.get().queryFor(AssignmentType.class)
                    .item(AssignmentType.F_TARGET_REF)
                    .ref(targetReferenceWithRelations, true)
                    .buildFilter();

            SearchFilterType assignmentFilterBean;
            try {
                assignmentFilterBean = PrismContext.get().getQueryConverter().createSearchFilterType(assignmentFilter);
            } catch (SchemaException e) {
                throw new SystemException("Unexpected schema exception: " + e.getMessage(), e);
            }

            UnassignActionExpressionType unassignAction = new UnassignActionExpressionType()
                    .filter(assignmentFilterBean);

            var unassignmentRequest = new ExecuteScriptType()
                    .scriptingExpression(
                            new JAXBElement<>(
                                    SchemaConstantsGenerated.SC_UNASSIGN,
                                    UnassignActionExpressionType.class,
                                    unassignAction));

            return createIterativeScriptingActivity(unassignmentRequest);
        }
    }

    /** "Assign members" operation. */
    public static class Assign extends MemberOperationsTaskCreator {

        /** Relations with which to assign. */
        @NotNull private final QName relation;

        public Assign(
                @NotNull AbstractRoleType targetAbstractRole, @NotNull QName memberType, @NotNull ObjectQuery memberQuery,
                @NotNull PageBase pageBase, @NotNull QName relation) {
            super(targetAbstractRole, memberType, memberQuery, pageBase);
            this.relation = relation;
        }

        @Override
        public String getOperationKey() {
            return createOperationKey(OP_KEY_ASSIGN, null, "to");
        }

        /** Returns task OID */
        public String createAndSubmitTask(Task task, OperationResult result) throws CommonException {
            checkScriptingAuthorization(task, result);
            return submitTask(
                    createActivity(), task, result);
        }

        private ActivityDefinitionType createActivity() throws SchemaException {
            AssignActionExpressionType assignmentAction =
                    new AssignActionExpressionType()
                            .targetRef(
                                    ObjectTypeUtil.createObjectRef(targetAbstractRole, relation));

            var assignmentRequest = new ExecuteScriptType()
                    .scriptingExpression(
                            new JAXBElement<>(
                                    SchemaConstantsGenerated.SC_ASSIGN,
                                    AssignActionExpressionType.class,
                                    assignmentAction));

            return createIterativeScriptingActivity(assignmentRequest);
        }
    }

    /** "Delete members" operation. */
    static class Delete extends Scoped {

        Delete(
                @NotNull AbstractRoleType targetAbstractRole, @NotNull QName memberType, @NotNull ObjectQuery memberQuery,
                @NotNull QueryScope scope, @NotNull PageBase pageBase) {
            super(targetAbstractRole, memberType, memberQuery, scope, pageBase);
        }

        @Override
        public String getOperationKey() {
            return createOperationKey(OP_KEY_DELETE, scope, "of");
        }

        void createAndSubmitTask(Task task, OperationResult result) throws CommonException {
            checkScriptingAuthorization(task, result);
            submitTask(
                    createActivity(), task, result);
        }

        private ActivityDefinitionType createActivity() throws SchemaException {
            ExecuteScriptType deletionRequest = new ExecuteScriptType()
                    .scriptingExpression(
                            new JAXBElement<>(
                                    SchemaConstantsGenerated.SC_DELETE,
                                    DeleteActionExpressionType.class,
                                    new DeleteActionExpressionType()));

            return createIterativeScriptingActivity(deletionRequest);
        }
    }

    /** "Recompute members" operation. */
    static class Recompute extends Scoped {

        public Recompute(
                @NotNull AbstractRoleType targetAbstractRole, @NotNull QName memberType, @NotNull ObjectQuery memberQuery,
                @NotNull QueryScope scope, @NotNull PageBase pageBase) {
            super(targetAbstractRole, memberType, memberQuery, scope, pageBase);
        }

        @Override
        public String getOperationKey() {
            return createOperationKey(OP_KEY_RECOMPUTE, scope, "of");
        }

        /** Creates and submits a task that recomputes the role members. */
        void createAndSubmitTask(@NotNull Task task, @NotNull OperationResult result) throws CommonException {
            checkRecomputationAuthorization(task, result);
            submitTask(
                    createActivity(), task, result);
        }

        /** Creates a task that recomputes the role members. */
        PrismObject<TaskType> createTask(@NotNull Task task, @NotNull OperationResult result) throws CommonException {
            checkRecomputationAuthorization(task, result);
            return createTask(
                    createActivity(), task, result);
        }

        /**
         * Creates a task that recomputes the members. We do not use scripting for backwards-compatibility
         * reasons (this task does not require #executeScript authorization).
         *
         * The method does exist just for the sake of symmetry with
         * {@link #createIterativeScriptingActivity(ExecuteScriptType)}.
         */
        private ActivityDefinitionType createActivity() throws SchemaException {
            return new ActivityDefinitionType()
                    .work(new WorkDefinitionsType()
                            .recomputation(new RecomputationWorkDefinitionType()
                                    .objects(
                                            createObjectSetBean())));
        }
    }

    /** Creates iterative scripting activity for given script (and prescribed objects). */
    @NotNull ActivityDefinitionType createIterativeScriptingActivity(@NotNull ExecuteScriptType executionRequest)
            throws SchemaException {
        return ActivityDefinitionBuilder.create(new IterativeScriptingWorkDefinitionType()
                        .objects(
                                createObjectSetBean())
                        .scriptExecutionRequest(
                                executionRequest))
                .build();
    }

    ObjectSetType createObjectSetBean() throws SchemaException {
        return new ObjectSetType()
                .type(memberType)
                .query(PrismContext.get().getQueryConverter().createQueryType(memberQuery));
    }

    @NotNull String submitTask(ActivityDefinitionType activityDefinition, Task task, OperationResult result)
            throws CommonException {
        return pageBase.getModelInteractionService().submit(
                activityDefinition, createSubmissionOptions(), task, result);
    }

    @NotNull PrismObject<TaskType> createTask(ActivityDefinitionType activityDefinition, Task task, OperationResult result)
            throws CommonException {
        return pageBase.getModelInteractionService().createExecutionTask(
                        activityDefinition, createSubmissionOptions(), task, result)
                .asPrismObject();
    }

    private ActivitySubmissionOptions createSubmissionOptions() {
        return ActivitySubmissionOptions.create()
                .withTaskTemplate(new TaskType()
                        .name(WebComponentUtil.createPolyFromOrigString(
                                getOperationName())));
    }

    void checkScriptingAuthorization(Task task, OperationResult result) throws CommonException {
        pageBase.getSecurityEnforcer().authorize(ModelAuthorizationAction.EXECUTE_SCRIPT.getUrl(),
                null, AuthorizationParameters.EMPTY, null, task, result);
    }

    void checkRecomputationAuthorization(@NotNull Task task, @NotNull OperationResult result) throws CommonException {
        pageBase.getSecurityEnforcer().authorize(ModelAuthorizationAction.RECOMPUTE.getUrl(),
                null, AuthorizationParameters.EMPTY, null, task, result);
    }

    /** Converts {@link #getOperationKey()} to a resolved name. Used also as a task name. */
    public String getOperationName() {
        return pageBase
                .getString(
                        getOperationKey(),
                        WebComponentUtil.getDisplayNameOrName(targetAbstractRole.asPrismObject()));
    }

    /**
     * Returns a localization key for the operation - to be used in task name and in GUI messages.
     *
     * It has the form of `operation.OPERATION.SCOPE.members.PREPOSITION.TARGET-TYPE`.
     */
    public abstract String getOperationKey();

    String createOperationKey(String operation, @Nullable QueryScope scope, String preposition) {
        StringBuilder nameBuilder = new StringBuilder("operation.");
        nameBuilder.append(operation);
        nameBuilder.append(".");
        if (scope != null) {
            nameBuilder.append(scope.name());
            nameBuilder.append(".");
        }
        nameBuilder.append("members").append(".");
        nameBuilder.append(preposition != null ? preposition : "").append(preposition != null ? "." : "");
        String objectType = targetAbstractRole.getClass().getSimpleName();
        if (objectType.endsWith("Type")) {
            objectType = objectType.substring(0, objectType.indexOf("Type"));
        }
        nameBuilder.append(objectType);

        return nameBuilder.toString().toLowerCase();
    }
}
