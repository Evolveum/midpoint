/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.casemgmt.impl;

import com.evolveum.midpoint.casemgmt.api.CaseEventDispatcher;
import com.evolveum.midpoint.casemgmt.api.CaseCreationListener;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CaseEventDispatcherImpl implements CaseEventDispatcher {

    private static final Trace LOGGER = TraceManager.getTrace(CaseEventDispatcherImpl.class);

    private final Set<CaseCreationListener> listeners = ConcurrentHashMap.newKeySet();

    @Override
    public void registerCaseCreationEventListener(CaseCreationListener listener) {
        listeners.add(listener);
    }

    @Override
    public void unregisterCaseCreationEventListener(CaseCreationListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void dispatchCaseCreationEvent(CaseType aCase, Task task, OperationResult result) {
        for (CaseCreationListener listener : listeners) {
            try {
                listener.onCaseCreation(aCase, task, result);
            } catch (Throwable t) {
                LoggingUtils.logUnexpectedException(LOGGER, "Exception when invoking case listener; case = {}", t, aCase);
            }
        }
    }
}
