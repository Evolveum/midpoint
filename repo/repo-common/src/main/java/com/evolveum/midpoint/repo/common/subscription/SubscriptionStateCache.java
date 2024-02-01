/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.subscription;

import com.evolveum.midpoint.repo.api.SystemConfigurationChangeDispatcher;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeListener;

import com.evolveum.midpoint.task.api.TaskManager;

import com.evolveum.midpoint.task.api.TaskManager.ClusteringAvailabilityProvider;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SystemConfigurationTypeUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

import java.util.Objects;

/**
 * Computes the {@link SubscriptionState}, caching components needed to compute it.
 * (The caching may change if needed.)
 */
@Component
public class SubscriptionStateCache implements SystemConfigurationChangeListener, ClusteringAvailabilityProvider {

    private static final Trace LOGGER = TraceManager.getTrace(SubscriptionStateCache.class);

    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired private SystemFeaturesEnquirer systemFeaturesEnquirer;
    @Autowired private SystemConfigurationChangeDispatcher systemConfigurationChangeDispatcher;
    @Autowired private TaskManager taskManager;

    private static final String OP_GET_SUBSCRIPTION_STATE = SubscriptionStateCache.class.getName() + ".getSubscriptionState";
    private static final long STATE_REFRESH_INTERVAL = 10 * 1000L;

    /** Cached version of the features. */
    private volatile SubscriptionState cachedState;

    /** When those were obtained. */
    private volatile long cachedStateTimestamp;

    /** Use only if there's no way of obtaining the operation result! */
    public @NotNull SubscriptionState getSubscriptionState() {
        return Objects.requireNonNullElseGet(
                getCachedState(),
                () -> determineAndCacheSubscriptionState(new OperationResult(OP_GET_SUBSCRIPTION_STATE)));
    }

    /** This is the recommended version. */
    public @NotNull SubscriptionState getSubscriptionState(OperationResult result) {
        return Objects.requireNonNullElseGet(
                getCachedState(),
                () -> determineAndCacheSubscriptionState(result));
    }

    private SubscriptionState getCachedState() {
        if (System.currentTimeMillis() - cachedStateTimestamp <= STATE_REFRESH_INTERVAL) {
            return cachedState;
        } else {
            return null;
        }
    }

    private @NotNull SubscriptionState determineAndCacheSubscriptionState(OperationResult result) {
        try {
            var systemConfiguration = systemObjectCache.getSystemConfigurationBean(result);
            var subscriptionId = SubscriptionId.parse(
                    SystemConfigurationTypeUtil.getSubscriptionId(systemConfiguration));
            var systemFeatures = systemFeaturesEnquirer.getSystemFeatures(result);
            var currentSubscriptionState = SubscriptionState.determine(subscriptionId, systemFeatures);

            cachedStateTimestamp = System.currentTimeMillis();
            cachedState = currentSubscriptionState;
            return currentSubscriptionState;
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't determine the subscription state", e);
            // Not caching anything!
            return SubscriptionState.error();
        }
    }

    @Override
    public boolean isClusteringAvailable() {
        return getSubscriptionState().isClusteringAvailable();
    }

    @Override
    public void update(@Nullable SystemConfigurationType value) {
        // Many of the features depend on the system configuration. So let's just invalidate the value if there's a change.
        // The refresh is very cheap.
        cachedState = null;
    }

    @PostConstruct
    public void init() {
        systemConfigurationChangeDispatcher.registerListener(this);
        taskManager.registerClusteringAvailabilityProvider(this);
    }

    @PreDestroy
    public void shutdown() {
        systemConfigurationChangeDispatcher.unregisterListener(this);
        taskManager.unregisterClusteringAvailabilityProvider(this);
    }
}
