/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.security.enforcer.impl;

import com.evolveum.midpoint.security.enforcer.api.ObjectOperationConstraints;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;

import org.jetbrains.annotations.Nullable;

/** See {@link ObjectOperationConstraints} for the description. */
public class ObjectOperationConstraintsImpl implements ObjectOperationConstraints {

    private final ItemSecurityConstraintsImpl requestConstraints = new ItemSecurityConstraintsImpl();
    private final ItemSecurityConstraintsImpl execConstraints = new ItemSecurityConstraintsImpl();

    void applyAuthorization(Authorization autz) {
        AuthorizationPhaseType phase = autz.getPhase();
        if (phase == null) {
            requestConstraints.collectItems(autz);
            execConstraints.collectItems(autz);
        } else {
            getConstraints(phase).collectItems(autz);
        }
    }

    @Override
    public boolean isCompletelyAllowed(@Nullable AuthorizationPhaseType phase) {
        if (phase == null) {
            return requestConstraints.isCompletelyAllowed() && execConstraints.isCompletelyAllowed();
        } else {
            return getConstraints(phase).isCompletelyAllowed();
        }
    }

    @Override
    public AuthorizationDecisionType findAllItemsDecision(@Nullable AuthorizationPhaseType phase) {
        if (phase == null) {
            AuthorizationDecisionType requestDecision = requestConstraints.findAllItemsDecision();
            if (requestDecision == null || requestDecision == AuthorizationDecisionType.DENY) {
                return requestDecision;
            }
            return execConstraints.findAllItemsDecision();
        } else {
            return getConstraints(phase).findAllItemsDecision();
        }
    }

    @Override
    public AuthorizationDecisionType findItemDecision(ItemPath nameOnlyItemPath, @Nullable AuthorizationPhaseType phase) {
        if (phase == null) {
            AuthorizationDecisionType requestDecision = requestConstraints.findItemDecision(nameOnlyItemPath);
            if (requestDecision == null || requestDecision == AuthorizationDecisionType.DENY) {
                return requestDecision;
            }
            return execConstraints.findItemDecision(nameOnlyItemPath);
        } else {
            return getConstraints(phase).findItemDecision(nameOnlyItemPath);
        }
    }

    private @NotNull ItemSecurityConstraintsImpl getConstraints(@NotNull AuthorizationPhaseType phase) {
        return phase == AuthorizationPhaseType.REQUEST ? requestConstraints : execConstraints;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(ObjectOperationConstraintsImpl.class, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "requestConstraints", requestConstraints, indent+1);
        DebugUtil.debugDumpWithLabel(sb, "execConstraints", execConstraints, indent+1);
        return sb.toString();
    }
}
