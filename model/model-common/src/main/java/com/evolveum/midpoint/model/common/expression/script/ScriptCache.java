/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script;

import java.util.HashMap;
import java.util.Map;

import com.evolveum.midpoint.schema.expression.ExpressionProfile;

/**
 * Cache for compiled scripts and interpreters, aware of expression profiles.
 *
 * @param <I> interpreter (script execution runtime)
 * @param <C> compiled code
 * @param <K> code caching key (e.g. source code)
 * @author Radovan Semancik
 */
public class ScriptCache<I,C,K> {

    /**
     * Caching prepared interpreters (execution runtimes) for scripts.
     * We assume that the runtime depends only on expression profile, not the code or variables or anything else.
     */
    private final Map<String, I> interpreterCache = new HashMap<>();

    /**
     * Caching compiled scripts.
     * Cache is segmented per expression profile.
     * Profile ID (key; nullable) -> Script caching key (e.g. source code) -> Compiled code (value)
     */
    private final Map<String, Map<K, C>> codeCache = new HashMap<>();

    public synchronized I getInterpreter(ExpressionProfile profile) {
        return interpreterCache.get(getProfileKey(profile));
    }

    public synchronized void putInterpreter(ExpressionProfile profile, I interpreter) {
        interpreterCache.put(getProfileKey(profile), interpreter);
    }

    synchronized C getCode(ExpressionProfile profile, K cachingKey) {
        String profileKey = getProfileKey(profile);
        Map<K, C> profileCache = codeCache.get(profileKey);
        return profileCache != null ? profileCache.get(cachingKey) : null;
    }

    synchronized void putCode(ExpressionProfile profile, K scriptCachingKey, C compiledCode) {
        String profileKey = getProfileKey(profile);
        Map<K, C> profileCache = codeCache.computeIfAbsent(profileKey, k -> new HashMap<>());
        profileCache.put(scriptCachingKey, compiledCode);
    }

    private String getProfileKey(ExpressionProfile profile) {
        if (profile == null) {
            return null;
        } else {
            return profile.getIdentifier();
        }
    }

    public synchronized void clear() {
        codeCache.clear();
    }
}
