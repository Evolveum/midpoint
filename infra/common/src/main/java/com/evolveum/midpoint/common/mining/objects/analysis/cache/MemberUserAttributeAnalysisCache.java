package com.evolveum.midpoint.common.mining.objects.analysis.cache;

import com.evolveum.midpoint.common.mining.objects.analysis.AttributePathResult;
import com.evolveum.midpoint.prism.path.ItemPath;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//TODO think tmp
public class MemberUserAttributeAnalysisCache {

    private final Map<String, Map<ItemPath, AttributePathResult>> cache;

    public MemberUserAttributeAnalysisCache() {
        this.cache = new ConcurrentHashMap<>();
    }

    public void putPathResult(String userId, ItemPath key, AttributePathResult value) {
        Map<ItemPath, AttributePathResult> userMap = cache.computeIfAbsent(userId, k -> new HashMap<>());
        userMap.put(key, value);
    }

    public AttributePathResult getPathResult(String userId, String key) {
        Map<ItemPath, AttributePathResult> userMap = cache.get(userId);
        if (userMap != null) {
            return userMap.get(key);
        }
        return null;
    }

    public Map<ItemPath, AttributePathResult> getUserResult(String userId) {
        return cache.get(userId);
    }

    public void  putUserResult(String userId, Map<ItemPath, AttributePathResult> value) {
        cache.put(userId, value);
    }
}
