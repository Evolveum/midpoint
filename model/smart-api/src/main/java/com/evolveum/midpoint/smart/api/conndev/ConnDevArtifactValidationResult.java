/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.smart.api.conndev;

import java.io.Serializable;
import java.util.List;

/**
 * Result of the connector script validation executed by the connector itself
 * (development mode only). For a failed validation, {@code errors} holds one
 * entry per script that failed — usually just the one being validated, but a
 * schema candidate that itself builds cleanly can still break an
 * already-deployed operation script referencing it, in which case there is
 * more than one entry, each identifying its own {@code source}.
 */
public record ConnDevArtifactValidationResult(boolean ok, List<Error> errors) implements Serializable {

    /**
     * A single validation failure. {@code phase} distinguishes where it
     * occurred: {@code compile}, {@code evaluate}, {@code build} or
     * {@code initialization}. {@code source} is the file the failure
     * occurred in, if different from the artifact being validated (e.g. a
     * deployed operation script broken by a schema candidate).
     */
    public record Error(String phase, String message, Integer line, Integer column, String source) implements Serializable {
    }

    public static ConnDevArtifactValidationResult success() {
        return new ConnDevArtifactValidationResult(true, List.of());
    }

    public static ConnDevArtifactValidationResult errors(List<Error> errors) {
        return new ConnDevArtifactValidationResult(false, errors);
    }
}
