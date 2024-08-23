package com.evolveum.midpoint.common.mining.objects.analysis.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//TODO think tmp
public class RoleMemberCountCache {

    private final Map<String, Integer> cache;

    public RoleMemberCountCache() {
        this.cache = new ConcurrentHashMap<>();
    }

    public void put(String member, Integer value) {
        cache.put(member, value);
    }

    public Integer get(String role) {
        return cache.get(role);
    }

}
