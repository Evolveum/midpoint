/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.route;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemRouteSegmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemRouteType;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.ItemPath;

import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

/**
 * A generalized {@link ItemPath}: It allows pointing to a specific item value, while allowing to select
 * from multivalued items not only by ID, but by arbitrary filter.
 *
 * Should be immutable in the future. Now it's terribly hacked.
 */
public class ItemRoute {

    public static final ItemRoute EMPTY = new ItemRoute(List.of());

    @NotNull private final List<ItemRouteSegment> segments;

    private ItemRoute(@NotNull List<ItemRouteSegment> segments) {
        this.segments = segments;
    }

    /**
     * Creates a route from either path or route bean. Returns empty route if both are null.
     */
    public static @NotNull ItemRoute fromBeans(
            @Nullable ItemPathType pathBean,
            @Nullable ItemRouteType routeBean) {
        if (pathBean != null && routeBean != null) {
            throw new IllegalArgumentException("Both path and route are present: " + pathBean + ", " + routeBean);
        } else if (pathBean != null) {
            return fromPath(pathBean.getItemPath());
        } else if (routeBean != null) {
            return fromBean(routeBean);
        } else {
            return EMPTY;
        }
    }

    public static @NotNull ItemRoute fromBean(@NotNull ItemRouteType bean) {
        List<ItemRouteSegment> segments = new ArrayList<>();
        for (ItemRouteSegmentType segmentBean : bean.getSegment()) {
            segments.add(
                    ItemRouteSegment.fromBean(segmentBean));
        }
        return new ItemRoute(segments);
    }

    public static ItemRoute fromPath(ItemPath path) {
        return new ItemRoute(
                List.of(ItemRouteSegment.fromPath(path)));
    }

    public boolean isEmpty() {
        return segments.isEmpty();
    }

    public int size() {
        return segments.size();
    }

    public @NotNull ItemRouteSegment get(int i) {
        return segments.get(i);
    }

    public boolean startsWithVariable() {
        return !isEmpty() && segments.get(0).path.startsWithVariable();
    }

    // Assumes that it's not empty, and first segment's path is not empty
    // FIXME should create a new object, not modify the existing one!
    public ItemRoute rest() {
        ItemRouteSegment first = segments.get(0);
        segments.set(0,
                first.replacePath(
                        first.path.rest()));
        return this;
    }

    public @NotNull QName variableName() {
        assert startsWithVariable();
        return Objects.requireNonNull(
                segments.get(0).path.firstToVariableNameOrNull());
    }

    /** Shouldn't return `null` values. */
    public @NotNull List<PrismValue> resolveFor(@Nullable Containerable containerable) throws SchemaException {
        return ItemRouteResolver.resolve(containerable, this);
    }

    public @NotNull ItemRoute append(ItemRoute other) {
        List<ItemRouteSegment> allSegments = new ArrayList<>();
        allSegments.addAll(this.segments);
        allSegments.addAll(other.segments);
        return new ItemRoute(allSegments);
    }

    /**
     * Returns the last name path segment value; or null if there's no name path segment.
     */
    public @Nullable ItemName lastName() {
        for (int i = segments.size() - 1; i >= 0; i--) {
            ItemRouteSegment segment = segments.get(i);
            ItemName lastName = segment.getPath().lastName();
            if (lastName != null) {
                return lastName;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return segments.stream()
                .map(ItemRouteSegment::toString)
                .collect(Collectors.joining("/"));
    }
}
