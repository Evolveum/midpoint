/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Contains the complete information about required and authorized items used for a query evaluation.
 *
 * They not necessarily reside on a single object type. For example, when dealing with `referencedBy` filter
 * looking for roles referenced by `assignment/targetRef` for a given user, we should make sure the user has
 * an access to `assignment/targetRef` on {@link UserType}.
 *
 * TEMPORARY SOLUTION. The final solution should ensure that the user has an access to specific assignments
 * and their `targetRef` values - by embedding specific sub-filter right into the `referencedBy` filter.
 *
 * See also MID-9670.
 */
public class QueryObjectsAutzCoverage implements ShortDumpable {

    private final Map<Class<?>, QueryObjectAutzCoverage> objectAutzCoverages = new HashMap<>();

    /**
     * Takes all items referenced by the given filter and adds them here as required ones.
     *
     * TODO What about right-hand-side ones? What about ownedBy and other currently ignored types of filters?
     */
    void addRequiredItems(@NotNull Class<?> filterType, @Nullable ObjectFilter filter) {
        addRequiredItemsRecursively(filterType, filter, ItemPath.EMPTY_PATH);
    }

    private void addRequiredItemsRecursively(
            @NotNull Class<?> filterType, @Nullable ObjectFilter filter, @NotNull ItemPath localRoot) {
        if (filter == null
                || filter instanceof UndefinedFilter
                || filter instanceof NoneFilter
                || filter instanceof AllFilter) {
            // No specific items are present here
        } else if (filter instanceof ValueFilter<?, ?> valueFilter) {
            addRequiredItem(
                    filterType,
                    localRoot.append(valueFilter.getFullPath()));
        } else if (filter instanceof ExistsFilter existsFilter) {
            ItemPath existsRoot = localRoot.append(existsFilter.getFullPath());
            // Currently, we require also the root path to be authorized. This may be relaxed eventually
            // (but checking there is at least one item required by the inner filter). However, it is maybe safer
            // to require it in the current way.
            addRequiredItem(filterType, existsRoot);
            addRequiredItemsRecursively(filterType, existsFilter.getFilter(), existsRoot);
        } else if (filter instanceof TypeFilter typeFilter) {
            // TODO what object type should be required here?
            addRequiredItemsRecursively(filterType, typeFilter.getFilter(), localRoot);
        } else if (filter instanceof LogicalFilter logicalFilter) {
            for (ObjectFilter condition : logicalFilter.getConditions()) {
                addRequiredItemsRecursively(filterType, condition, localRoot);
            }
        } else if (filter instanceof OrgFilter) {
            // Currently, no item is connected to this kind of filter; TODO consider parentOrgRef
        } else if (filter instanceof ReferencedByFilter referencedByFilter) {
            // This is a preliminary implementation; working for filters like this:
            //
            // . referencedBy (
            //   @type = UserType
            //   and @path = assignment/targetRef
            //   and # = "00000000-0000-0000-0000-000000000002"
            // )
            //
            // but not like this:
            //
            // . referencedBy (
            //   @type = AssignmentType
            //   and @path = targetRef
            //   and . ownedBy (
            //     @type = UserType
            //     and @path = assignment
            //     and # = "00000000-0000-0000-0000-000000000002"
            //   )
            // )
            //
            // FIXME finish this
            var type = referencedByFilter.getType().getTypeClass();
            if (type != null) {
                // this should be normally the case
                addRequiredItem(type, referencedByFilter.getPath());
            }
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

    private void addRequiredItem(
            @NotNull Class<?> type, @NotNull ItemPath itemPath) {
        objectAutzCoverages
                .computeIfAbsent(type, k -> new QueryObjectAutzCoverage())
                .addRequiredItem(itemPath);
    }

    @Nullable String getUnsatisfiedItemsDescription() {
        var desc = objectAutzCoverages.entrySet().stream()
                .map(entry -> {
                    var items = entry.getValue().getUnsatisfiedItems();
                    if (!items.isEmpty()) {
                        return entry.getKey().getSimpleName() + ": " + items;
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.joining("; "));
        return MiscUtil.nullIfEmpty(desc);
    }

    /**
     * Updates the "authorized items" information, based on a specific authorization. The authorization
     * has already passed basic checks, like actions, limitations, phase, etc BUT not other, finer checks
     * like detailed filtering via object selectors. This method is called only for types that differ from the
     * type we are filtering on. (See {@link #processAuthorizationForFilterType(Class, Authorization)} for the latter.)
     *
     * TEMPORARY/LIMITED IMPLEMENTATION
     */
    void processAuthorizationForOtherTypes(@NotNull Class<?> filterType, @NotNull Authorization authorization)
            throws ConfigurationException {
        for (var entry : objectAutzCoverages.entrySet()) {
            Class<?> type = entry.getKey();
            if (!type.equals(filterType)) {
                entry.getValue().processAuthorization(type, authorization);
            }
        }
    }

    /**
     * Updates the "authorized items" information, based on a specific authorization. Called for the same type as the
     * one we are filtering on. The authorization proved itself applicable.
     *
     * TEMPORARY/LIMITED IMPLEMENTATION
     */
    void processAuthorizationForFilterType(@NotNull Class<?> filterType, @NotNull Authorization authorization)
            throws ConfigurationException {
        var coverages = objectAutzCoverages.get(filterType);
        if (coverages != null) {
            coverages.processAuthorization(filterType, authorization);
        }
    }

    @Override
    public void shortDump(StringBuilder sb) {
        sb.append(
                objectAutzCoverages.entrySet().stream()
                        .map(e -> e.getKey().getSimpleName() + ": " + e.getValue().shortDump())
                        .collect(Collectors.joining("; ")));
    }
}
