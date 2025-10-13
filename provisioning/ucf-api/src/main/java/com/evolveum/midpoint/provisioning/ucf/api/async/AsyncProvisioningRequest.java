/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.ucf.api.async;

import com.evolveum.midpoint.util.DebugDumpable;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

/**
 * Request for asynchronous provisioning operation. It is to be sent to one or more asynchronous provisioning targets.
 */
@VisibleForTesting // just to provide mock implementations
public interface AsyncProvisioningRequest extends DebugDumpable {

    /**
     * @return String representation of the request.
     */
    @NotNull String asString();

    default @NotNull byte[] asUtf8Bytes() {
        return asString().getBytes(StandardCharsets.UTF_8);
    }
}
