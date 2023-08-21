/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class CandidateOwnersMap implements Serializable, DebugDumpable {

    @NotNull private final Map<String, CandidateOwner> map = new HashMap<>();

    public static CandidateOwnersMap from(Collection<CandidateOwner> source) {
        CandidateOwnersMap map = new CandidateOwnersMap();
        source.forEach(map::put);
        return map;
    }

    public void put(@NotNull ObjectType candidate, @Nullable String externalId, double confidence) {
        put(new CandidateOwner(candidate, externalId, confidence));
    }

    public void put(@NotNull CandidateOwner candidateOwner) {
        map.put(candidateOwner.getOid(), candidateOwner);
    }

    public @NotNull Collection<CandidateOwner> values() {
        return map.values();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public Collection<CandidateOwner> selectWithConfidenceAtLeast(double threshold) {
        return map.values().stream()
                .filter(candidateOwner -> candidateOwner.getConfidence() >= threshold)
                .collect(Collectors.toSet());
    }

    @Override
    public String debugDump(int indent) {
        // improve if needed
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.toStringCollection(sb, map.values(), indent + 1);
        return sb.toString();
    }

    public void mergeWith(CandidateOwnersMap other) {
        for (CandidateOwner candidateOwner : other.values()) {
            CandidateOwner existing = map.get(candidateOwner.getOid());
            if (existing == null) {
                map.put(candidateOwner.getOid(), candidateOwner);
            } else {
                existing.increaseConfidence(candidateOwner.getConfidence());
            }
        }
    }
}
