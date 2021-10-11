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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Helper class to SecurityEnforcer, used to evaluate query item authorizations.
 *
 * @author semancik
 */
public class QueryAutzItemPaths extends AutzItemPaths {

    private static final Trace LOGGER = TraceManager.getTrace(QueryAutzItemPaths.class);

    private List<ItemPath> requiredItems = new ArrayList<>();

    public List<ItemPath> getRequiredItems() {
        return requiredItems;
    }

    public void addRequiredItem(ItemPath path) {
        requiredItems.add(path);
    }

    public void addRequiredItems(ObjectFilter filter) {
        filter.accept(visitable -> {
            if (visitable instanceof ItemFilter) {
                requiredItems.add(((ItemFilter)visitable).getFullPath());
            }
        });
    }


    public List<ItemPath> evaluateUnsatisfierItems() {
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
