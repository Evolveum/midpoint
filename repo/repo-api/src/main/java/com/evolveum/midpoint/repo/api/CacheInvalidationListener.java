/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.api;

import java.util.Collection;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FunctionLibraryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

import org.jetbrains.annotations.Nullable;

/**
 * A listener that is notified about object change (cache invalidation) events - events describing that an object
 * of given type was added, modified, or deleted. Changes may originate on the local node, or may come from a different
 * node in the cluster.
 *
 * Note for implementers:
 *
 * - A module implementing this interface may be a local cache for specific type of objects, like for
 * {@link SystemConfigurationType}.
 * - Or, it may be a module that derives information from specific objects, and caches that information (like compiled scripts
 * code depending on {@link FunctionLibraryType}, or archetype manager depending on {@link ArchetypeType}
 * and {@link SystemConfigurationType}, etc). Such a module needs to be notified when one of its inputs has changed, so it can
 * invalidate its own cache.
 *
 * @see ClusterwideCacheInvalidationListener
 * @see CacheDiagnostics
 */
public interface CacheInvalidationListener {

    /**
     * Returns event specifications that this listener is interested in. For example, a module that uses
     * {@link FunctionLibraryType} objects may be interested only in invalidation events related to this type of objects.
     *
     * It is used as a kind of optimization and code simplification feature - the listener can declare what kinds of events
     * it is interesting in, and the dispatcher will filter out all irrelevant events.
     *
     * When unsure, just return {@link CacheInvalidationEventSpecification#ALL_AVAILABLE_EVENTS}.
     */
    Collection<CacheInvalidationEventSpecification> getEventSpecifications();

    /**
     * Signals that given object (or more objects) was changed.
     *
     * @param type Type of object ({@code null} means all types).
     * @param oid OID of object ({@code null} means all object(s) of given type(s)).
     * @param context More details regarding invalidation request (may be missing for events from remote node).
     */
    <O extends ObjectType> void invalidate(
            @Nullable Class<O> type,
            @Nullable String oid,
            @Nullable CacheInvalidationContext context);
}
