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
import com.evolveum.midpoint.prism.query.*;

/**
 * Helper class to {@link SecurityEnforcerImpl}, used to evaluate query item authorizations.
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

    /**
     * Takes all left-hand-side items referenced by the given filter and adds them here as required ones.
     *
     * TODO What about right-hand-side ones?
     */
    void addRequiredItems(ObjectFilter filter) {
        addRequiredItemsInternal(filter, ItemPath.EMPTY_PATH);
    }

    private void addRequiredItemsInternal(ObjectFilter filter, ItemPath localRoot) {
        if (filter == null
                || filter instanceof UndefinedFilter
                || filter instanceof NoneFilter
                || filter instanceof AllFilter) {
            // No specific items are present here
        } else if (filter instanceof ValueFilter) {
            requiredItems.add(
                    localRoot.append(((ValueFilter<?, ?>) filter).getFullPath()));
        } else if (filter instanceof ExistsFilter) {
            ExistsFilter existsFilter = (ExistsFilter) filter;
            ItemPath existsRoot = localRoot.append(existsFilter.getFullPath());
            // Currently, we require also the root path to be authorized. This may be relaxed eventually
            // (but checking there is at least one item required by the inner filter). However, it is maybe safer
            // to require it in the current way.
            requiredItems.add(existsRoot);
            addRequiredItemsInternal(existsFilter.getFilter(), existsRoot);
        } else if (filter instanceof TypeFilter) {
            addRequiredItemsInternal(((TypeFilter) filter).getFilter(), localRoot);
        } else if (filter instanceof LogicalFilter) {
            for (ObjectFilter condition : ((LogicalFilter) filter).getConditions()) {
                addRequiredItemsInternal(condition, localRoot);
            }
        } else if (filter instanceof OrgFilter) {
            // Currently, no item is connected to this kind of filter; TODO consider parentOrgRef
        } else if (filter instanceof ReferencedByFilter) {
            // The filter contains conditions related to the referencing object, not to the one we are looking for here.
            // So any items mentioned there are irrelevant for our purposes here.
        } else if (filter instanceof InOidFilter) {
            // No item here (OID is not considered to be an item, for now).
        } else if (filter instanceof OwnedByFilter) {
            // Used for container searches. We currently do not support authorization at this level.
            // TODO reconsider after we start supporting authorizations for containers
        } else if (filter instanceof FullTextFilter) {
            // No item here.
        } else {
            throw new AssertionError("Unsupported kind of filter: " + filter);
        }
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
