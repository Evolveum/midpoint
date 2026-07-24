/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.smart.api.conndev;

import java.io.Serializable;

/**
 * Result of the connector script validation executed by the connector itself
 * (development mode only). For failed validation, {@code phase} distinguishes
 * where the script failed: {@code compile}, {@code evaluate} or {@code build}.
 */
public record ConnDevArtifactValidationResult(boolean ok, String phase, String message, Integer line, Integer column)
        implements Serializable {

    public static ConnDevArtifactValidationResult success() {
        return new ConnDevArtifactValidationResult(true, null, null, null, null);
    }

    public static ConnDevArtifactValidationResult error(String phase, String message, Integer line, Integer column) {
        return new ConnDevArtifactValidationResult(false, phase, message, line, column);
    }
}
