/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;

import org.springframework.context.ApplicationEvent;

/**
 * @author skublik
 */

public class RemoveUnusedSecurityFilterEvent extends ApplicationEvent {
    private MidpointAuthentication mpAuthentication;

    public RemoveUnusedSecurityFilterEvent(Object source, MidpointAuthentication mpAuthentication) {
        super(source);
        this.mpAuthentication = mpAuthentication;
    }

    public MidpointAuthentication getMpAuthentication() {
        return mpAuthentication;
    }
}
