/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.tasks;

import static com.evolveum.midpoint.model.api.ModelExecuteOptions.fromModelExecutionOptionsType;
import static com.evolveum.midpoint.schema.GetOperationOptions.readOnly;
import static com.evolveum.midpoint.util.MiscUtil.configNonNull;

import java.util.List;
import java.util.Set;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.impl.tasks.simple.SimpleActivityHandler;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.common.activity.definition.AbstractWorkDefinition;
import com.evolveum.midpoint.repo.common.activity.definition.ObjectSetSpecificationProvider;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory.WorkDefinitionInfo;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinitionFactory.WorkDefinitionSupplier;
import com.evolveum.midpoint.repo.common.activity.run.ActivityReportingCharacteristics;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.SearchBasedActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.roles.RoleManagementUtil;
import com.evolveum.midpoint.schema.util.task.work.ObjectSetUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Specific activities related to role membership management, supporting e.g. the transition from application to business roles.
 *
 * Currently, this handler supports:
 *
 * . assigning specified {@link MyWorkDefinition#roleRef} to prospective {@link MyWorkDefinition#members};
 * . unassigning roles induced by that role from the members.
 */
@Component
public class RoleMembershipManagementActivityHandler
        extends SimpleActivityHandler<
            AssignmentHolderType,
            RoleMembershipManagementActivityHandler.MyWorkDefinition,
        RoleMembershipManagementActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(RoleMembershipManagementActivityHandler.class);

    @Override
    protected @NotNull QName getWorkDefinitionTypeName() {
        return RoleMembershipManagementWorkDefinitionType.COMPLEX_TYPE;
    }

    @Override
    protected @NotNull QName getWorkDefinitionItemName() {
        return WorkDefinitionsType.F_ROLE_MEMBERSHIP_MANAGEMENT;
    }

    @Override
    protected @NotNull Class<MyWorkDefinition> getWorkDefinitionClass() {
        return MyWorkDefinition.class;
    }

    @Override
    protected @NotNull WorkDefinitionSupplier getWorkDefinitionSupplier() {
        return MyWorkDefinition::new;
    }

    @Override
    protected @NotNull ExecutionSupplier<AssignmentHolderType, MyWorkDefinition, RoleMembershipManagementActivityHandler> getExecutionSupplier() {
        return MyRun::new;
    }

    @Override
    public String getDefaultArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_UTILITY_TASK.value();
    }

    @Override
    protected @NotNull String getShortName() {
        return "Role membership management";
    }

    @Override
    public String getIdentifierPrefix() {
        return "role-membership-management";
    }

    final class MyRun extends
            SearchBasedActivityRun<
                    AssignmentHolderType,
                    MyWorkDefinition,
                    RoleMembershipManagementActivityHandler,
                    AbstractActivityWorkStateType> {

        /**
         * The role whose membership we manage - currently, we mean to assign this role to prospective members.
         * Never null after initialization.
         */
        private AbstractRoleType role;

        /**
         * Roles which are induced by {@link #role}; we want to unassign these from prospective members.
         * Never null after initialization.
         */
        private Set<String> inducedRolesOids;

        MyRun(@NotNull ActivityRunInstantiationContext<MyWorkDefinition, RoleMembershipManagementActivityHandler> context,
                String shortName) {
            super(context, shortName);
            setInstanceReady();
        }

        @Override
        public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
            return super.createReportingCharacteristics()
                    .actionsExecutedStatisticsSupported(true);
        }

        @Override
        public void beforeRun(OperationResult result) throws CommonException {
            ensureNoDryRun();
            role = modelObjectResolver.resolve(
                    getWorkDefinition().roleRef,
                    AbstractRoleType.class,
                    readOnly(),
                    "roleRef resolution",
                    getRunningTask(), result);
            inducedRolesOids = RoleManagementUtil.getInducedRolesOids(role);
        }

        @Override
        public boolean processItem(
                @NotNull AssignmentHolderType member,
                @NotNull ItemProcessingRequest<AssignmentHolderType> request,
                RunningTask workerTask,
                OperationResult result)
                throws CommonException {

            LOGGER.debug("Processing [prospective] member {}", member);

            AssignmentType assignmentToAdd = ObjectTypeUtil.createAssignmentTo(role.asPrismObject());
            List<AssignmentType> assignmentsToDelete =
                    CloneUtil.cloneCollectionMembers(
                            RoleManagementUtil.getMatchingAssignments(member.getAssignment(), inducedRolesOids));

            ObjectDelta<AssignmentHolderType> memberDelta = prismContext
                    .deltaFor(AssignmentHolderType.class)
                    .item(AssignmentHolderType.F_ASSIGNMENT)
                    .deleteRealValues(assignmentsToDelete)
                    .add(assignmentToAdd)
                    .asObjectDelta(member.getOid());

            LOGGER.trace("Delta to execute:\n{}", memberDelta.debugDumpLazily(1));

            modelService.executeChanges(
                    List.of(memberDelta),
                    getWorkDefinition().executionOptions,
                    workerTask,
                    result);

            LOGGER.debug("Processing [prospective] member done: {}", member);

            return true;
        }
    }

    public static class MyWorkDefinition extends AbstractWorkDefinition implements ObjectSetSpecificationProvider {

        @NotNull private final ObjectReferenceType roleRef;
        @NotNull private final ObjectSetType members;
        @Nullable private final ModelExecuteOptions executionOptions;

        MyWorkDefinition(@NotNull WorkDefinitionInfo info) throws ConfigurationException {
            super(info);
            var typedDefinition = (RoleMembershipManagementWorkDefinitionType) info.getBean();
            roleRef = configNonNull(typedDefinition.getRoleRef(), "No roleRef in work definition in %s", info.origin());
            members = ObjectSetUtil.emptyIfNull(typedDefinition.getMembers());
            ObjectSetUtil.applyDefaultObjectType(members, AssignmentHolderType.COMPLEX_TYPE);
            executionOptions = fromModelExecutionOptionsType(typedDefinition.getExecutionOptions());
        }

        @Override
        public @NotNull ObjectSetType getObjectSetSpecification() {
            return members;
        }

        @Override
        protected void debugDumpContent(StringBuilder sb, int indent) {
            DebugUtil.debugDumpWithLabelLn(sb, "roleRef", String.valueOf(roleRef), indent+1);
            DebugUtil.debugDumpWithLabelLn(sb, "members", members, indent+1);
            DebugUtil.debugDumpWithLabel(sb, "executionOptions", String.valueOf(executionOptions), indent+1);
        }
    }
}
