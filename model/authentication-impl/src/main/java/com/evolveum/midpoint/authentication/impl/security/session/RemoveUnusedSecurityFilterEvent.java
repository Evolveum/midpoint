/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.security.session;

import com.evolveum.midpoint.authentication.api.authentication.MidpointAuthentication;

import org.springframework.context.ApplicationEvent;

/**
 * @author skublik
 */

public class RemoveUnusedSecurityFilterEvent extends ApplicationEvent {

    private final MidpointAuthentication mpAuthentication;

    public RemoveUnusedSecurityFilterEvent(Object source, MidpointAuthentication mpAuthentication) {
        super(source);
        this.mpAuthentication = mpAuthentication;
    }

    public MidpointAuthentication getMpAuthentication() {
        return mpAuthentication;
    }
}
