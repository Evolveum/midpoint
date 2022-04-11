/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.api;

import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;

import org.springframework.context.ApplicationEvent;

/**
 * @author skublik
 */

public abstract class RemoveUnusedSecurityFilterEvent extends ApplicationEvent{
    protected RemoveUnusedSecurityFilterEvent(Object source) {
        super(source);
    }

    public abstract MidpointAuthentication getMpAuthentication();
}
