/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.subscription;

import com.evolveum.midpoint.repo.api.SystemConfigurationChangeDispatcher;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeListener;

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

/**
 * Computes the {@link SubscriptionState}, caching components needed to compute it.
 * (The caching may change if needed.)
 */
@Component
public class SubscriptionStateCache implements SystemConfigurationChangeListener {

    private static final Trace LOGGER = TraceManager.getTrace(SubscriptionStateCache.class);

    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired private SystemFeaturesEnquirer systemFeaturesEnquirer;
    @Autowired private SystemConfigurationChangeDispatcher systemConfigurationChangeDispatcher;

    private static final String OP_GET_SUBSCRIPTION_STATE = SubscriptionStateCache.class.getName() + ".getSubscriptionState";
    private static final long FEATURES_REFRESH_INTERVAL = 60 * 1000L;

    /** Cached version of the features. */
    private volatile SystemFeatures lastKnownFeatures;

    /** When those were obtained. */
    private volatile long lastKnownFeaturesTimestamp;

    /** Use only if there's no way of obtaining the operation result! */
    public @NotNull SubscriptionState getSubscriptionState() {
        return getSubscriptionState(new OperationResult(OP_GET_SUBSCRIPTION_STATE));
    }

    /** This is the recommended version. */
    public @NotNull SubscriptionState getSubscriptionState(OperationResult result) {
        try {
            return SubscriptionState.determine(
                    getSubscriptionId(systemObjectCache.getSystemConfigurationBean(result)),
                    getSystemFeatures(result));
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't determine the subscription state", e);
            return SubscriptionState.error();
        }
    }

    /** Useful when we know the subscription from the outside. Use only if there's no way of obtaining the operation result. */
    public @NotNull SubscriptionState getSubscriptionState(@NotNull SubscriptionId subscriptionId) {
        return getSubscriptionState(subscriptionId, new OperationResult(OP_GET_SUBSCRIPTION_STATE));
    }

    /** Useful when we know the subscription from the outside. */
    public @NotNull SubscriptionState getSubscriptionState(
            @NotNull SubscriptionId subscriptionId, @NotNull OperationResult result) {
        try {
            return SubscriptionState.determine(
                    subscriptionId,
                    getSystemFeatures(result));
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't determine the subscription state", e);
            return SubscriptionState.error();
        }
    }

    private SystemFeatures getSystemFeatures(OperationResult result) {
        var features = lastKnownFeatures;
        if (features == null || System.currentTimeMillis() - lastKnownFeaturesTimestamp > FEATURES_REFRESH_INTERVAL) {
            features = systemFeaturesEnquirer.getSystemFeatures(result);
            lastKnownFeatures = features;
            lastKnownFeaturesTimestamp = System.currentTimeMillis();
        }
        return features;
    }

    public static @NotNull SubscriptionId getSubscriptionId(@Nullable SystemConfigurationType systemConfiguration) {
        return SubscriptionId.parse(
                SystemConfigurationTypeUtil.getSubscriptionId(systemConfiguration));
    }

    @Override
    public void update(@Nullable SystemConfigurationType value) {
        // Many of the features depend on the system configuration. So let's just invalidate the value if there's a change.
        // The refresh is very cheap.
        lastKnownFeatures = null;
    }

    @PostConstruct
    public void init() {
        systemConfigurationChangeDispatcher.registerListener(this);
    }

    @PreDestroy
    public void shutdown() {
        systemConfigurationChangeDispatcher.unregisterListener(this);
    }
}
