/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

/**
 * This is a set of objects that considers objects being equal by simply comparing their OIDs.
 *
 * It should be more efficient and robust than e.g. {@link HashSet} that uses {@link ObjectType#hashCode()} and
 * {@link ObjectType#equals(Object)} methods.
 *
 * We intentionally do not implement {@link Set} interface, because we do not fulfill its contract.
 *
 * Requirement: only objects with OID can be stored here.
 *
 * TODO better name
 */
@Experimental
public class ObjectSet<O extends ObjectType> implements Collection<O> {

    @NotNull private final HashMap<String, O> objects = new HashMap<>();

    public ObjectSet() {
    }

    public ObjectSet(Collection<? extends O> initialObjects) {
        addAll(initialObjects);
    }

    @SafeVarargs
    public static <O extends ObjectType> ObjectSet<O> of(O... objects) {
        return new ObjectSet<>(List.of(objects));
    }

    public static <O extends ObjectType> ObjectSet<O> ofPrismObjects(Collection<PrismObject<O>> objects) {
        return new ObjectSet<>(ObjectTypeUtil.asObjectables(objects));
    }

    @Override
    public int size() {
        return objects.size();
    }

    @Override
    public boolean isEmpty() {
        return objects.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        //noinspection SuspiciousMethodCalls
        return objects.containsValue(o);
    }

    @NotNull
    @Override
    public Iterator<O> iterator() {
        return objects.values().iterator();
    }

    @SuppressWarnings("NullableProblems")
    @NotNull
    @Override
    public Object[] toArray() {
        return objects.values().toArray();
    }

    @SuppressWarnings("NullableProblems")
    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        //noinspection SuspiciousToArrayCall
        return objects.values().toArray(a);
    }

    @Override
    public boolean add(O o) {
        argCheck(o != null, "cannot add null object");
        String oid = o.getOid();
        argCheck(oid != null, "cannot add OID-less object");
        if (objects.containsKey(oid)) {
            return false;
        } else {
            objects.put(oid, o);
            return true;
        }
    }

    @Override
    public boolean remove(Object o) {
        if (!(o instanceof ObjectType)) {
            return false;
        }
        String oid = ((ObjectType) o).getOid();
        if (oid == null) {
            return false;
        }
        return objects.remove(oid) != null;
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return c.stream().allMatch(this::contains);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends O> c) {
        boolean changed = false;
        for (O o : c) {
            if (add(o)) {
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        boolean changed = false;
        for (Object o : c) {
            if (remove(o)) {
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException("too lazy to implement this");
    }

    @Override
    public void clear() {
        objects.clear();
    }

    public @NotNull List<O> asList() {
        return new ArrayList<>(objects.values());
    }

    public @NotNull List<PrismObject<O>> asPrismObjectList() {
        var list = new ArrayList<PrismObject<O>>(objects.size());
        //noinspection unchecked
        objects.values()
                .forEach(object -> list.add((PrismObject<O>) object.asPrismObject()));
        return list;
    }

    public boolean containsOid(String oid) {
        return objects.containsKey(oid);
    }

    public O get(String oid) {
        return objects.get(oid);
    }

    public @NotNull Set<String> oidSet() {
        return objects.keySet();
    }
}
