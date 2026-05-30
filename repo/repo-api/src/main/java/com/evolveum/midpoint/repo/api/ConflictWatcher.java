/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.api;

import com.evolveum.midpoint.schema.result.OperationResult;

import org.jetbrains.annotations.NotNull;

/**
 * Detects if a repository object was modified in an unexpected way, typically by a different thread.
 *
 * Unlike {@link VersionPrecondition}, this is not used to prevent modifications, but rather to detect that they
 * happened and so the caller can later act upon it.
 *
 * A watcher is connected to a single object, specified at the creation time. See {@link #getOid()}.
 *
 * @see RepositoryService#createAndRegisterConflictWatcher(String)
 * @see RepositoryService#hasConflict(ConflictWatcher, OperationResult)
 */
public interface ConflictWatcher {

    /** OID of the object this watcher is watching. */
    @NotNull String getOid();

    /** Was a conflict detected on the object by this watcher? */
    boolean hasConflict();

    /**
     * Tells the watcher to expect given version of the object being watched. Calling this method is optional:
     * If expected version is not provided, it will be determined automatically e.g. on the first MODIFY operation.
     */
    void setExpectedVersion(String version);

    /** {@code true} if the watcher was initialized, so it actually detects the conflicts. */
    boolean isInitialized();

    /**
     * What is the version we are currently expected? Relevant only if {@link #isInitialized()} is {@code true}.
     *
     * NOTE: This is a bit of encapsulation breakage. The conflict detection may be based on things other that the object
     * version. However, for some reason, we (probably temporarily) need this information.
     */
    int getExpectedVersion();
}
