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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * See {@link ItemSecurityConstraints} for the description.
 *
 * @author semancik
 */
public class ItemSecurityConstraintsImpl implements ItemSecurityConstraints {

    /** Items covered by positive (`ALLOW`) authorizations. */
    private final AutzItemPaths allowedItems = new AutzItemPaths();

    /** Items covered by negative (`DENY`) authorizations. */
    private final AutzItemPaths deniedItems = new AutzItemPaths();

    @Override
    public boolean isCompletelyAllowed() {
        return allowedItems.includesAllItems() && deniedItems.includesNoItems();
    }

    void collectItems(Authorization autz) {
        if (autz.getDecision() == AuthorizationDecisionType.ALLOW) {
            allowedItems.collectItems(autz);
        } else {
            deniedItems.collectItems(autz);
        }
    }

    @Override
    public @Nullable AuthorizationDecisionType findItemDecision(@NotNull ItemPath nameOnlyItemPath) {
        if (deniedItems.includes(nameOnlyItemPath)) {
            return AuthorizationDecisionType.DENY;
        }
        if (allowedItems.includes(nameOnlyItemPath)) {
            return AuthorizationDecisionType.ALLOW;
        }
        return null;
    }

    @Override
    public @Nullable AuthorizationDecisionType findAllItemsDecision() {
        if (deniedItems.includesAllItems()) {
            return AuthorizationDecisionType.DENY;
        }
        if (allowedItems.includesAllItems()) {
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
