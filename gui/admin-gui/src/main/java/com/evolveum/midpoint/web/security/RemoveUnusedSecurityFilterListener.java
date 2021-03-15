/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.model.api.authentication.AuthModule;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.stereotype.Component;

/**
 * @author skublik
 */

@Component
public class RemoveUnusedSecurityFilterListener  implements ApplicationListener<RemoveUnusedSecurityFilterEvent> {

    private static final Trace LOGGER = TraceManager.getTrace(RemoveUnusedSecurityFilterListener.class);

    @Autowired private ObjectPostProcessor<Object> objectObjectPostProcessor;

    @Override
    public void onApplicationEvent(RemoveUnusedSecurityFilterEvent event) {
        LOGGER.trace("Received spring RemoveUnusedSecurityFilterEvent event - " + event.getMpAuthentication());

        if (event.getMpAuthentication() != null && CollectionUtils.isNotEmpty(event.getMpAuthentication().getAuthModules())
                && objectObjectPostProcessor instanceof MidpointAutowireBeanFactoryObjectPostProcessor) {
            for (AuthModule module : event.getMpAuthentication().getAuthModules()) {
                if (module.getSecurityFilterChain() != null
                        && CollectionUtils.isNotEmpty(module.getSecurityFilterChain().getFilters())) {
                    ((MidpointAutowireBeanFactoryObjectPostProcessor)objectObjectPostProcessor).destroyAndRemoveFilters(module.getSecurityFilterChain().getFilters());
                }
            }
        }

    }
}
