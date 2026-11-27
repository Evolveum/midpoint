/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.expression.evaluator.caching;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Remembers objects created on demand in a simulation (they are not stored anywhere), so the same search
 * does not create another object with another OID each time it is evaluated.
 *
 * Lives on the current thread from the first {@link #enter()} to the matching last {@link #exit()}.
 */
public class SimulatedCreateOnDemandRegistry {

    private static final ThreadLocal<SimulatedCreateOnDemandRegistry> INSTANCE = new ThreadLocal<>();

    private final Map<Key, PrismObject<? extends ObjectType>> objects = new HashMap<>();
    private int depth;

    public static void enter() {
        SimulatedCreateOnDemandRegistry registry = INSTANCE.get();
        if (registry == null) {
            registry = new SimulatedCreateOnDemandRegistry();
            INSTANCE.set(registry);
        }
        registry.depth++;
    }

    public static void exit() {
        SimulatedCreateOnDemandRegistry registry = INSTANCE.get();
        if (registry != null && --registry.depth <= 0) {
            INSTANCE.remove();
        }
    }

    public static SimulatedCreateOnDemandRegistry get() {
        return INSTANCE.get();
    }

    @SuppressWarnings("unchecked")
    public <O extends ObjectType> PrismObject<O> find(Class<O> type, Collection<ObjectQuery> queries) {
        return (PrismObject<O>) objects.get(new Key(type, queries));
    }

    /** Returns the object created on demand in the current simulation, or null (also if the task is fully persistent). */
    public static PrismObject<? extends ObjectType> findSimulated(Task task, String oid) {
        SimulatedCreateOnDemandRegistry registry = INSTANCE.get();
        return registry != null && !task.isExecutionFullyPersistent() ? registry.findByOid(oid) : null;
    }

    public PrismObject<? extends ObjectType> findByOid(String oid) {
        return objects.values().stream()
                .filter(object -> oid.equals(object.getOid()))
                .findFirst()
                .orElse(null);
    }

    public <O extends ObjectType> void put(Class<O> type, Collection<ObjectQuery> queries, PrismObject<O> object) {
        objects.put(new Key(type, queries), object);
    }

    private record Key(Class<? extends ObjectType> type, List<ObjectQuery> queries) {

        Key(Class<? extends ObjectType> type, Collection<ObjectQuery> queries) {
            this(type, new ArrayList<>(queries));
        }
    }
}
