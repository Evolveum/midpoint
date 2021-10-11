/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.common.LoggingConfigurationManager;
import com.evolveum.midpoint.common.ProfilingConfigurationManager;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeDispatcher;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeListener;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringNormalizerConfigurationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * @author mederly
 */
@Component
public class SystemConfigurationChangeDispatcherImpl implements SystemConfigurationChangeDispatcher {

    private static final Trace LOGGER = TraceManager.getTrace(SystemConfigurationChangeDispatcherImpl.class);

    @Autowired private RepositoryService repositoryService;
    @Autowired private PrismContext prismContext;
    @Autowired private RelationRegistry relationRegistry;
    @Autowired private MidpointConfiguration midpointConfiguration;
    @Autowired private CacheConfigurationManager cacheConfigurationManager;

    private final Collection<SystemConfigurationChangeListener> listeners = new HashSet<>();

    private String lastVersionApplied = null;

    public synchronized void dispatch(boolean ignoreVersion, boolean allowNotFound,
            OperationResult result) throws SchemaException {
        LOGGER.trace("Applying system configuration: lastVersionApplied = {}, ignoreVersion = {}", lastVersionApplied,
                ignoreVersion);

        Collection<SelectorOptions<GetOperationOptions>> options = GetOperationOptions.createReadOnlyCollection();
        PrismObject<SystemConfigurationType> configurationObject;
        try {
            configurationObject = repositoryService
                    .getObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(),
                            options, result);
        } catch (ObjectNotFoundException e) {
            if (allowNotFound) {
                LOGGER.debug("System configuration not found");
                result.muteLastSubresultError();
            } else {
                LOGGER.warn("System configuration not found", e);
            }
            notifyListeners(null);
            lastVersionApplied = null;
            return;
        }

        String currentVersion = configurationObject.getVersion();
        if (!ignoreVersion && lastVersionApplied != null && lastVersionApplied.equals(currentVersion)) {
            LOGGER.trace("Last version applied ({}) is the same as the current version ({}), skipping application",
                    lastVersionApplied, currentVersion);
            return;
        }

        SystemConfigurationType configuration = configurationObject.asObjectable();

        // This value is reset to null on exception in applyXXX method. This causes repeated application of sysconfig,
        // because this method is called also from the cluster management thread.
        lastVersionApplied = currentVersion;

        notifyListeners(configuration);
        applyLoggingConfiguration(configurationObject, result);
        applyRemoteHostAddressHeadersConfiguration(configuration);
        applyPolyStringNormalizerConfiguration(configuration);
        applyFullTextSearchConfiguration(configuration);
        applyRelationsConfiguration(configuration);
        applyOperationResultHandlingConfiguration(configuration);
        applyCachingConfiguration(configuration);
        applyRepositoryConfiguration(configuration);

        if (lastVersionApplied != null) {
            LOGGER.trace("System configuration version {} applied successfully", lastVersionApplied);
        } else {
            LOGGER.warn("There was a problem during application of the system configuration");
        }
    }

    private void notifyListeners(SystemConfigurationType configuration) {
        for (SystemConfigurationChangeListener listener : listeners) {
            try {
                listener.update(configuration);
            } catch (Throwable t) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't update system configuration listener {}", t, listener);
                lastVersionApplied = null;
            }
        }
    }

    private void applyLoggingConfiguration(PrismObject<SystemConfigurationType> configuration, OperationResult result) {
        try {
            LoggingConfigurationType loggingWithProfiling = ProfilingConfigurationManager
                    .checkSystemProfilingConfiguration(configuration);
            if (loggingWithProfiling != null) {
                LoggingConfigurationManager.configure(loggingWithProfiling, configuration.getVersion(), midpointConfiguration, result);
            }
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't apply logging configuration", t);
            lastVersionApplied = null;
        }
    }

    private void applyRemoteHostAddressHeadersConfiguration(SystemConfigurationType configurationBean) {
        try {
            SecurityUtil.setRemoteHostAddressHeaders(configurationBean);
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't apply configuration of remote host address headers", t);
            lastVersionApplied = null;
        }
    }

    private void applyPolyStringNormalizerConfiguration(SystemConfigurationType configType) {
        try {
            PolyStringNormalizerConfigurationType normalizerConfig = null;
            InternalsConfigurationType internals = configType.getInternals();
            if (internals != null) {
                normalizerConfig = internals.getPolyStringNormalizer();
            }
            prismContext.configurePolyStringNormalizer(normalizerConfig);
            LOGGER.trace("Applied PolyString normalizer configuration {}", DebugUtil.shortDumpLazily(normalizerConfig));
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't apply PolyString normalizer configuration", t);
            lastVersionApplied = null;
        }
    }

    private void applyFullTextSearchConfiguration(SystemConfigurationType configuration) {
        try {
            repositoryService.applyFullTextSearchConfiguration(configuration.getFullTextSearch());
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't apply fulltext search configuration", t);
            lastVersionApplied = null;
        }
    }

    private void applyRelationsConfiguration(SystemConfigurationType configuration) {
        try {
            relationRegistry.applyRelationsConfiguration(configuration);
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't apply relations configuration", t);
            lastVersionApplied = null;
        }
    }

    private void applyOperationResultHandlingConfiguration(SystemConfigurationType configuration) {
        try {
            if (configuration != null && configuration.getInternals() != null) {
                OperationResult.applyOperationResultHandlingStrategy(
                        configuration.getInternals().getOperationResultHandlingStrategy(),
                        configuration.getInternals().getSubresultStripThreshold());
            } else {
                OperationResult.applyOperationResultHandlingStrategy(Collections.emptyList(), null);
            }
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't apply operation result handling configuration", t);
            lastVersionApplied = null;
        }
    }

    private void applyCachingConfiguration(SystemConfigurationType configuration) {
        try {
            cacheConfigurationManager.applyCachingConfiguration(configuration);
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't apply caching configuration", t);
            lastVersionApplied = null;
        }
    }

    private void applyRepositoryConfiguration(SystemConfigurationType configuration) {
        try {
            RepositoryStatisticsReportingConfigurationType statistics =
                    configuration.getInternals() != null && configuration.getInternals().getRepository() != null ?
                    configuration.getInternals().getRepository().getStatistics() : null;
            repositoryService.getPerformanceMonitor().setConfiguration(statistics);
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't apply repository configuration", t);
            lastVersionApplied = null;
        }
    }

    @Override
    public synchronized void registerListener(SystemConfigurationChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        } else {
            LOGGER.warn("Attempt to register already-registered listener: {}", listener);
        }
    }

    @Override
    public synchronized void unregisterListener(SystemConfigurationChangeListener listener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
        } else {
            LOGGER.warn("Attempt to unregister a listener that was not registered: {}", listener);
        }
    }
}
