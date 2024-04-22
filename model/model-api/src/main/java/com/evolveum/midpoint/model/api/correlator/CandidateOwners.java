/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import com.evolveum.midpoint.model.api.correlator.CandidateOwner.ObjectBased;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Collection of (unique) candidate owners. When adding an owner that's already there, we just update the record.
 *
 * For objects, the OID is the primary key.
 * For all other values, the whole value is the key.
 */
public class CandidateOwners implements Serializable, DebugDumpable {

    @NotNull private final Collection<CandidateOwner> owners = new HashSet<>();

    public static CandidateOwners from(Collection<? extends CandidateOwner> source) {
        CandidateOwners map = new CandidateOwners();
        source.forEach(map::put);
        return map;
    }

    public void put(@NotNull Containerable candidate, @Nullable String externalId, double confidence) {
        if (candidate instanceof ObjectType object) {
            putObject(object, externalId, confidence);
        } else {
            putValue(candidate, externalId, confidence);
        }
    }

    public void putObject(@NotNull ObjectType candidate, @Nullable String externalId, double confidence) {
        put(new ObjectBased(candidate, externalId, confidence));
    }

    public void putValue(@NotNull Containerable value, @Nullable String externalId, double confidence) {
        put(new CandidateOwner.ValueBased(value, externalId, confidence));
    }

    private void put(@NotNull CandidateOwner candidateOwner) {
        owners.removeIf(o -> o.matchesIdentity(candidateOwner));
        owners.add(candidateOwner);
    }

    public @NotNull Collection<CandidateOwner> values() {
        return Set.copyOf(owners);
    }

    public @NotNull Collection<ObjectBased> objectBasedValues() {
        return CandidateOwner.ensureObjectBased(
                Set.copyOf(owners));
    }

    public boolean isEmpty() {
        return owners.isEmpty();
    }

    public @NotNull Set<String> getCandidateOids() {
        return values().stream()
                .filter(owner -> owner instanceof ObjectBased)
                .map(owner -> (ObjectBased) owner)
                .map(ObjectBased::getOid)
                .collect(Collectors.toSet());
    }

    public Collection<CandidateOwner> selectWithConfidenceAtLeast(double threshold) {
        return owners.stream()
                .filter(candidateOwner -> candidateOwner.getConfidence() >= threshold)
                .collect(Collectors.toSet());
    }

    @Override
    public String debugDump(int indent) {
        // improve if needed
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.toStringCollection(sb, owners, indent + 1);
        return sb.toString();
    }

    public void clear() {
        owners.clear();
    }

    public void replaceWith(CandidateOwners other) {
        owners.clear();
        owners.addAll(other.values());
    }
}
