/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.sysperf;

import com.evolveum.midpoint.test.DummyTestResource;

import org.jetbrains.annotations.NotNull;

record OperationDelay(int offset, int range) {

    private static final String OFFSET_SUFFIX = ".operation-delay-offset";
    private static final String RANGE_SUFFIX = ".operation-delay-range";

    static @NotNull OperationDelay fromSystemProperties(String propPrefix) {
        return new OperationDelay(
                Integer.parseInt(System.getProperty(propPrefix + OFFSET_SUFFIX, "0")),
                Integer.parseInt(System.getProperty(propPrefix + RANGE_SUFFIX, "0")));
    }

    void applyTo(DummyTestResource resource) {
        resource.getDummyResource().setOperationDelayOffset(offset);
        resource.getDummyResource().setOperationDelayRange(range);
    }

    @Override
    public String toString() {
        return "{offset=%d, range=%d}".formatted(offset, range);
    }
}
