package com.evolveum.midpoint.common.mining.objects.analysis.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeAnalysisResultType;

//TODO think tmp
public class UserAttributeAnalyseCache {

    private final Map<String, RoleAnalysisAttributeAnalysisResultType> cache;

    public UserAttributeAnalyseCache() {
        this.cache = new ConcurrentHashMap<>();
    }

    public void put(String member, RoleAnalysisAttributeAnalysisResultType value) {
        cache.put(member, value);
    }

    public RoleAnalysisAttributeAnalysisResultType get(String member) {
        return cache.get(member);
    }

}
