/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.casemgmt.impl;

import com.evolveum.midpoint.casemgmt.api.CaseEventDispatcher;
import com.evolveum.midpoint.casemgmt.api.CaseEventListener;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author mederly
 */
@Service
public class CaseEventDispatcherImpl implements CaseEventDispatcher {

    private static final Trace LOGGER = TraceManager.getTrace(CaseEventDispatcherImpl.class);

    private final Set<CaseEventListener> listeners = ConcurrentHashMap.newKeySet();

    @Override
    public void registerCaseCreationEventListener(CaseEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void unregisterCaseCreationEventListener(CaseEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void dispatchCaseEvent(CaseType aCase, OperationResult result) {
        for (CaseEventListener listener : listeners) {
            try {
                listener.onCaseCreation(aCase, result);
            } catch (Throwable t) {
                LoggingUtils.logUnexpectedException(LOGGER, "Exception when invoking case listener; case = {}", t, aCase);
            }
        }
    }
}
