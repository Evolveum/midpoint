/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.api;

import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * @author skublik
 */

public abstract class RemoveUnusedSecurityFilterEvent extends ApplicationEvent{
    protected RemoveUnusedSecurityFilterEvent(Object source) {
        super(source);
    }

    public abstract List<AuthModule<?>> getAuthModules();
}
