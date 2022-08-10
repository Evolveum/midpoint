/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.correlation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

class CandidateOwner {

    @NotNull private final String name;
    @Nullable private final Double confidence;

    CandidateOwner(@NotNull String name, @Nullable Double confidence) {
        this.name = name;
        this.confidence = confidence;
    }

    @NotNull String getName() {
        return name;
    }

    @Nullable Double getConfidence() {
        return confidence;
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
        return name.equals(that.name)
                && Objects.equals(confidence, that.confidence);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, confidence);
    }

    @Override
    public String toString() {
        return "CandidateOwner{" +
                "name='" + name + '\'' +
                ", confidence=" + confidence +
                '}';
    }
}
