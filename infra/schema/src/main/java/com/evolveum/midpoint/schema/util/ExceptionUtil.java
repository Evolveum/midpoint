/**
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
package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.schema.exception.TunnelException;

/**
 * @author Radovan Semancik
 *
 */
public class ExceptionUtil {
	
	public static Throwable lookForTunneledException(Throwable ex) {
		if (ex instanceof TunnelException) {
			return ex.getCause();
		}
		if (ex.getCause() != null) {
			return lookForTunneledException(ex.getCause());
		}
		return null;
	}

	public static String lookForMessage(Throwable e) {
		if (e.getMessage() != null) {
			return e.getMessage();
		}
		if (e.getCause() != null) {
			return lookForMessage(e.getCause());
		}
		return null;
	}
	
}
