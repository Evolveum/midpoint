/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import org.identityconnectors.common.logging.Log.Level;
import org.identityconnectors.common.logging.LogSpi;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Logger for ICF Connectors.
 * 
 * The ICF connectors will call this class to log messages. It is configured in
 * META-INF/services/org.identityconnectors.common.logging
 * 
 * @author Katka Valalikova
 *
 */
public class Slf4jConnectorLogger implements LogSpi {

	@Override
	public void log(Class<?> clazz, String method, Level level, String message, Throwable ex) {
		Trace LOGGER = TraceManager.getTrace(clazz);
		//Mark all messages from ICF as ICF
		Marker m = MarkerFactory.getMarker("ICF");
		
		//Translate ICF logging into slf4j
		// OK    -> trace
		// INFO  -> debug
		// WARN  -> warn
		// ERROR -> error
		if (Level.OK.equals(level)) {
			if (null == ex) {
				LOGGER.trace(m, "method: {} msg:{}", method, message);
			} else {
				LOGGER.trace(m, "method: {} msg:{}", new Object[] { method, message }, ex);
			}
		} else if (Level.INFO.equals(level)) {
			if (null == ex) {
				LOGGER.debug(m, "method: {} msg:{}", method, message);
			} else {
				LOGGER.debug(m, "method: {} msg:{}", new Object[] { method, message }, ex);
			}
		} else if (Level.WARN.equals(level)) {
			if (null == ex) {
				LOGGER.warn(m, "method: {} msg:{}", method, message);
			} else {
				LOGGER.warn(m, "method: {} msg:{}", new Object[] { method, message }, ex);
			}
		} else if (Level.ERROR.equals(level)) {
			if (null == ex) {
				LOGGER.error(m, "method: {} msg:{}", method, message);
			} else {
				LOGGER.error(m, "method: {} msg:{}", new Object[] { method, message }, ex);
			}
		}
	}

    //@Override
    // not using override to be able to work with both "old" and current version of connid
    @Override
    public void log(Class<?> clazz, StackTraceElement caller, Level level, String message, Throwable ex) {
        log(clazz, caller.getMethodName(), level, message, ex);
    }

    @Override
	public boolean isLoggable(Class<?> clazz, Level level) {
		return true;
	}

    //@Override
    // not using override to be able to work with both "old" and current version of connid
    @Override
    public boolean needToInferCaller(Class<?> clazz, Level level) {
        return false;
    }

}
