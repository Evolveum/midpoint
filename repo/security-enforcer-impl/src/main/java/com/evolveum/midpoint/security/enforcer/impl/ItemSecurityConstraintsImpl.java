/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.security.enforcer.impl;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.enforcer.api.ItemSecurityConstraints;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationDecisionType;

/**
 * @author semancik
 *
 */
public class ItemSecurityConstraintsImpl implements ItemSecurityConstraints {

    private AutzItemPaths allowedItems = new AutzItemPaths();
    private AutzItemPaths deniedItems = new AutzItemPaths();

    protected AutzItemPaths getAllowedItems() {
        return allowedItems;
    }

    protected AutzItemPaths getDeniedItems() {
        return deniedItems;
    }

    public AutzItemPaths get(AuthorizationDecisionType decision) {
        switch (decision) {
            case ALLOW:
                return allowedItems;
            case DENY:
                return deniedItems;
            default:
                return null;
        }
    }

    public void collectItems(Authorization autz) {
        AuthorizationDecisionType decision = autz.getDecision();
        if (decision == null || decision == AuthorizationDecisionType.ALLOW) {
            allowedItems.collectItems(autz);
        } else {
            deniedItems.collectItems(autz);
        }
    }

    @Override
    public AuthorizationDecisionType findItemDecision(ItemPath nameOnlyItemPath) {
        if (deniedItems.isApplicable(nameOnlyItemPath)) {
            return AuthorizationDecisionType.DENY;
        }
        if (allowedItems.isApplicable(nameOnlyItemPath)) {
            return AuthorizationDecisionType.ALLOW;
        }
        return null;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(ItemSecurityConstraintsImpl.class, indent);
        DebugUtil.debugDumpShortWithLabelLn(sb, "allowedItems", allowedItems, indent+1);
        DebugUtil.debugDumpShortWithLabel(sb, "deniedItems", deniedItems, indent+1);
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ItemSecurityConstraintsImpl(allowedItems=");
        allowedItems.shortDump(sb);
        sb.append(", deniedItems=");
        deniedItems.shortDump(sb);
        sb.append(")");
        return sb.toString();
    }

}
