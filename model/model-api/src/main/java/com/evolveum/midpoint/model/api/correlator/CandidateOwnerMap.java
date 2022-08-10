/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class CandidateOwnerMap<O extends ObjectType> implements Serializable {

    @NotNull private final Map<String, CandidateOwner<O>> map = new HashMap<>();

    public static <O extends ObjectType> CandidateOwnerMap<O> from(Collection<CandidateOwner<O>> source) {
        CandidateOwnerMap<O> map = new CandidateOwnerMap<>();
        source.forEach(map::put);
        return map;
    }

    public void put(@NotNull O candidate, @Nullable Double confidence) {
        put(new CandidateOwner<>(candidate, confidence));
    }

    public void put(@NotNull CandidateOwner<O> candidateOwner) {
        map.put(candidateOwner.getOid(), candidateOwner);
    }

    public @NotNull Collection<CandidateOwner<O>> values() {
        return map.values();
    }

    public Double add(CandidateOwner<O> candidateOwner) {
        CandidateOwner<O> existing = map.get(candidateOwner.getOid());
        if (existing != null) {
            return existing.addMatching(candidateOwner);
        } else {
            put(candidateOwner);
            return candidateOwner.getConfidence();
        }
    }

    public @NotNull CandidateOwnerMap<O> copyAtLeast(double threshold) {
        CandidateOwnerMap<O> targetMap = new CandidateOwnerMap<>();
        for (CandidateOwner<O> value : values()) {
            if (value.isAtLeast(threshold)) {
                targetMap.put(value);
            }
        }
        return targetMap;
    }

    public Collection<CandidateOwner<O>> selectWithConfidenceAtLeast(double threshold) {
        return map.values().stream()
                .filter(candidateOwner -> candidateOwner.isAtLeast(threshold))
                .collect(Collectors.toSet());
    }
}
