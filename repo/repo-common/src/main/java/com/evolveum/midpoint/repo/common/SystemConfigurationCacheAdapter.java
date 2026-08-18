/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common;

import java.util.Collection;

import com.evolveum.midpoint.repo.api.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * Adapter from {@link SystemConfigurationChangeDispatcher} to {@link CacheInvalidationListener}.
 * Distributes events about system configuration invalidation changes.
 */
@Component
public class SystemConfigurationCacheAdapter implements CacheInvalidationListener {

    private static final Trace LOGGER = TraceManager.getTrace(SystemConfigurationCacheAdapter.class);

    @Autowired private CacheInvalidationDispatcher cacheInvalidationDispatcher;
    @Autowired private SystemConfigurationChangeDispatcher systemConfigurationChangeDispatcher;

    @PostConstruct
    public void register() {
        cacheInvalidationDispatcher.registerListener(this);
    }

    @PreDestroy
    public void unregister() {
        cacheInvalidationDispatcher.unregisterListener(this);
    }

    @Override
    public Collection<CacheInvalidationEventSpecification> getEventSpecifications() {
        return CacheInvalidationEventSpecification.ALL_AVAILABLE_EVENTS; // TODO narrow the scope
    }

    @Override
    public <O extends ObjectType> void invalidate(Class<O> type, String oid, CacheInvalidationContext context) {
        if (type == null || type.isAssignableFrom(SystemConfigurationType.class)) {
            // We ignore OID by now, assuming there's only a single system configuration object
            try {
                OperationResult result = new OperationResult(SystemConfigurationCacheAdapter.class.getName() + ".invalidate");
                systemConfigurationChangeDispatcher.dispatch(true, true, result);
            } catch (Throwable t) {
                LoggingUtils
                        .logUnexpectedException(LOGGER, "Couldn't dispatch information about updated system configuration", t);
            }
        }
    }
}
