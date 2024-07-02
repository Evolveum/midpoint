/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathImpl;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

/**
 * Path pointing to a specific {@link ShadowAttribute}.
 * It is always in the form of `attributes/<name>`.
 */
@Experimental
public class AttributePath implements ItemPath {

    @NotNull private final ItemName attributeName;
    @NotNull private final List<Object> segments;

    private AttributePath(@NotNull ItemName attributeName) {
        this.attributeName = attributeName;
        this.segments = List.of(ShadowType.F_ATTRIBUTES, attributeName);
    }

    public static @NotNull AttributePath of(ItemPath path) {
        if (path instanceof AttributePath attributePath) {
            return attributePath;
        }
        argCheck(path.startsWith(ShadowType.F_ATTRIBUTES), "Path is not related to attributes: %s", path);
        argCheck(path.size() == 2, "Supposed attribute-related path has wrong format: %s", path);
        return new AttributePath(ItemPath.toName(path.getSegment(1)));
    }

    public static @NotNull Optional<AttributePath> optionalOf(ItemPath path) {
        if (path instanceof AttributePath attributePath) {
            return Optional.of(attributePath);
        }
        if (path.startsWith(ShadowType.F_ATTRIBUTES)) {
            return Optional.of(of(path));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public int size() {
        return 2;
    }

    @Override
    public @NotNull List<?> getSegments() {
        return segments;
    }

    @Override
    public @Nullable Object getSegment(int i) {
        return segments.get(i);
    }

    @Override
    public @Nullable Object first() {
        return ShadowType.F_ATTRIBUTES;
    }

    @Override
    public @NotNull ItemPath rest(int n) {
        if (n == 0) {
            return this;
        } else if (n == 1) {
            return attributeName;
        } else {
            return ItemPathImpl.EMPTY_PATH;
        }
    }

    @Override
    public ItemPath firstAsPath() {
        return ShadowType.F_ATTRIBUTES;
    }

    @Override
    public @NotNull Object last() {
        return attributeName;
    }

    @Override
    public @NotNull ItemPath allExceptLast() {
        return ShadowType.F_ATTRIBUTES;
    }

    @Override
    public ItemPath subPath(int from, int to) {
        // Hopefully not used too often (as it's not optimized much)
        return ItemPath.create(segments)
                .subPath(from, to);
    }

    @Override
    public ItemName lastName() {
        return attributeName;
    }

    @Override
    public @NotNull ItemPath namedSegmentsOnly() {
        return this;
    }

    @Override
    public @NotNull ItemPath removeIds() {
        return this;
    }

    public @NotNull ItemName getAttributeName() {
        return attributeName;
    }
}
