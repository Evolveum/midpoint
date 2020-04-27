/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.repo.api.Cacheable;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeDispatcher;
import com.evolveum.midpoint.repo.cache.CacheRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SingleCacheStateInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Collection;
import java.util.Collections;

/**
 * @author mederly
 */
@Component
public class SystemConfigurationCacheableAdapter implements Cacheable {

    private static final Trace LOGGER = TraceManager.getTrace(SystemConfigurationCacheableAdapter.class);

    @Autowired private CacheRegistry cacheRegistry;
    @Autowired private SystemConfigurationChangeDispatcher systemConfigurationChangeDispatcher;

    @PostConstruct
    public void register() {
        cacheRegistry.registerCacheableService(this);
    }

    @PreDestroy
    public void unregister() {
        cacheRegistry.unregisterCacheableService(this);
    }

    @Override
    public void invalidate(Class<?> type, String oid, CacheInvalidationContext context) {
        if (type == null || type.isAssignableFrom(SystemConfigurationType.class)) {
            // We ignore OID by now, assuming there's only a single system configuration object
            try {
                OperationResult result = new OperationResult(SystemConfigurationCacheableAdapter.class.getName() + ".invalidate");
                systemConfigurationChangeDispatcher.dispatch(true, true, result);
            } catch (Throwable t) {
                LoggingUtils
                        .logUnexpectedException(LOGGER, "Couldn't dispatch information about updated system configuration", t);
            }
        }
    }

    @NotNull
    @Override
    public Collection<SingleCacheStateInformationType> getStateInformation() {
        return Collections.emptySet();
    }

    @Override
    public void dumpContent() {
        // nothing to do here
    }
}
