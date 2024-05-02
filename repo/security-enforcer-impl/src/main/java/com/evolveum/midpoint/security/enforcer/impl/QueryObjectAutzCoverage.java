/*
 * Copyright (c) 2017-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.security.enforcer.impl;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.selector.spec.ValueSelector;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.enforcer.api.PositiveNegativeItemPaths;
import com.evolveum.midpoint.util.ShortDumpable;

import com.evolveum.midpoint.util.exception.ConfigurationException;

import org.jetbrains.annotations.NotNull;

/**
 * Contains the complete information about required and authorized items (of some specific object type, defined externally)
 * that are needed for a query evaluation.
 *
 * @author semancik
 */
public class QueryObjectAutzCoverage implements ShortDumpable {

    /** Items required by search filter(s) for type represented by this object. */
    private final List<ItemPath> requiredItems = new ArrayList<>();

    /** Item paths allowed by authorizations. */
    private final AutzItemPaths allowedItemPaths = new AutzItemPaths();

    /** Item paths denied by authorizations. */
    private final AutzItemPaths deniedItemPaths = new AutzItemPaths();

    void addRequiredItem(@NotNull ItemPath itemPath) {
        requiredItems.add(itemPath);
    }

    /** Returns items that are required but we have no authorizations for. */
    List<ItemPath> getUnsatisfiedItems() {
        List<ItemPath> unsatisfiedItems = new ArrayList<>();
        for (ItemPath requiredItem : requiredItems) {
            if (deniedItemPaths.includes(requiredItem)
                    || !allowedItemPaths.includes(requiredItem)) {
                unsatisfiedItems.add(requiredItem);
            }
        }
        return unsatisfiedItems;
    }

    /** Extracts allowed/denied item paths from given authorization. */
    void processAuthorization(Class<?> type, Authorization authorization) throws ConfigurationException {
        // TODO implement at least minimal tracing here
        if (isAuthorizationIrrelevantForType(type, authorization)) {
            return;
        }
        if (authorization.isAllow()) {
            allowedItemPaths.collectItems(authorization);
        } else {
            deniedItemPaths.collectItems(authorization);
        }
    }

    private boolean isAuthorizationIrrelevantForType(Class<?> requiredType, Authorization authorization)
            throws ConfigurationException {
        for (ValueSelector selector : authorization.getParsedObjectSelectors()) {
            var typeInSelector = selector.getEffectiveType();
            if (!typeInSelector.isAssignableFrom(requiredType) && !requiredType.isAssignableFrom(typeInSelector)) {
                return true; // no overlap
            }
        }
        return false;
    }

    @Override
    public void shortDump(StringBuilder sb) {
        sb.append("required: ");
        PositiveNegativeItemPaths.dumpItems(sb, requiredItems);
        sb.append("; allowed: ");
        allowedItemPaths.shortDump(sb);
        sb.append("; denied: ");
        deniedItemPaths.shortDump(sb);
    }
}
