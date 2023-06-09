/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.security;

import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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
 */
@SuppressWarnings("deprecation")
@Component
public class AuthorizationMigrator {

    private static final Map<String, ActionMigrator> MIGRATORS_MAP = Map.ofEntries(
            entry(
                    ModelAuthorizationAction.READ_OWN_CERTIFICATION_DECISIONS.getUrl(),
                    (migrated, original) -> {
                        add(migrated, original, new AuthorizationType()
                                .action(ModelAuthorizationAction.READ.getUrl())
                                .object(new AuthorizationObjectSelectorType()
                                        .type(AccessCertificationWorkItemType.COMPLEX_TYPE)
                                        .assignee(self())));
                        add(migrated, original, new AuthorizationType()
                                .action(ModelAuthorizationAction.READ.getUrl())
                                .object(new AuthorizationObjectSelectorType()
                                        .type(AccessCertificationCaseType.COMPLEX_TYPE)
                                        .assignee(self()))
                                .exceptItem(AccessCertificationCaseType.F_WORK_ITEM.toBean()));
                    }),

            entry(
                    ModelAuthorizationAction.RECORD_CERTIFICATION_DECISION.getUrl(),
                    (migrated, original) -> add(migrated, original, new AuthorizationType()
                            .action(ModelAuthorizationAction.COMPLETE_WORK_ITEM.getUrl())
                            .object(new AuthorizationObjectSelectorType()
                                    .type(AccessCertificationWorkItemType.COMPLEX_TYPE)
                                    .assignee(self()))))
    );

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
