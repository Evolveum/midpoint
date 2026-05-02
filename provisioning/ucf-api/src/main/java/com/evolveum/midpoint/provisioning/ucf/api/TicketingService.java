/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

import org.jetbrains.annotations.NotNull;

/**
 * Allows manipulating ITSM tickets as projections of midPoint case objects.
 *
 * Used to provide higher-level services (creating cases with projections) to low-level UCF manual connectors.
 */
public interface TicketingService {

    /** Creates a case with a ticket as a projection. */
    @NotNull String addCase(
            @NotNull CaseType aCase,
            @NotNull TicketingResourceSpec ticketingResourceSpec,
            @NotNull Task task,
            @NotNull OperationResult result);

    /** Retrieves the case, updating it with the current state of the ticket in remote ITSM system. */
    @NotNull CaseType getCase(
            @NotNull String caseOid,
            @NotNull Task task,
            @NotNull OperationResult result);

    /**
     * Specificiation of where the ticket should be created: the resource and its object type.
     * All relevant mappings must be in place for the mechanism to work.
     */
    record TicketingResourceSpec(
            @NotNull String resourceOid,
            @NotNull ResourceObjectTypeIdentification ticketingObjectType) {
    }
}
