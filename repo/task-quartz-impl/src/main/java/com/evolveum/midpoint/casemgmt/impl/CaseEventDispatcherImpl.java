/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
