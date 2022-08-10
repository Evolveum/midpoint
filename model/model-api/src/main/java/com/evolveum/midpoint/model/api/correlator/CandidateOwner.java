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

/**
 * Equals/hashCode contract: looks after {@link #oid} and {@link #confidence} only.
 */
public class CandidateOwner<O extends ObjectType> implements Serializable {

    @NotNull private final String oid;
    @NotNull private final O object;
    private Double confidence;

    CandidateOwner(@NotNull O object, Double confidence) {
        this.oid = MiscUtil.requireNonNull(
                object.getOid(),
                () -> new IllegalArgumentException("No oid of " + object));
        this.object = object;
        this.confidence = confidence;
    }

    public @NotNull String getOid() {
        return oid;
    }

    public @NotNull O getObject() {
        return object;
    }

    public Double getConfidence() {
        return confidence;
    }

    @Override
    public String toString() {
        return "CandidateOwner{" +
                "object=" + object +
                ", confidence=" + confidence +
                '}';
    }

    Double addMatching(CandidateOwner<O> another) {
        assert oid.equals(another.getOid());
        Double anotherConfidence = another.getConfidence();
        if (anotherConfidence != null) {
            if (confidence == null) {
                confidence = anotherConfidence;
            } else {
                confidence += anotherConfidence;
            }
        }
        return confidence;
    }

    public boolean isAtLeast(double threshold) {
        return threshold <= 0
                || confidence != null && confidence >= threshold;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CandidateOwner<?> that = (CandidateOwner<?>) o;
        return oid.equals(that.oid) && Objects.equals(confidence, that.confidence);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oid, confidence);
    }
}
