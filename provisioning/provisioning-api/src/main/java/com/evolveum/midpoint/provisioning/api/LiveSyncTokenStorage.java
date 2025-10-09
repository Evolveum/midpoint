/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Manages the storage of a live sync token.
 */
public interface LiveSyncTokenStorage {

    /**
     * Gets the value of the stored token.
     *
     * We assume this is simple operation, e.g. no repository access is expected.
     * (Therefore no operation result is provided.)
     */
    LiveSyncToken getToken();

    /**
     * Stores the value of the token. Usually involves repository write.
     */
    void setToken(LiveSyncToken token, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException;
}
