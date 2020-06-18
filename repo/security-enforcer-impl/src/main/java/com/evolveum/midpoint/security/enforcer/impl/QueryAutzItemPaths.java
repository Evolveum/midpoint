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
import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;
import com.evolveum.midpoint.prism.query.ItemFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;

/**
 * Helper class to SecurityEnforcer, used to evaluate query item authorizations.
 * It checks whether we are authorized to use all items that are present in given search filter(s).
 *
 * @author semancik
 */
public class QueryAutzItemPaths extends AutzItemPaths {

    /**
     * Items required by search filter(s).
     */
    private final List<ItemPath> requiredItems = new ArrayList<>();

    public List<ItemPath> getRequiredItems() {
        return requiredItems;
    }

    public void addRequiredItem(ItemPath path) {
        requiredItems.add(path);
    }

    void addRequiredItems(ObjectFilter filter) {
        filter.accept(visitable -> {
            if (visitable instanceof ItemFilter) {
                requiredItems.add(((ItemFilter)visitable).getFullPath());
            }
        });
    }

    /**
     * @return Items that are required but we have no authorizations for.
     */
    List<ItemPath> evaluateUnsatisfiedItems() {
        List<ItemPath> unsatisfiedItems = new ArrayList<>();
        if (isAllItems()) {
            return unsatisfiedItems;
        }
        for (ItemPath requiredItem: requiredItems) {
            if (ItemPathCollectionsUtil.containsEquivalent(getIncludedItems(), requiredItem)) {
                // allowed
                continue;
            }
            if (!getExcludedItems().isEmpty() && !ItemPathCollectionsUtil.containsEquivalent(getExcludedItems(), requiredItem)) {
                // not notAllowed = allowed
                continue;
            }
            unsatisfiedItems.add(requiredItem);
        }
        return unsatisfiedItems;
    }

    @Override
    public void shortDump(StringBuilder sb) {
        sb.append("required: ");
        dumpItems(sb, requiredItems);
        sb.append("; ");
        super.shortDump(sb);
    }

}
