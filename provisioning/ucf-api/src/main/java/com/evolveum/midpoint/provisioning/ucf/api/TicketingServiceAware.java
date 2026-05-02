/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

/** Connector that can accept a {@link TicketingService}. */
public interface TicketingServiceAware {

    void setTicketingService(TicketingService ticketingService);
}
