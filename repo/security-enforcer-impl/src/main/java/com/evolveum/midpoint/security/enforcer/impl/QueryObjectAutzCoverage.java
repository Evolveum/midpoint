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
import com.evolveum.midpoint.security.enforcer.api.PositiveNegativeItemPaths;
import com.evolveum.midpoint.security.enforcer.impl.AuthorizationSearchItemsEvaluation.AuthorizedSearchItems;
import com.evolveum.midpoint.util.ShortDumpable;

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

    @NotNull List<ItemPath> getRequiredItems() {
        return requiredItems;
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

    /** Applies item/exceptItem information derived from an authorization. */
    void processSearchItems(@NotNull AuthorizedSearchItems searchItems, boolean isAllow) {
        if (isAllow) {
            allowedItemPaths.collectItemPaths(searchItems.positives(), searchItems.negatives());
        } else {
            deniedItemPaths.collectItemPaths(searchItems.positives(), searchItems.negatives());
        }
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
