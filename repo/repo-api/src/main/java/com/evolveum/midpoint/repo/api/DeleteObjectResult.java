/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.api;

/**
 *  Contains information about object deletion result; primarily needed by repository caching algorithms.
 *  Because it is bound to the current (SQL) implementation of the repository, avoid using this information
 *  for any other purposes.
 *
 *  EXPERIMENTAL.
 */
public class DeleteObjectResult {

    private final String objectTextRepresentation;
    private final String language;

    public DeleteObjectResult(String objectTextRepresentation, String language) {
        this.objectTextRepresentation = objectTextRepresentation;
        this.language = language;
    }

    /**
     * The textual representation of the object as stored in repository. It is to be parsed when really
     * necessary. Note that it does not contain information that is stored elsewhere (user photo, lookup table rows,
     * certification cases, task result, etc).
     */
    public String getObjectTextRepresentation() {
        return objectTextRepresentation;
    }

    /**
     * Language in which the text representation is encoded.
     */
    public String getLanguage() {
        return language;
    }
}
