/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.security.enforcer.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.ShortDumpable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.prism.path.ItemPath.*;

/**
 * Supports "intelligent" specification of a set of item paths formed by three kinds of primitives:
 *
 * - "include all items" (corresponds to an authorization with neither `item` nor `exceptItem` values)
 * - "include specified items (plus children)" (corresponds to an authorization with `item` values)
 * - "include all items except for specified items (plus children)" (corresponds to an authorization with `exceptItem` values)
 *
 * This object is gradually built by calling {@link #collectItemPaths(Collection, Collection)} method providing increments
 * to `item` and `exceptItem` sets.
 *
 * Then, it is queried by calling {@link #includes(ItemPath)} to determine whether given item path is _completely_
 * covered by this set.
 *
 * An alternative approach is represented by {@link PrismEntityOpConstraints} and its implementations:
 *
 * . This (older) class is used e.g. in {@link ObjectSecurityConstraints} implementation, returned e.g. by
 * {@link SecurityEnforcer#compileSecurityConstraints(PrismObject, boolean, SecurityEnforcer.Options, Task, OperationResult)} method.
 *
 * . The newer class ({@link PrismEntityOpConstraints}) is used e.g. as a return value of narrow-focused
 * {@link SecurityEnforcer#compileOperationConstraints(MidPointPrincipal, PrismObjectValue, AuthorizationPhaseType, String[],
 * SecurityEnforcer.Options, CompileConstraintsOptions, Task, OperationResult)}.
 *
 * @author semancik
 */
public class PositiveNegativeItemPaths implements ShortDumpable {

    /** Items that are considered included (with all their children). */
    private final List<ItemPath> includedItems = new ArrayList<>();

    /** Items that are considered excluded (with all their children). */
    private final List<ItemPath> excludedItems = new ArrayList<>();

    /** If `true`, then all items are considered as included, regardless of {@link #includedItems} and {@link #excludedItems}. */
    private boolean allItemsIncluded;

    public boolean includesAllItems() {
        return allItemsIncluded;
    }

    protected @NotNull List<? extends ItemPath> getIncludedItems() {
        return includedItems;
    }

    protected @NotNull List<? extends ItemPath> getExcludedItems() {
        return excludedItems;
    }

    /**
     * Augments this specification with additional "included" (~ item) and "excluded" (~ exceptItem) item path collections.
     *
     * Note that currently `newIncludeItems` and `newExcludeItems` cannot be both non-empty.
     */
    public void collectItemPaths(
            Collection<? extends ItemPath> newIncludedItems, Collection<? extends ItemPath> newExcludedItems) {
        if (allItemsIncluded) {
            // We already explicitly include everything.
            // All specific (existing + new) inclusions can be ignored.
            // Also, all (existing + new) exclusions are ignored as well.
            return;
        }
        if (newIncludedItems.isEmpty() && newExcludedItems.isEmpty()) {
            // No items mean "include everything".
            allItemsIncluded = true;
            return;
        }

        // TODO: better merging, consider sub-paths
        //  That optimization would provide faster evaluation; but the current solution is correct as well.
        includedItems.addAll(newIncludedItems);

        if (excludedItems.isEmpty()) {
            // Again, we could do some merging with included items here. But not necessarily.
            excludedItems.addAll(newExcludedItems);
        } else {
            // Merging exceptItem is in fact intersection operation, not addition.
            // But we need to carefully consider sub-paths.
            List<ItemPath> excludedItemsToAdd = new ArrayList<>();
            Iterator<ItemPath> iterator = excludedItems.iterator();
            while (iterator.hasNext()) {
                ItemPath excludedItem = iterator.next();
                ItemPath replacementItem = null;
                boolean keep = false;
                for (ItemPath newExcludedItem : newExcludedItems) {
                    CompareResult result = newExcludedItem.compareComplex(excludedItem);
                    if (result == CompareResult.SUBPATH || result == CompareResult.EQUIVALENT) {
                        // match, keep excludedItem in the list
                        keep = true;
                        break;
                    }
                    if (result == CompareResult.SUPERPATH) {
                        // replace excludedItem with a more specific item
                        replacementItem = newExcludedItem;
                    }
                }
                if (!keep) {
                    iterator.remove();
                    if (replacementItem != null) {
                        excludedItemsToAdd.add(replacementItem);
                    }
                }
            }
            excludedItems.addAll(excludedItemsToAdd);
            if (excludedItems.isEmpty()) {
                allItemsIncluded = true;
            }
        }
    }

    /**
     * Is given `nameOnlyItemPath` considered to be _completely included_ in this specification?
     *
     * Generally, the contract of this method is intuitive. For completeness, see the comments inside.
     * The only non-obvious part is handling of excluded items at levels deeper than one.
     *
     * For example, let us assume we have
     *
     * - `includedItems` = `name`, `description`
     * - `excludedItems` = `assignment/activation`
     *
     * Then, all paths except for the following ones are considered to be "completely included":
     *
     * - `assignment` (because the `activation` child is excluded)
     * - `assignment/activation`
     * - `assignment/activation/xyz` (where `xyz` are children i.e. `administrativeStatus`, `validFrom`, `validTo`, and so on)
     *
     * Other children of `assignment` (like `assignment/targetRef`) are completely included.
     */
    public boolean includes(ItemPath nameOnlyItemPath) {
        if (allItemsIncluded) {
            // Obvious: If we consider all items as explicitly included, this one is included as well.
            return true;
        }
        if (ItemPathCollectionsUtil.containsSubpathOrEquivalent(includedItems, nameOnlyItemPath)) {
            // Obvious: The `includedItems` are those that are explicitly included, with their children.
            return true;
        }
        if (excludedItems.isEmpty()) {
            // Not everything is included, and there is nothing to exclude.
            // This means that what is not included, is excluded by default.
            return false;
        }
        for (ItemPath excludedItem : excludedItems) {
            CompareResult result = excludedItem.compareComplex(nameOnlyItemPath);
            // This is tricky. We really want to exclude all related paths:
            // sub-paths, super-paths and (obviously) the item itself. See the method's javadoc.
            if (result != CompareResult.NO_RELATION) {
                return false;
            }
        }
        // The current item is not matched by any exclude. It means that it was matched by at least one authorization with
        // "exceptItem" set. Hence, we consider it matching.
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName()).append("(");
        shortDump(sb);
        sb.append(")");
        return sb.toString();
    }

    @Override
    public void shortDump(StringBuilder sb) {
        if (allItemsIncluded) {
            sb.append("[all]");
        } else {
            if (includedItems.isEmpty() && excludedItems.isEmpty()) {
                sb.append("[none]");
            } else {
                sb.append("included: ");
                dumpItems(sb, includedItems);
                sb.append("; excluded: ");
                dumpItems(sb, excludedItems);
            }
        }
    }

    // TODO move to a better place
    public static void dumpItems(StringBuilder sb, List<? extends ItemPath> items) {
        if (items.isEmpty()) {
            sb.append("[none]");
        } else {
            Iterator<? extends ItemPath> iterator = items.iterator();
            while (iterator.hasNext()) {
                sb.append(PrettyPrinter.prettyPrint(iterator.next()));
                if (iterator.hasNext()) {
                    sb.append(", ");
                }
            }
        }
    }
}
