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

/**
 * Migrates authorizations from a legacy format into a new one.
 *
 * For example, when going from 4.4/4.7 to 4.8, `#readOwnCertificationDecisions`
 * is replaced by `#read` on `AccessCertificationWorkItemType` objects.
 */
@Component
public class AuthorizationMigrator {

    @SuppressWarnings("deprecation")
    public @NotNull Collection<AuthorizationType> migrate(@NotNull AuthorizationType original) {
        List<AuthorizationType> migrated = new ArrayList<>();
        migrated.add(original);

        var origName = original.getName();
        var origDecision = original.getDecision();
        var origPhase = original.getPhase();
        var limitations = original.getLimitations();

        List<String> actions = original.getAction();
        if (actions.contains(ModelAuthorizationAction.READ_OWN_CERTIFICATION_DECISIONS.getUrl())) {
            migrated.add(new AuthorizationType()
                    .decision(origDecision)
                    .phase(origPhase)
                    .name(newName(origName))
                    .action(ModelAuthorizationAction.READ.getUrl())
                    .limitations(CloneUtil.clone(limitations))
                    .object(new AuthorizationObjectSelectorType()
                            .type(AccessCertificationWorkItemType.COMPLEX_TYPE)
                            .assignee(self())));
            if (origDecision != AuthorizationDecisionType.DENY) {
                migrated.add(new AuthorizationType()
                        .phase(origPhase)
                        .name(newName(origName))
                        .action(ModelAuthorizationAction.READ.getUrl())
                        .limitations(CloneUtil.clone(limitations))
                        .object(new AuthorizationObjectSelectorType()
                                .type(AccessCertificationCaseType.COMPLEX_TYPE)
                                .assignee(self()))
                        .exceptItem(AccessCertificationCaseType.F_WORK_ITEM.toBean()));
            }
        }

        return migrated;
    }

    private SubjectedObjectSelectorType self() {
        return new SubjectedObjectSelectorType()
                .special(SpecialObjectSpecificationType.SELF);
    }

    private String newName(String name) {
        if (name == null) {
            return "migrated";
        } else {
            return name + " (migrated)";
        }
    }
}
