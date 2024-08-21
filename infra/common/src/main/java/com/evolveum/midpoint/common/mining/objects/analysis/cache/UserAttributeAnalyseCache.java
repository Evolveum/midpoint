package com.evolveum.midpoint.common.mining.objects.analysis.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeAnalysisResult;

//TODO think tmp
public class UserAttributeAnalyseCache {

    private final Map<String, RoleAnalysisAttributeAnalysisResult> cache;

    public UserAttributeAnalyseCache() {
        this.cache = new ConcurrentHashMap<>();
    }

    public void put(String member, RoleAnalysisAttributeAnalysisResult value) {
        cache.put(member, value);
    }

    public RoleAnalysisAttributeAnalysisResult get(String member) {
        return cache.get(member);
    }

}
