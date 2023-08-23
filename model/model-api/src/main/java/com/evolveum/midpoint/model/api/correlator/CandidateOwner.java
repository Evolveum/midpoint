/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import java.io.Serializable;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

/**
 * A candidate owner along with its confidence value (a number between 0 and 1, inclusive).
 *
 * Equals/hashCode contract: looks after {@link #oid} and {@link #confidence} only.
 *
 * @see CandidateOwnersMap
 */
public class CandidateOwner implements Serializable {

    @NotNull private final String oid;
    @NotNull private final ObjectType object;
    private double confidence;

    /**
     * ID of the candidate in the external system, e.g. ID Match.
     */
    @Nullable private final String externalId;

    public CandidateOwner(@NotNull ObjectType object, @Nullable String externalId, double confidence) {
        this.oid = MiscUtil.requireNonNull(
                object.getOid(),
                () -> new IllegalArgumentException("No oid of " + object));
        this.object = object;
        this.externalId = externalId;
        argCheck(confidence >= 0 && confidence <= 1, "Invalid confidence value: %s", confidence);
        this.confidence = confidence;
    }

    public @NotNull String getOid() {
        return oid;
    }

    public @NotNull ObjectType getObject() {
        return object;
    }

    public double getConfidence() {
        return confidence;
    }

    public void increaseConfidence(double confidence) {
        this.confidence += confidence;
    }

    public @Nullable String getExternalId() {
        return externalId;
    }

    @Override
    public String toString() {
        return "CandidateOwner{" +
                "object=" + object +
                (externalId != null ? ", externalId=" + externalId : "") +
                ", confidence=" + confidence +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CandidateOwner that = (CandidateOwner) o;
        return oid.equals(that.oid)
                && confidence == that.confidence;
    }

    @Override
    public int hashCode() {
        return Objects.hash(oid, confidence);
    }
}
