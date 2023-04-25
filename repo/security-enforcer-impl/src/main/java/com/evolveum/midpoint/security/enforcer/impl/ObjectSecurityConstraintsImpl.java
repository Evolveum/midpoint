/*
 * Copyright (c) 2014-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.security.enforcer.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.enforcer.api.ObjectSecurityConstraints;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** See {@link ObjectSecurityConstraints} for the description. */
public class ObjectSecurityConstraintsImpl implements ObjectSecurityConstraints {

    private final Map<String, PhasedConstraints> actionMap = new HashMap<>();

    void applyAuthorization(@NotNull Authorization autz) {
        List<String> actions = autz.getAction();
        AuthorizationPhaseType phase = autz.getPhase();
        for (String action : actions) {
            if (phase == null) {
                getOrCreateItemConstraints(action, AuthorizationPhaseType.REQUEST).collectItems(autz);
                getOrCreateItemConstraints(action, AuthorizationPhaseType.EXECUTION).collectItems(autz);
            } else {
                getOrCreateItemConstraints(action, phase).collectItems(autz);
            }
        }
    }

    private @NotNull ItemSecurityConstraintsImpl getOrCreateItemConstraints(
            @NotNull String action, @NotNull AuthorizationPhaseType phase) {
        return actionMap
                .computeIfAbsent(action, k -> new PhasedConstraints())
                .get(phase);
    }

    @Override
    public @Nullable AuthorizationDecisionType findAllItemsDecision(
            @NotNull String @NotNull [] actionUrls, @Nullable AuthorizationPhaseType phase) {
        AuthorizationDecisionType decision = null;
        for (String actionUrl : actionUrls) {
            AuthorizationDecisionType actionDecision = findAllItemsDecision(actionUrl, phase);
            if (actionDecision == AuthorizationDecisionType.DENY) {
                return actionDecision;
            }
            if (actionDecision != null) {
                assert actionDecision == AuthorizationDecisionType.ALLOW;
                decision = actionDecision;
            }
        }
        return decision;
    }

    @Override
    public @Nullable AuthorizationDecisionType findAllItemsDecision(
            @NotNull String actionUrl, @Nullable AuthorizationPhaseType phase) {
        if (phase == null) {
            AuthorizationDecisionType requestDecision = getActionDecisionPhase(actionUrl, AuthorizationPhaseType.REQUEST);
            if (requestDecision == null || requestDecision == AuthorizationDecisionType.DENY) {
                return requestDecision;
            }
            return getActionDecisionPhase(actionUrl, AuthorizationPhaseType.EXECUTION);
        } else {
            return getActionDecisionPhase(actionUrl, phase);
        }
    }

    private AuthorizationDecisionType getActionDecisionPhase(@NotNull String actionUrl, @NotNull AuthorizationPhaseType phase) {
        ItemSecurityConstraintsImpl itemConstraints = getItemConstraints(actionUrl, phase);
        return itemConstraints != null ? itemConstraints.findAllItemsDecision() : null;
    }

    private ItemSecurityConstraintsImpl getItemConstraints(@NotNull String action, @NotNull AuthorizationPhaseType phase) {
        PhasedConstraints phasedConstraints = actionMap.get(action);
        return phasedConstraints != null ? phasedConstraints.get(phase) : null;
    }

    @Override
    public @Nullable AuthorizationDecisionType findItemDecision(
            @NotNull ItemPath nameOnlyItemPath, @NotNull String @NotNull [] actionUrls, @Nullable AuthorizationPhaseType phase) {
        AuthorizationDecisionType decision = null;
        for (String actionUrl : actionUrls) {
            AuthorizationDecisionType actionDecision = findItemDecision(nameOnlyItemPath, actionUrl, phase);
            if (actionDecision == AuthorizationDecisionType.DENY) {
                return actionDecision;
            }
            if (actionDecision != null) {
                assert actionDecision == AuthorizationDecisionType.ALLOW;
                decision = actionDecision;
            }
        }
        return decision;
    }

    @Override
    public @Nullable AuthorizationDecisionType findItemDecision(
            @NotNull ItemPath nameOnlyItemPath, @NotNull String actionUrl, @Nullable AuthorizationPhaseType phase) {
        if (phase == null) {
            AuthorizationDecisionType requestDecision =
                    findItemDecisionPhase(nameOnlyItemPath, actionUrl, AuthorizationPhaseType.REQUEST);
            if (requestDecision == null || requestDecision == AuthorizationDecisionType.DENY) {
                return requestDecision;
            }
            return findItemDecisionPhase(nameOnlyItemPath, actionUrl, AuthorizationPhaseType.EXECUTION);
        } else {
            return findItemDecisionPhase(nameOnlyItemPath, actionUrl, phase);
        }
    }

    private AuthorizationDecisionType findItemDecisionPhase(
            ItemPath nameOnlyItemPath, String actionUrl, @NotNull AuthorizationPhaseType phase) {
        ItemSecurityConstraintsImpl itemConstraints = getItemConstraints(actionUrl, phase);
        AuthorizationDecisionType decision;
        if (itemConstraints != null) {
            decision = itemConstraints.findItemDecision(nameOnlyItemPath);
            if (decision == AuthorizationDecisionType.DENY) {
                return AuthorizationDecisionType.DENY;
            }
        } else {
            decision = null;
        }
        ItemSecurityConstraintsImpl itemConstraintsActionAll = getItemConstraints(AuthorizationConstants.AUTZ_ALL_URL, phase);
        if (itemConstraintsActionAll == null) {
            return decision;
        }
        AuthorizationDecisionType decisionActionAll = itemConstraintsActionAll.findItemDecision(nameOnlyItemPath);
        if (AuthorizationDecisionType.DENY.equals(decisionActionAll)) {
            return AuthorizationDecisionType.DENY;
        }
        if (AuthorizationDecisionType.ALLOW.equals(decisionActionAll)) {
            return AuthorizationDecisionType.ALLOW;
        }
        return decision;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(ObjectSecurityConstraintsImpl.class, indent);
        DebugUtil.debugDumpWithLabel(sb, "actionMap", actionMap, indent+1);
        return sb.toString();
    }
}
