/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator.idmatch;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.util.exception.SecurityViolationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents external ID Match service that is invoked as part of correlation (and other processes)
 * within ID Match-based correlator.
 */
public interface IdMatchService {

    /**
     * Request a match for given resource object attributes.
     */
    @NotNull MatchingResult executeMatch(@NotNull MatchingRequest request, @NotNull OperationResult result)
            throws CommunicationException, SchemaException, SecurityViolationException;

    /**
     * Updates an object in ID Match service.
     */
    void update(
            @NotNull IdMatchObject idMatchObject,
            @Nullable String referenceId,
            @NotNull OperationResult result)
            throws CommunicationException, SchemaException, SecurityViolationException;

    /**
     * Resolves a pending match.
     *
     * @param idMatchObject Object whose pending match is to be updated.
     * @param matchRequestId Identifier of the match request (if provided by the service)
     * @param referenceId What reference ID to assign. Null means "generate new".
     *
     * @return Current reference ID
     */
    @NotNull String resolve(
            @NotNull IdMatchObject idMatchObject,
            @Nullable String matchRequestId,
            @Nullable String referenceId,
            @NotNull OperationResult result) throws CommunicationException, SchemaException, SecurityViolationException;
}
