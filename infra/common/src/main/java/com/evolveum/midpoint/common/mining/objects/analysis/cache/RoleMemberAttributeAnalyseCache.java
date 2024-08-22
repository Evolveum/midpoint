package com.evolveum.midpoint.common.mining.objects.analysis.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeAnalysisResult;

//TODO think tmp
public class RoleMemberAttributeAnalyseCache {

    private final Map<String, RoleAnalysisAttributeAnalysisResult> cache;

    public RoleMemberAttributeAnalyseCache() {
        this.cache = new ConcurrentHashMap<>();
    }

    public void put(String role, RoleAnalysisAttributeAnalysisResult value) {
        cache.put(role, value);
    }

    public RoleAnalysisAttributeAnalysisResult get(String role) {
        return cache.get(role);
    }
}
