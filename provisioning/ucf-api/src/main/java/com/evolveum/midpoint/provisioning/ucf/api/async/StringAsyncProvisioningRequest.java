/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api.async;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;

/**
 * Provisioning request represented by simple string value.
 */
@VisibleForTesting // just to provide mock implementations
public class StringAsyncProvisioningRequest implements AsyncProvisioningRequest {

    @NotNull private final String stringValue;

    private StringAsyncProvisioningRequest(@NotNull String stringValue) {
        this.stringValue = stringValue;
    }

    @NotNull public static StringAsyncProvisioningRequest of(@NotNull String value) {
        return new StringAsyncProvisioningRequest(value);
    }

    @Override
    public String debugDump(int indent) {
        return stringValue;
    }

    public @NotNull String getStringValue() {
        return stringValue;
    }

    @Override
    public @NotNull String asString() {
        return stringValue;
    }
}
