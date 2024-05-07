/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.security.enforcer.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.TypedItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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

    /** Required and authorized search items for specific object types. */
    private final Map<Class<? extends ObjectType>, QueryObjectAutzCoverage> objectAutzCoverages = new HashMap<>();

    /**
     * Takes all items referenced by the given filter and adds them here as required ones.
     *
     * TODO What about right-hand-side ones? What about ownedBy and other currently ignored types of filters?
     */
    void addRequiredItems(@NotNull Class<?> filterType, @Nullable ObjectFilter filter) {
        if (filter == null) {
            return;
        }
        var schemaRegistry = PrismContext.get().getSchemaRegistry();
        filter.collectUsedPaths(
                TypedItemPath.of(
                        schemaRegistry.determineTypeForClassRequired(filterType)),
                typedItemPath -> addRequiredItem(
                        schemaRegistry.determineClassForTypeRequired(typedItemPath.getRootType()),
                        typedItemPath.getPath()),
                true);
    }

    private void addRequiredItem(
            @NotNull Class<?> type, @NotNull ItemPath itemPath) {
        if (itemPath.startsWith(PrismConstants.T_PARENT)) {
            // temporary solution; we should know the broader context
            var parent = determineParent(type);
            if (parent != null) {
                addRequiredItem(parent.type, itemPath.rest());
            }
        } else if (!ObjectType.class.isAssignableFrom(type)) {
            // temporary solution; we should know the broader context
            var parent = determineParent(type);
            if (parent != null) {
                addRequiredItem(parent.type, parent.path.append(itemPath));
            }
        } else {
            //noinspection unchecked
            objectAutzCoverages
                    .computeIfAbsent((Class<? extends ObjectType>) type, k -> new QueryObjectAutzCoverage())
                    .addRequiredItem(itemPath);
        }
    }

    // See also SelectorWithItems#getCandidateAdjustments
    private Parent determineParent(@NotNull Class<?> type) {
        if (AccessCertificationCaseType.class.isAssignableFrom(type)) {
            return new Parent(AccessCertificationCampaignType.class, AccessCertificationCampaignType.F_CASE);
        } else if (AccessCertificationWorkItemType.class.isAssignableFrom(type)) {
            return new Parent(AccessCertificationCaseType.class, AccessCertificationCaseType.F_WORK_ITEM);
        } else if (CaseWorkItemType.class.isAssignableFrom(type)) {
            return new Parent(CaseType.class, CaseType.F_WORK_ITEM);
        } else if (AssignmentType.class.isAssignableFrom(type)) {
            return new Parent(AssignmentHolderType.class, AssignmentHolderType.F_ASSIGNMENT);
        } else if (OperationExecutionType.class.isAssignableFrom(type)) {
            return new Parent(ObjectType.class, ObjectType.F_OPERATION_EXECUTION);
        } else if (SimulationResultProcessedObjectType.class.isAssignableFrom(type)) {
            return new Parent(SimulationResultType.class, SimulationResultType.F_PROCESSED_OBJECT);
        } else {
            return null; // ignoring this requirement
        }
    }

    private record Parent(@NotNull Class<?> type, @NotNull ItemPath path) {
    }

    // TODO better name
    @NotNull Collection<Map.Entry<Class<? extends ObjectType>, QueryObjectAutzCoverage>> getAllEntries() {
        return objectAutzCoverages.entrySet();
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

    @Override
    public void shortDump(StringBuilder sb) {
        sb.append(
                objectAutzCoverages.entrySet().stream()
                        .map(e -> e.getKey().getSimpleName() + ": " + e.getValue().shortDump())
                        .collect(Collectors.joining("; ")));
    }

    @Override
    public String toString() {
        return shortDump();
    }
}
