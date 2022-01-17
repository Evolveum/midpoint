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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents external ID Match service that is invoked as part of correlation (and other processes)
 * within ID Match-based correlator.
 */
public interface IdMatchService {

    /**
     * Request a match for given resource object attributes.
     *
     * @param attributes Attributes of an account that should be matched.
     */
    @NotNull MatchingResult executeMatch(@NotNull ShadowAttributesType attributes, @NotNull OperationResult result)
            throws CommunicationException, SchemaException;

    /**
     * Resolves a pending match.
     *
     * @param attributes Attributes of an account that is pending (that was previously asked to being matched).
     * @param matchRequestId Identifier of the match request (if provided by the service)
     * @param referenceId What reference ID to assign. Null means "generate new".
     */
    void resolve(
            @NotNull ShadowAttributesType attributes,
            @Nullable String matchRequestId,
            @Nullable String referenceId,
            @NotNull OperationResult result) throws CommunicationException;
}
