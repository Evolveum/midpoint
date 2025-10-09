/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.security;

import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

/**
 * Migrates authorizations from a legacy format into a new one.
 *
 * For example, when going from 4.4/4.7 to 4.8, `#readOwnCertificationDecisions`
 * is replaced by `#read` on `AccessCertificationWorkItemType` objects.
 *
 * The migration occurs automatically when creating {@link Authorization} objects from assignments
 * by `TargetPayloadEvaluation` class.
 */
@SuppressWarnings("deprecation")
@Component
public class AuthorizationMigrator {

    private static final Map<String, ActionMigrator> MIGRATORS_MAP = Map.ofEntries(
            entry(
                    // from
                    ModelAuthorizationAction.READ_OWN_CERTIFICATION_DECISIONS.getUrl(),

                    // to
                    (migrated, original) -> {
                        add(migrated, original, readAssignedCertificationWorkItems());
                        add(migrated, original, readAssignedCertificationCasesExceptForForeignWorkItems());
                    }),

            entry(
                    // from
                    ModelAuthorizationAction.RECORD_CERTIFICATION_DECISION.getUrl(),

                    // to
                    (migrated, original) -> add(migrated, original, completeAssignedCertificationWorkItems())),

            entry(
                    // from (This action URL was checked for case management only.)
                    ModelAuthorizationAction.DELEGATE_OWN_WORK_ITEMS.getUrl(),

                    // to
                    (migrated, original) -> add(migrated, original, delegateAssignedCaseWorkItems())),

            entry(
                    // from (This action URL was checked for case management only.)
                    ModelAuthorizationAction.COMPLETE_ALL_WORK_ITEMS.getUrl(),

                    // to
                    (migrated, original) -> {
                        add(migrated, original, readAllCompletableCases());
                        add(migrated, original, completeAllCaseWorkItems());
                    }),

            entry(
                    // from (This action URL was checked fully for case management, and partially for certification.)
                    ModelAuthorizationAction.DELEGATE_ALL_WORK_ITEMS.getUrl(),

                    // to
                    (migrated, original) -> {
                        add(migrated, original, readAllCompletableCases());
                        add(migrated, original, readAllCertificationCases());
                        add(migrated, original, delegateAllWorkItems());
                    }),

            entry(
                    ModelAuthorizationAction.EXECUTE_SCRIPT.getUrl(),

                    // to
                    (migrated, original) -> {
                        add(migrated, original, new AuthorizationType()
                                .action(AuthorizationConstants.AUTZ_BULK_ALL_URL));
                    })
    );

    private static AuthorizationType readAllCertificationCases() {
        return new AuthorizationType()
                .action(ModelAuthorizationAction.READ.getUrl())
                .object(new OwnedObjectSelectorType()
                        .parent(certificationCaseParentSelector())
                        .type(AccessCertificationCaseType.COMPLEX_TYPE));
    }

    private static AuthorizationType readAllCompletableCases() {
        return new AuthorizationType()
                .action(ModelAuthorizationAction.READ.getUrl())
                .object(new OwnedObjectSelectorType()
                        .type(CaseType.COMPLEX_TYPE)
                        .archetypeRef(SystemObjectsType.ARCHETYPE_APPROVAL_CASE.value(), ArchetypeType.COMPLEX_TYPE)
                        .archetypeRef(SystemObjectsType.ARCHETYPE_MANUAL_CASE.value(), ArchetypeType.COMPLEX_TYPE)
                        .archetypeRef(SystemObjectsType.ARCHETYPE_CORRELATION_CASE.value(), ArchetypeType.COMPLEX_TYPE));
    }

    private static AuthorizationType readAssignedCertificationCasesExceptForForeignWorkItems() {
        return new AuthorizationType()
                .action(ModelAuthorizationAction.READ.getUrl())
                .object(new OwnedObjectSelectorType()
                        .parent(certificationCaseParentSelector())
                        .type(AccessCertificationCaseType.COMPLEX_TYPE)
                        .assignee(self()))
                .exceptItem(AccessCertificationCaseType.F_WORK_ITEM.toBean());
    }

