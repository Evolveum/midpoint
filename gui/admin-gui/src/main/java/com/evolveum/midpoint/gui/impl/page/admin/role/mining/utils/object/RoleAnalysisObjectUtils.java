package com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.object;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.LOGGER;

import java.util.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component.AbstractRoleMemberPanel;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component.MemberOperationsTaskCreator;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.RelationTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class RoleAnalysisObjectUtils {

    public static void memberOperationsTaskAssignCreator(@NotNull AjaxRequestTarget target,
            @NotNull PrismObject<RoleType> rolePrismObject,
            @NotNull PageBase pageBase,
            List<String> selectedObjectsList,
            @NotNull Component feedBackPanel) {
        var taskCreator = new MemberOperationsTaskCreator.Assign(
                rolePrismObject.asObjectable(),
                UserType.COMPLEX_TYPE,
                createInOidQuery(selectedObjectsList, pageBase),
                pageBase,
                RelationTypes.MEMBER.getRelation());

        pageBase.taskAwareExecutor(target, taskCreator.getOperationName())
                .withOpResultOptions(OpResult.Options.create()
                        .withHideTaskLinks(false))
                .withCustomFeedbackPanel(feedBackPanel)
                .run(taskCreator::createAndSubmitTask);
    }

    public static void memberOperationsTaskUnassignedCreator(@NotNull AjaxRequestTarget target,
            @NotNull PrismObject<RoleType> rolePrismObject,
            @NotNull PageBase pageBase,
            List<String> selectedObjectsList,
            @NotNull Component feedBackPanel) {

        var taskCreator = new MemberOperationsTaskCreator.Unassign(
                rolePrismObject.asObjectable(),
                UserType.COMPLEX_TYPE,
                createInOidQuery(selectedObjectsList, pageBase),
                AbstractRoleMemberPanel.QueryScope.ALL_DIRECT,
                Collections.singleton(RelationTypes.MEMBER.getRelation()),
                pageBase);

        pageBase.taskAwareExecutor(target, taskCreator.getOperationName())
                .withOpResultOptions(OpResult.Options.create()
                        .withHideTaskLinks(false))
                .withCustomFeedbackPanel(feedBackPanel)
                .runVoid(taskCreator::createAndSubmitTask);
    }

    public static ObjectQuery createInOidQuery(List<String> selectedObjectsList, @NotNull PageBase pageBase) {
        PrismContext prismContext = pageBase.getPrismContext();
        InOidFilter inOid = prismContext.queryFactory().createInOid(
                selectedObjectsList != null ? selectedObjectsList : Collections.emptyList());
        return prismContext.queryFactory().createQuery(inOid);
    }

    public static void resolveMembersOperation(Set<ObjectReferenceType> candidateMembers,
            @NotNull Map<String, PrismObject<UserType>> userExistCache,
            @NotNull List<String> userToAssign,
            @NotNull List<String> userToUnassigned) {

        candidateMembers.forEach(member -> {
            PrismObject<UserType> userPrismObject = userExistCache.get(member.getOid());
            if (userPrismObject == null && member.getOid() != null) {
                userToAssign.add(member.getOid());
            } else {
                userExistCache.remove(member.getOid());
            }
        });

        userExistCache.values().stream().map(userObject -> userObject.asObjectable()).forEach(user -> {
            userToUnassigned.add(user.getOid());
        });
    }

    public static void executeChangesOnCandidateRole(
            @NotNull RoleAnalysisService roleAnalysisService,
            @NotNull PageBase pageBase,
            @NotNull AjaxRequestTarget target,
            @NotNull PrismObject<RoleAnalysisClusterType> cluster,
            @NotNull List<RoleAnalysisCandidateRoleType> candidateRole,
            Set<ObjectReferenceType> candidateMembers,
            @NotNull Set<AssignmentType> candidateInducements,
            @NotNull Task task,
            @NotNull OperationResult result) {
        RoleAnalysisCandidateRoleType roleAnalysisCandidateRoleType = candidateRole.get(0);
        pageBase.getRoleAnalysisService().executeChangesOnCandidateRole(cluster,
                roleAnalysisCandidateRoleType,
                candidateMembers,
                candidateInducements, task, result);

        ObjectReferenceType candidateRoleRef = roleAnalysisCandidateRoleType.getCandidateRoleRef();
        PrismObject<RoleType> rolePrismObject = roleAnalysisService.getRoleTypeObject(
                candidateRoleRef.getOid(), task, result);
        if (rolePrismObject == null) {
            LOGGER.error("Couldn't get candidate role object{}", candidateRoleRef.getOid());
            return;
        }

        Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
        roleAnalysisService.extractUserTypeMembers(userExistCache,
                null,
                Collections.singleton(rolePrismObject.getOid()),
                task,
                result);

        List<String> userToAssign = new ArrayList<>();
        List<String> userToUnassigned = new ArrayList<>();
        resolveMembersOperation(candidateMembers, userExistCache, userToAssign, userToUnassigned);

        if (!userToAssign.isEmpty()) {
            memberOperationsTaskAssignCreator(target, rolePrismObject, pageBase, userToAssign,
                    pageBase.getFeedbackPanel());
        }

        if (!userToUnassigned.isEmpty()) {
            memberOperationsTaskUnassignedCreator(target, rolePrismObject, pageBase, userToUnassigned,
                    pageBase.getFeedbackPanel());
        }
    }
}
