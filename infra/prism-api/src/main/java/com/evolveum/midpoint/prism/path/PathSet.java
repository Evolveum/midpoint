/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.path;

import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 *
 */
@Experimental
public class PathSet implements Set<ItemPath> {

    private final List<ItemPath> content = new ArrayList<>();

    @Override
    public int size() {
        return content.size();
    }

    @Override
    public boolean isEmpty() {
        return content.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return o instanceof ItemPath && ItemPathCollectionsUtil.containsEquivalent(content, (ItemPath) o);
    }

    @NotNull
    @Override
    public Iterator<ItemPath> iterator() {
        return content.iterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return content.toArray();
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        return content.toArray(a);
    }

    @Override
    public boolean add(@NotNull ItemPath itemPath) {
        //noinspection SimplifiableIfStatement
        if (contains(itemPath)) {
            return false;
        } else {
            return content.add(itemPath);
        }
    }

    @Override
    public boolean remove(Object o) {
        return content.removeIf(path -> o instanceof ItemPath && path.equivalent((ItemPath) o));
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        for (Object o : c) {
            //noinspection SuspiciousMethodCalls
            if (!content.contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends ItemPath> c) {
        c.forEach(this::add);
        return true; // fixme
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        c.forEach(this::remove);
        return true; // fixme
    }

    @Override
    public void clear() {
        content.clear();
    }
}
