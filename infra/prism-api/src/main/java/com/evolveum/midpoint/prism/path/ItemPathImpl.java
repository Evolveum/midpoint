/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.path;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 *
 */
public final class ItemPathImpl implements ItemPath {

    public static final ItemPath EMPTY_PATH = new ItemPathImpl(Collections.emptyList());

    @NotNull private final List<Object> segments;

    private ItemPathImpl() {
        segments = new ArrayList<>();
    }

    /**
     * @pre segments list does not contain null elements
     */
    private ItemPathImpl(@NotNull List<Object> segments) {
        this.segments = segments;
    }

    @NotNull
    static ItemPathImpl createFromList(@NotNull List<?> components) {
        ItemPathImpl path = new ItemPathImpl();
        path.addAll(components);
        return path;
    }

    @NotNull
    static ItemPathImpl createFromListReverse(@NotNull List<?> components) {
        ItemPathImpl path = new ItemPathImpl();
        for (int i = components.size() - 1; i >= 0; i--) {
            path.add(components.get(i));
        }
        return path;
    }

    @NotNull
    static ItemPathImpl createFromArray(@NotNull Object[] components) {
        ItemPathImpl path = new ItemPathImpl();
        path.addAll(components);
        return path;
    }

    @NotNull
    static ItemPathImpl createFromIterator(@NotNull Iterator<?> iterator) {
        ItemPathImpl path = new ItemPathImpl();
        iterator.forEachRemaining(path::add);
        return path;
    }

    private void addAll(List<?> components) {
        for (Object component : components) {
            add(component);
        }
    }

    private void addAll(Object[] components) {
        for (Object component : components) {
            add(component);
        }
    }

    private void add(Object component) {
        if (component instanceof ItemPath && !(component instanceof ItemName)) {
            addAll(((ItemPath) component).getSegments());
        } else if (component instanceof Object[]) {
            addAll((Object[]) component);
        } else if (component instanceof String) {
            segments.add(new ItemName((String) component));
        } else if (component != null) {
            segments.add(component);
        } else {
            segments.add(IdItemPathSegment.NULL);
        }
    }

    @NotNull
    @Override
    public List<?> getSegments() {
        return segments;
    }

    @Override
    public boolean isEmpty() {
        return segments.size() == 0;
    }

    @Override
    public int size() {
        return segments.size();
    }

    @Override
    public Object first() {
        return segments.isEmpty() ? null : segments.get(0);
    }

    @NotNull
    @Override
    public ItemPath rest() {
        return rest(1);
    }

    @NotNull
    @Override
    public ItemPath rest(int n) {
        if (n == 0) {
            return this;
        } else {
            return subPath(n, segments.size());
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ItemPathImpl)) return false;
        ItemPathImpl itemPath = (ItemPathImpl) o;
        return segments.equals(itemPath.segments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(segments);
    }

    @NotNull
    @Override
    public ItemPath namedSegmentsOnly() {
        List<Object> newComponents = new ArrayList<>();
        for (Object segment : segments) {
            if (ItemPath.isName(segment)) {
                newComponents.add(segment);
            }
        }
        return new ItemPathImpl(newComponents);
    }

    @NotNull
    @Override
    public ItemPath removeIds() {
        List<Object> filtered = new ArrayList<>(segments);
        filtered.removeIf(ItemPath::isId);
        return new ItemPathImpl(filtered);
    }

    @Override
    public ItemName lastName() {
        for (int i = segments.size() - 1; i >= 0; i--) {
            Object segment = segments.get(i);
            if (ItemPath.isName(segment)) {
                return ItemPath.toName(segment);
            }
        }
        return null;
    }

    @Override
    public Object last() {
        return segments.isEmpty() ? null : segments.get(segments.size() - 1);
    }

    @Override
    public ItemPath firstAsPath() {
        return segments.isEmpty() ? this : new ItemPathImpl(Collections.singletonList(first()));
    }

    @Override
    public ItemPath subPath(int from, int to) {
        int fromClipped = Math.max(0, from);
        int toClipped = Math.min(to, segments.size());
        if (fromClipped >= toClipped) {
            return EMPTY_PATH;
        } else {
            // TODO ... the problem is that subList is not serializable; but this creates one object more
            return new ItemPathImpl(new ArrayList<>(segments.subList(fromClipped, toClipped)));
        }
    }

    @NotNull
    @Override
    public ItemPath allExceptLast() {
        return subPath(0, segments.size()-1);
    }

    @Override
    public Object getSegment(int i) {
        return segments.get(i);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        shortDump(sb);
        return sb.toString();
    }
}
