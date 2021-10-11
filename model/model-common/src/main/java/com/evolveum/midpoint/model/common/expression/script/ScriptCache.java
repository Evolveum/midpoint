/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.script;

import java.util.HashMap;
import java.util.Map;

import com.evolveum.midpoint.schema.expression.ExpressionProfile;

/**
 * Cache for compiled scripts and interpreters, aware of expression profiles.
 *
 * @param <C> compiled code
 * @author Radovan Semancik
 */
public class ScriptCache<I,C> {

    private final Map<String, I> interpreterCache = new HashMap<>();
    private final Map<String, Map<String, C>> codeCache = new HashMap<>();

    public synchronized I getInterpreter(ExpressionProfile profile) {
        return interpreterCache.get(getProfileKey(profile));
    }

    public synchronized void putInterpreter(ExpressionProfile profile, I interpreter) {
        interpreterCache.put(getProfileKey(profile), interpreter);
    }

    public synchronized C getCode(ExpressionProfile profile, String sourceCodeKey) {
        String profileKey = getProfileKey(profile);
        Map<String, C> profileCache = codeCache.get(profileKey);
        if (profileCache == null) {
            return null;
        }
        return profileCache.get(sourceCodeKey);
    }

    public synchronized void putCode(ExpressionProfile profile, String sourceCodeKey, C compiledCode) {
        String profileKey = getProfileKey(profile);
        Map<String, C> profileCache = codeCache.get(profileKey);
        if (profileCache == null) {
            profileCache = new HashMap<>();
            codeCache.put(profileKey, profileCache);
        }
        profileCache.put(sourceCodeKey, compiledCode);
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
