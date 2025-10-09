/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.util.delta;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathSet;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Filters item deltas according to specified criteria.
 *
 * Currently in `schema` module (not in `prism` one), because I am not sure if it will be midPoint-specific or not.
 *
 * LIMITED FUNCTIONALITY. We are not able to see inside deltas. So, for example, when looking for
 * `activation/administrativeStatus` property, and the whole `activation` container is added, the delta is not shown.
 */
@Experimental
public class ItemDeltaFilter {

    @NotNull private final PathSet pathsToInclude = new PathSet();
    @NotNull private final PathSet pathsToExclude = new PathSet();
    private boolean includeOperationalItems = false;

    public static ItemDeltaFilter create(Object pathsToInclude, Object pathsToExclude, Boolean includeOperationalItems) {
        ItemDeltaFilter filter = new ItemDeltaFilter();
        filter.pathsToInclude.addAll(toPaths(pathsToInclude));
        filter.pathsToExclude.addAll(toPaths(pathsToExclude));
        if (includeOperationalItems != null) {
            filter.includeOperationalItems = includeOperationalItems;
        }
        return filter;
    }

    private static Collection<? extends ItemPath> toPaths(Object object) {
        if (object == null) {
            return List.of();
        } else if (object instanceof Collection) {
            return ((Collection<?>) object).stream()
                    .map(o -> toPath(o))
                    .collect(Collectors.toList());
        } else {
            return List.of(toPath(object));
        }
    }

    private static ItemPath toPath(Object object) {
        if (object instanceof ItemPath) {
            return (ItemPath) object;
        } else if (object instanceof ItemPathType) {
            return ((ItemPathType) object).getItemPath();
        } else if (object instanceof String) {
            return ItemPath.fromString((String) object);
        } else {
            throw new IllegalArgumentException("Not a path: " + object);
        }
    }

    /**
     * Algorithm:
     *
     * . The changed item path is stripped off the container identifiers, e.g. `assignment/[123]/description` becomes `assignment/description`.
     * . The simplified path is compared with {@link #pathsToInclude} and {@link #pathsToExclude}.
     * If it matches the former, the change is included.
     * If it matches the latter, the change is excluded.
     * . Otherwise, the last segment of the path is removed, e.g. `assignment/description` becomes `assignment`.
     * The process continues at previous point.
     * . If nothing matches, then
     * .. if there were any {@link #pathsToInclude}, the item will be excluded,
     * .. if there were no {@link #pathsToInclude}, the item will be included.
     * . Non-operational items are handled like this:
     * They are excluded, unless {@link #includeOperationalItems} is `true` _or_ they are explicitly and fully mentioned in {@link #pathsToInclude}.
     *
     * Limitations: this algorithm does not "see" inside container deltas. They are treated atomically - purely formally
     * according to their path.
     */
    public boolean matches(ItemDelta<?, ?> itemDelta) {
        ItemPath path = itemDelta.getPath().namedSegmentsOnly();
        Boolean match = getExactMatch(path);
        if (match != null) {
            return match; // Ignoring "operational" flag for exact match
        }
        if (itemDelta.isOperational() && !includeOperationalItems) {
            return false;
        }
        ItemPath reduced = path.allExceptLast();
        for (;;) {
            Boolean matchReduced = getExactMatch(reduced);
            if (matchReduced != null) {
                return matchReduced;
            }
            if (reduced.isEmpty()) {
                // If we specified anything to include, the the default is assumed to be "NOT INCLUDED".
                return pathsToInclude.isEmpty();
            }
            reduced = reduced.allExceptLast();
        }
    }

    private Boolean getExactMatch(ItemPath path) {
        if (pathsToInclude.contains(path)) {
            return true;
        } else if (pathsToExclude.contains(path)) {
            return false;
        } else {
            return null;
        }
    }
}
