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

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemRouteSegmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemRouteType;

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

    public @NotNull List<PrismValue> resolveFor(@Nullable Containerable containerable) throws SchemaException {
        return ItemRouteResolver.resolve(containerable, this);
    }

    public @NotNull ItemRoute append(ItemRoute other) {
        List<ItemRouteSegment> allSegments = new ArrayList<>();
        allSegments.addAll(this.segments);
        allSegments.addAll(other.segments);
        return new ItemRoute(allSegments);
    }

//    // Temporary code - document, optimize, etc
//    public @NotNull ItemPath getTotalPathNaive() {
//        ItemPath total = ItemPath.EMPTY_PATH;
//        for (ItemRouteSegment segment : segments) {
//            total = total.append(segment.path);
//        }
//        return total;
//    }
}
