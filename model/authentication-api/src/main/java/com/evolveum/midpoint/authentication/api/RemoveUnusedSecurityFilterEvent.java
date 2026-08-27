/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.api;

import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * Spring application event carrying the auth modules whose dynamically built security filters
 * should be destroyed and removed. Published by {@link RemoveUnusedSecurityFilterPublisher} and
 * handled by {@code RemoveUnusedSecurityFilterListener}; decouples the authentication flow from
 * the {@code ObjectPostProcessor} that actually holds the filters.
 *
 * @author skublik
 */
public abstract class RemoveUnusedSecurityFilterEvent extends ApplicationEvent {

    protected RemoveUnusedSecurityFilterEvent(Object source) {
        super(source);
    }

    public abstract List<AuthModule<?>> getAuthModules();
}
