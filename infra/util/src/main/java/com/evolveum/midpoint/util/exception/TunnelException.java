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
package com.evolveum.midpoint.util.exception;

/**
 * Exception used for tunneling checked exceptions through places where checked exceptipons are not allowed (e.g. callbacks).
 * 
 * This exception must not appear to the "outside", it must be caught and transformed back to the original form.
 * 
 * @author Radovan Semancik
 *
 */
public class TunnelException extends RuntimeException {
	private static final long serialVersionUID = -3745473492409029661L;

	public TunnelException() {
		super();
	}

	public TunnelException(String message, Throwable cause) {
		super(message, cause);
	}

	public TunnelException(String message) {
		super(message);
	}

	public TunnelException(Throwable cause) {
		super(cause);
	}
	
}
