package com.evolveum.midpoint.common.mining.objects.analysis.cache;

import com.evolveum.midpoint.common.mining.objects.analysis.AttributePathResult;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//TODO think tmp
public class MemberUserAttributeAnalysisCache {

    private final Map<String, Map<String, AttributePathResult>> cache;

    public MemberUserAttributeAnalysisCache() {
        this.cache = new ConcurrentHashMap<>();
    }

    public void putPathResult(String userId, String key, AttributePathResult value) {
        Map<String, AttributePathResult> userMap = cache.computeIfAbsent(userId, k -> new HashMap<>());
        userMap.put(key, value);
    }

    public AttributePathResult getPathResult(String userId, String key) {
        Map<String, AttributePathResult> userMap = cache.get(userId);
        if (userMap != null) {
            return userMap.get(key);
        }
        return null;
    }

    public Map<String, AttributePathResult> getUserResult(String userId) {
        return cache.get(userId);
    }

    public void  putUserResult(String userId, Map<String, AttributePathResult> value) {
        cache.put(userId, value);
    }
}
