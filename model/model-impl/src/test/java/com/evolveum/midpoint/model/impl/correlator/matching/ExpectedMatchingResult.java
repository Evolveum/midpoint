/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.matching;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Parsed form of expected result of the matching process (including operator choice).
 */
public class ExpectedMatchingResult {

    private boolean isNew;
    private Integer equalTo;
    UncertainWithResolution uncertainWithResolution;

    ExpectedMatchingResult(String stringValue) {
        if (stringValue == null) {
            return; // no checking
        }
        if (stringValue.equals("new")) {
            isNew = true;
            return;
        }
        if (stringValue.startsWith("=")) {
            equalTo = Integer.parseInt(stringValue.substring(1));
            return;
        }
        if (stringValue.startsWith("?")) {
            uncertainWithResolution = new UncertainWithResolution(stringValue);
            return;
        }
        throw new AssertionError("Unsupported expected matching result: " + stringValue);
    }

    public boolean isEqualTo() {
        return equalTo != null;
    }

    Integer getEqualTo() {
        return equalTo;
    }

    public boolean isNew() {
        return isNew;
    }

    boolean isUncertain() {
        return uncertainWithResolution != null;
    }

    /**
     * Format: ?a,b,c:d
     *
     * i.e.
     *
     * - should offer a, b, c (account numbers)
     * - we select the account numbered "d" (or "new" for new identity)
     */
    public static class UncertainWithResolution {
        @NotNull private final Set<Integer> options = new HashSet<>();
        @Nullable private final Integer operatorResponse; // null = new

        UncertainWithResolution(String stringValue) {
            String[] segments = stringValue.split(":");
            argCheck(segments.length == 2, "Wrong # of segments in %s: %s", stringValue, segments);
            Arrays.stream(segments[0].substring(1).split(","))
                    .map(Integer::parseInt)
                    .forEach(options::add);
            if ("new".equals(segments[1])) {
                operatorResponse = null;
            } else {
                operatorResponse = Integer.parseInt(segments[1]);
            }
        }

        public @NotNull Set<Integer> getOptions() {
            return options;
        }

        @Nullable Integer getOperatorResponse() {
            return operatorResponse;
        }

        @Override
        public String toString() {
            return "UncertainWithResolution{" +
                    "options=" + options +
                    ", operatorResponse=" + operatorResponse +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "ExpectedMatchingResult{" +
                "isNew=" + isNew +
                ", equalTo=" + equalTo +
                ", uncertainWithResolution=" + uncertainWithResolution +
                '}';
    }
}