    private static AuthorizationType readAssignedCertificationWorkItems() {
        return new AuthorizationType()
                .action(ModelAuthorizationAction.READ.getUrl())
                .object(new OwnedObjectSelectorType()
                        .parent(certificationWorkItemParentSelector())
                        .type(AccessCertificationWorkItemType.COMPLEX_TYPE)
                        .assignee(self()));
    }

    private static AuthorizationType completeAssignedCertificationWorkItems() {
        return new AuthorizationType()
                .action(ModelAuthorizationAction.COMPLETE_WORK_ITEM.getUrl())
                .object(new OwnedObjectSelectorType()
                        .parent(certificationWorkItemParentSelector())
                        .type(AccessCertificationWorkItemType.COMPLEX_TYPE)
                        .assignee(self()));
    }
    private static AuthorizationType completeAllCaseWorkItems() {
        return new AuthorizationType()
                .action(ModelAuthorizationAction.COMPLETE_WORK_ITEM.getUrl())
                .object(new OwnedObjectSelectorType()
                        .parent(caseWorkItemParentSelector())
                        .type(CaseWorkItemType.COMPLEX_TYPE));
    }

    private static AuthorizationType delegateAssignedCaseWorkItems() {
        return new AuthorizationType()
                .action(ModelAuthorizationAction.DELEGATE_WORK_ITEM.getUrl())
                .object(new OwnedObjectSelectorType()
                        .parent(caseWorkItemParentSelector())
                        .type(CaseWorkItemType.COMPLEX_TYPE)
                        .assignee(self()));
    }

    private static AuthorizationType delegateAllWorkItems() {
        return new AuthorizationType()
                .action(ModelAuthorizationAction.DELEGATE_WORK_ITEM.getUrl())
                .object(new OwnedObjectSelectorType()
                        .parent(caseWorkItemParentSelector())
                        .type(CaseWorkItemType.COMPLEX_TYPE))
                .object(new OwnedObjectSelectorType()
                        .parent(certificationWorkItemParentSelector())
                        .type(AccessCertificationWorkItemType.COMPLEX_TYPE));
    }

    private static void add(List<AuthorizationType> migrated, AuthorizationType original, AuthorizationType newOne) {
        migrated.add(newOne
                .decision(original.getDecision())
                .phase(original.getPhase())
                .name(newName(original.getName()))
                .limitations(CloneUtil.clone(original.getLimitations())));
    }

    public @NotNull Collection<AuthorizationType> migrate(@NotNull AuthorizationType original) {
        List<AuthorizationType> migrated = new ArrayList<>();
        migrated.add(original);

        List<String> actions = original.getAction();
        for (Map.Entry<String, ActionMigrator> entry : MIGRATORS_MAP.entrySet()) {
            if (actions.contains(entry.getKey())) {
                entry.getValue().migrate(migrated, original);
            }
        }

        return migrated;
    }

    private static SubjectedObjectSelectorType self() {
        return new SubjectedObjectSelectorType()
                .special(SpecialObjectSpecificationType.SELF);
    }

    private static ObjectParentSelectorType certificationWorkItemParentSelector() {
        return new ObjectParentSelectorType()
                .type(AccessCertificationCampaignType.COMPLEX_TYPE)
                .path(new ItemPathType(
                        ItemPath.create(AccessCertificationCampaignType.F_CASE, AccessCertificationCaseType.F_WORK_ITEM)));
    }

    private static ObjectParentSelectorType certificationCaseParentSelector() {
        return new ObjectParentSelectorType()
                .type(AccessCertificationCampaignType.COMPLEX_TYPE)
                .path(new ItemPathType(AccessCertificationCampaignType.F_CASE));
    }

    private static ObjectParentSelectorType caseWorkItemParentSelector() {
        return new ObjectParentSelectorType()
                .type(CaseType.COMPLEX_TYPE)
                .path(new ItemPathType(CaseType.F_WORK_ITEM));
    }

    private static String newName(String name) {
        if (name == null) {
            return "migrated";
        } else {
            return name + " (migrated)";
        }
    }

    private interface ActionMigrator {
        void migrate(List<AuthorizationType> migrated, AuthorizationType original);
    }
}
