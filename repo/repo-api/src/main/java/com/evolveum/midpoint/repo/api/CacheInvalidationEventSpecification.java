/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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

public class CacheInvalidationEventSpecification {

    public static final Set<ChangeType> ALL_CHANGES = Collections.unmodifiableSet(EnumSet.allOf(ChangeType.class));
    public static final Set<ChangeType> MODIFY_DELETE = Collections.unmodifiableSet(EnumSet.of(ChangeType.MODIFY, ChangeType.DELETE));
    public static final Set<ItemPath> ALL_PATHS = Collections.singleton(ItemPath.EMPTY_PATH);

    public static final Set<CacheInvalidationEventSpecification> ALL_AVAILABLE_EVENTS = Collections.singleton(of(ObjectType.class, ALL_CHANGES));



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
