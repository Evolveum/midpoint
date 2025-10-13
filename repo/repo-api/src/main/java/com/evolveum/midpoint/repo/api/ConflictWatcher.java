/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.api;

import org.jetbrains.annotations.NotNull;

/**
 * A bit experimental.
 */
public interface ConflictWatcher {

    @NotNull
    String getOid();

    boolean hasConflict();

    void setExpectedVersion(String version);
}
