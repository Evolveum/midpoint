/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.controller;

import com.evolveum.midpoint.repo.api.SystemConfigurationChangeDispatcher;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeListener;
import com.evolveum.midpoint.schema.result.OperationResult;

import com.google.common.annotations.VisibleForTesting;

/**
 * To be used in those tests where repo module is not available.
 */
@VisibleForTesting
public class DummySystemConfigurationChangeDispatcherImpl implements SystemConfigurationChangeDispatcher {

    @Override
    public void dispatch(boolean ignoreVersion, boolean allowNotFound, OperationResult result) {
    }

    @Override
    public void registerListener(SystemConfigurationChangeListener listener) {
    }

    @Override
    public void unregisterListener(SystemConfigurationChangeListener listener) {
    }
}
