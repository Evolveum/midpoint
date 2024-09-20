/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;

import com.evolveum.midpoint.prism.Containerable;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * A candidate owner along with its confidence value (a number between 0 and 1, inclusive).
 *
 * @see CandidateOwners
 */
public abstract class CandidateOwner implements Serializable {

    final double confidence;

    /**
     * ID of the candidate in the external system, e.g. ID Match.
     */
    @Nullable private final String externalId;

    public CandidateOwner(@Nullable String externalId, double confidence) {
        this.externalId = externalId;
        argCheck(confidence >= 0 && confidence <= 1, "Invalid confidence value: %s", confidence);
        this.confidence = confidence;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Collection<ObjectBased> ensureObjectBased(Collection<CandidateOwner> candidateOwners) {
        for (CandidateOwner candidateOwner : candidateOwners) {
            stateCheck(candidateOwner instanceof ObjectBased,
                    "candidateOwner must be an ObjectBased object: %s", candidateOwner);
        }
        return (Collection) candidateOwners;
    }

    abstract public @NotNull Containerable getValue();

    public double getConfidence() {
        return confidence;
    }

    public @Nullable String getExternalId() {
        return externalId;
    }

    @Override
    public String toString() {
        return "CandidateOwner{" +
                "value=" + getValue() +
                (externalId != null ? ", externalId=" + externalId : "") +
                ", confidence=" + confidence +
                '}';
    }

    /** True if this record refers to the same candidate owner identity as the provided one. */
    public abstract boolean matchesIdentity(CandidateOwner candidateOwner);

    /**
     * Traditional, object-based owner.
     *
     * Equals/hashCode contract: looks after {@link #oid} and {@link #confidence} only.
     */
    public static class ObjectBased extends CandidateOwner {
        @NotNull private final String oid;
        @NotNull private final ObjectType object;

        public ObjectBased(@NotNull ObjectType object, @Nullable String externalId, double confidence) {
            super(externalId, confidence);
            this.oid = MiscUtil.requireNonNull(
                    object.getOid(),
                    () -> new IllegalArgumentException("No oid of " + object));
            this.object = object;
        }

        public @NotNull String getOid() {
            return oid;
        }

        @Override
        public @NotNull ObjectType getValue() {
            return object;
        }

        @Override
        public boolean matchesIdentity(CandidateOwner candidateOwner) {
            return candidateOwner instanceof ObjectBased objectBased
                    && oid.equals(objectBased.getOid());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            var that = (ObjectBased) o;
            return oid.equals(that.oid)
                    && confidence == that.confidence;
        }

        @Override
        public int hashCode() {
            return Objects.hash(oid, confidence);
        }
    }

    public static class ValueBased extends CandidateOwner {

        @NotNull private final Containerable value;

        ValueBased(@NotNull Containerable value, @Nullable String externalId, double confidence) {
            super(externalId, confidence);
            this.value = value;
        }

        @Override
        public @NotNull Containerable getValue() {
            return value;
        }

        @Override
        public boolean matchesIdentity(CandidateOwner candidateOwner) {
            return candidateOwner instanceof ValueBased valueBased
                    && value.equals(valueBased.value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ValueBased valueBased = (ValueBased) o;
            return Objects.equals(value, valueBased.value)
                    && confidence == valueBased.confidence;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value, confidence);
        }
    }
}
