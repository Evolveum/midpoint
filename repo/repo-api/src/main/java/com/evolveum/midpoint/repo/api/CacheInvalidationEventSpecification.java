/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.api;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * A declarative way of specifying what kinds of cache invalidation (object modification) events is particular
 * {@link CacheInvalidationListener} interested in.
 *
 * @see CacheInvalidationListener#getEventSpecifications()
 */
public class CacheInvalidationEventSpecification {

    // generally useful constants

    public static final Set<ChangeType> ALL_CHANGES = Collections.unmodifiableSet(EnumSet.allOf(ChangeType.class));
    public static final Set<ChangeType> MODIFY_DELETE = Collections.unmodifiableSet(EnumSet.of(ChangeType.MODIFY, ChangeType.DELETE));
    public static final Set<ItemPath> ALL_PATHS = Collections.singleton(ItemPath.EMPTY_PATH);

    /** A set of all available events. This is a convenience constant for listeners that want to listen to all events. */
    public static final Set<CacheInvalidationEventSpecification> ALL_AVAILABLE_EVENTS =
            Collections.singleton(of(ObjectType.class, ALL_CHANGES));

    private final Class<? extends ObjectType> objectType;
    private final Set<ItemPath> paths;
    private final Set<ChangeType> changeTypes;

    protected CacheInvalidationEventSpecification(Class<? extends ObjectType> objectType, Set<ItemPath> paths,
            Set<ChangeType> changeTypes) {
        this.objectType = objectType;
        this.paths = paths;
        this.changeTypes = changeTypes;
    }

    @SafeVarargs
    public static Set<CacheInvalidationEventSpecification> setOf(Class<? extends ObjectType>... types) {
        HashSet<CacheInvalidationEventSpecification> set = new HashSet<>();
        for (Class<? extends ObjectType> type : types) {
            set.add(of(type, ALL_CHANGES));
        }
        return set;
    }


    public static CacheInvalidationEventSpecification of(Class<? extends ObjectType> type, Set<ChangeType> changes) {
        return of(type, null, changes);
    }

    public static CacheInvalidationEventSpecification of(Class<? extends ObjectType> type, Set<ItemPath> paths,
            Set<ChangeType> changes) {
        if (paths == null) {
            paths = ALL_PATHS;
        }
        return new CacheInvalidationEventSpecification(type, paths, changes);
    }

    @NotNull
    public Class<? extends ObjectType> getObjectType() {
        return objectType;
    }

    @NotNull
    public Set<ItemPath> getPaths() {
        return paths;
    }

    @NotNull
    public Set<ChangeType> getChangeTypes() {
        return changeTypes;
    }

}
