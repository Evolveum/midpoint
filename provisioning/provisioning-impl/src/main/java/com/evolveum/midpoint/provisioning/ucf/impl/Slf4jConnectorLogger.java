/*
 * Copyright (c) 2011 Evolveum
 * 
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 * 
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.provisioning.ucf.impl;

import org.identityconnectors.common.logging.Log.Level;
import org.identityconnectors.common.logging.LogSpi;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;

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
	public void log(Class<?> clazz, String method, Level level, String message,
			Throwable ex) {
		Trace trace = TraceManager.getTrace(clazz);

		if (Level.OK.equals(level)) {
			trace.trace("method: {} \t{}", new Object[]{method, message}, ex);
		} else if (Level.INFO.equals(level)) {
			trace.info("method: {} \t{}", new Object[]{method, message}, ex);
		} else if (Level.WARN.equals(level)) {
			trace.warn("method: {} \t{}", new Object[]{method, message}, ex);
		} else if (Level.ERROR.equals(level)) {
			trace.error("method: {} \t{}", new Object[]{method, message}, ex);
		}

	}

	@Override
	public boolean isLoggable(Class<?> clazz, Level level) {
		return true;
	}

	

}
