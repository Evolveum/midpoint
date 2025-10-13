/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.common.mining.objects.analysis.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeAnalysisResultType;

//TODO think tmp
public class RoleMemberAttributeAnalyseCache {

    private final Map<String, RoleAnalysisAttributeAnalysisResultType> cache;

    public RoleMemberAttributeAnalyseCache() {
        this.cache = new ConcurrentHashMap<>();
    }

    public void put(String role, RoleAnalysisAttributeAnalysisResultType value) {
        cache.put(role, value);
    }

    public RoleAnalysisAttributeAnalysisResultType get(String role) {
        return cache.get(role);
    }
}
