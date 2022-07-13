/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.api;

import com.evolveum.midpoint.prism.delta.ChangeType;

/**
 * Contains information about object deletion result; primarily needed by repository caching algorithms.
 * Because it is bound to the current (SQL) implementation of the repository, avoid using this information
 * for any other purposes.
 * <p>
 * EXPERIMENTAL.
 */
public class DeleteObjectResult implements RepositoryOperationResult {

    private final String objectTextRepresentation;

    public DeleteObjectResult(String objectTextRepresentation) {
        this.objectTextRepresentation = objectTextRepresentation;
    }

    /**
     * The textual representation of the object as stored in repository. It is to be parsed when really
     * necessary. Note that it does not contain information that is stored elsewhere (user photo, lookup table rows,
     * certification cases, task result, etc).
     */
    public String getObjectTextRepresentation() {
        return objectTextRepresentation;
    }

    @Override
    public ChangeType getChangeType() {
        return ChangeType.DELETE;
    }
}
