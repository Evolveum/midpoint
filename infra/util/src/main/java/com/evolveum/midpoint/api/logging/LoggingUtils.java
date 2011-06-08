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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.api.logging;

import org.apache.commons.lang.Validate;

/**
 * 
 * @author lazyman
 * 
 */
public class LoggingUtils {

	// TODO: object list as parameters to message {}
	public static void logException(Trace logger, String message, Exception ex, Object... objects) {
		Validate.notNull(logger, "Logger can't be null.");
		Validate.notNull(ex, "Exception can't be null.");

		logger.error(message + ", reason: " + ex.getMessage());
		logger.debug(message + ".", ex);
	}
}
