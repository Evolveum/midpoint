/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.builtin;

import com.evolveum.midpoint.provisioning.ucf.api.ConfigurationItem;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

/**
 * @author semancik
 *
 */
public class ManualConnectorConfiguration {

    /** If present, denotes a resource we will use to manage tickets on a real ITSM system. */
    private String ticketingResourceOid;

    /** Shadow kind for the object type for tickets. */
    private String ticketingObjectKind = ShadowKindType.GENERIC.value();

    /** Intent for the object type for tickets. */
    private String ticketingObjectIntent = "ticket";

    private String defaultAssignee;

    @ConfigurationItem
    public String getDefaultAssignee() {
        return defaultAssignee;
    }

    public void setDefaultAssignee(String defaultAssignee) {
        this.defaultAssignee = defaultAssignee;
    }

    @ConfigurationItem
    public String getTicketingResourceOid() {
        return ticketingResourceOid;
    }

    public void setTicketingResourceOid(String ticketingResourceOid) {
        this.ticketingResourceOid = ticketingResourceOid;
    }

    @ConfigurationItem
    public String getTicketingObjectKind() {
        return ticketingObjectKind;
    }

    public void setTicketingObjectKind(String ticketingObjectKind) {
        this.ticketingObjectKind = ticketingObjectKind;
    }

    @ConfigurationItem
    public String getTicketingObjectIntent() {
        return ticketingObjectIntent;
    }

    public void setTicketingObjectIntent(String ticketingObjectIntent) {
        this.ticketingObjectIntent = ticketingObjectIntent;
    }
}
