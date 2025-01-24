/*
 * Copyright (C) 2014-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.AbstractFreezable;
import com.evolveum.midpoint.prism.Freezable;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.annotation.Experimental;

public class SearchResultList<T> extends AbstractFreezable
        implements List<T>, Cloneable, Serializable, ShortDumpable {

    private List<T> list = null;
    private SearchResultMetadata metadata = null;

    /** Returns modifiable instance, just to keep the existing behavior. */
    public static <T> SearchResultList<T> empty() {
        return new SearchResultList<>(new ArrayList<>());
    }

    @Override
    protected void performFreeze() {
        if (isImmutable()) {
            return;
        }
        if (list != null) {
            list = Collections.unmodifiableList(list);
        } else {
            list = Collections.emptyList();
        }
        for (T item : list) {
            if (item instanceof Freezable freezable) {
                freezable.freeze();
            }
        }
        if (metadata != null) {
            metadata.freeze();
        }
    }

    public SearchResultList() {
    }

    public SearchResultList(List<T> list) {
        this.list = list;
    }

    public SearchResultList(List<T> list, SearchResultMetadata metadata) {
        this.list = list;
        this.metadata = metadata;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public SearchResultMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(SearchResultMetadata metadata) {
        this.metadata = metadata;
    }

    public int size() {
        if (list == null) {
            return 0;
        }
        return list.size();
    }

    public boolean isEmpty() {
        return list == null || list.isEmpty();
    }

    public boolean contains(Object o) {
        return list != null && list.contains(o);
    }

    @NotNull
    public Iterator<T> iterator() {
        return list != null ? list.iterator() : Collections.emptyIterator();
    }

    @NotNull
    public Object[] toArray() {
        return getInitializedList().toArray();
    }

    @NotNull
    public <TT> TT[] toArray(@NotNull TT[] a) {
        //noinspection SuspiciousToArrayCall
        return getInitializedList().toArray(a);
    }

    public boolean add(T e) {
        return getInitializedList().add(e);
    }

    public boolean remove(Object o) {
        return list != null && list.remove(o);
    }

    public boolean containsAll(@NotNull Collection<?> c) {
        return list != null && list.containsAll(c);
    }

    public boolean addAll(@NotNull Collection<? extends T> c) {
        return getInitializedList().addAll(c);
    }

    public boolean addAll(int index, @NotNull Collection<? extends T> c) {
        return getInitializedList().addAll(index, c);
    }

    public boolean removeAll(@NotNull Collection<?> c) {
        return list != null && list.removeAll(c);
    }

    public boolean retainAll(@NotNull Collection<?> c) {
        return list != null && list.retainAll(c);
    }

    public void clear() {
        if (list != null) {
            list.clear();
        }
    }

    public T get(int index) {
        return list != null ? list.get(index) : null;
    }

    public T set(int index, T element) {
        return getInitializedList().set(index, element);
    }

    public void add(int index, T element) {
        getInitializedList().add(index, element);
    }

    public T remove(int index) {
        return list != null ? list.remove(index) : null;
    }

    public int indexOf(Object o) {
        return list != null ? list.indexOf(o) : -1;
    }

    public int lastIndexOf(Object o) {
        return list != null ? list.lastIndexOf(o) : -1;
    }

    @NotNull
    public ListIterator<T> listIterator() {
        return list != null ? list.listIterator() : Collections.emptyListIterator();
    }

    @NotNull
    public ListIterator<T> listIterator(int index) {
        return list != null ? list.listIterator(index) : Collections.emptyListIterator();
    }

    @NotNull
    public List<T> subList(int fromIndex, int toIndex) {
        return getInitializedList().subList(fromIndex, toIndex);
    }

    public <R> SearchResultList<R> map(Function<T, R> mappingFunction) {
        if (list == null) {
            //noinspection unchecked
            return (SearchResultList<R>) this;
        }
        return new SearchResultList<>(
                list.stream().map(mappingFunction).collect(Collectors.toList()),
                metadata);
    }

    // Do NOT auto-generate -- there are manual changes here
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((list == null) ? Collections.emptyList().hashCode() : list.hashCode());
        result = prime * result + ((metadata == null) ? 0 : metadata.hashCode());
        return result;
    }

    // Do NOT auto-generate -- there are manual changes here
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        SearchResultList other = (SearchResultList) obj;
        if (list == null || list.isEmpty()) {
            if (other.list != null && !other.list.isEmpty()) {
                return false;
            }
        } else if (!list.equals(other.list)) {
            return false;
        }
        if (metadata == null) {
            return other.metadata == null;
        } else {
            return metadata.equals(other.metadata);
        }
    }

    @Override
    public String toString() {
        if (metadata == null) {
            if (list == null) {
                return "SearchResultList(null)";
            } else {
                return "SearchResultList(" + list + ")";
            }
        } else {
            if (list == null) {
                return "SearchResultList(" + metadata + ")";
            } else {
                return "SearchResultList(" + list + ", " + metadata + ")";
            }
        }
    }

    /**
     * Just to emphasize the semantics.
     * (Is this a good idea? Because then someone could think that clone() is a shallow clone!)
     */
    @Experimental
    public SearchResultList<T> deepClone() {
        return clone();
    }

    /**
     * Just to indicate that clone() is a deep one :)
     */
    @Experimental
    public SearchResultList<T> shallowClone() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns deep frozen list - either this or a clone.
     */
    @Experimental
    public SearchResultList<T> toDeeplyFrozenList() {
        if (isImmutable()) {
            return this;
        } else {
            SearchResultList<T> clone = deepClone();
            clone.freeze();
            return clone;
        }
    }

    /**
     * This is actually a deep clone.
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public SearchResultList<T> clone() {
        return transform(CloneUtil::clone);
    }

    public <T2> @NotNull SearchResultList<T2> transform(@NotNull Function<T, T2> transformer) {
        SearchResultList<T2> clone = new SearchResultList<>();
        clone.metadata = this.metadata; // considered read-only object
        if (this.list != null) {
            clone.list = new ArrayList<>(this.list.size());
            for (T item : this.list) {
                clone.list.add(transformer.apply(item));
            }
        }
        return clone;
    }

    private List<T> getInitializedList() {
        if (list == null) {
            list = new ArrayList<>();
        }
        return list;
    }

    @Override
    public void shortDump(StringBuilder sb) {
        if (metadata == null) {
            if (list == null) {
                sb.append("null");
            } else {
                sb.append(list.size()).append(" results");
            }
        } else {
            if (list == null) {
                sb.append("null, metadata=(");
            } else {
                sb.append(list.size()).append(" results, metadata=(");
            }
            metadata.shortDump(sb);
            sb.append(")");
        }
    }
}
