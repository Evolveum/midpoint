/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint;

import com.evolveum.midpoint.repo.api.CacheInvalidationDetails;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.TerminateSessionEventType;

import java.util.List;

public class TerminateSessionEvent implements CacheInvalidationDetails {

    private List<String> principalOids;

    public List<String> getPrincipalOids() {
        return principalOids;
    }

    public void setPrincipalOids(List<String> principalOids) {
        this.principalOids = principalOids;
    }

    public static TerminateSessionEvent fromEventType(TerminateSessionEventType eventType) {
        if (eventType == null) {
            return null;
        }
        TerminateSessionEvent event = new TerminateSessionEvent();
        event.setPrincipalOids(eventType.getPrincipal());
        return event;
    }

    public TerminateSessionEventType toEventType() {
        if (principalOids == null || principalOids.isEmpty()) {
            return null;
        }
        TerminateSessionEventType eventType = new TerminateSessionEventType();
        eventType.getPrincipal().addAll(principalOids);
        return eventType;
    }
}
