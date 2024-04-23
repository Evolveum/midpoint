/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.correlation;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.correlator.CandidateOwner;

class TestCandidateOwner {

    static final double EPSILON = 0.001;

    @NotNull private final String name;
    private final double confidence;

    TestCandidateOwner(@NotNull String name, double confidence) {
        this.name = name;
        this.confidence = confidence;
    }

    public static TestCandidateOwner of(CandidateOwner.ObjectBased candidateOwner) {
        return new TestCandidateOwner(
                candidateOwner.getValue().getName().getOrig(),
                candidateOwner.getConfidence());
    }

    @NotNull String getName() {
        return name;
    }

    double getConfidence() {
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
        TestCandidateOwner that = (TestCandidateOwner) o;
        return name.equals(that.name)
                && Math.abs(confidence - that.confidence) <= EPSILON;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "TestCandidateOwner{" +
                "name='" + name + '\'' +
                ", confidence=" + confidence +
                '}';
    }
}
