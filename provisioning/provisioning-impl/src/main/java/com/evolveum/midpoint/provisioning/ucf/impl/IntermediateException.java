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
package com.evolveum.midpoint.provisioning.ucf.impl;

/**
 * This exception exists only for the purpose of passing checked exceptions where they are not allowed.
 * This comes handy e.g. in callbacks. This exception must never be shown to "outside".
 * 
 * @author Radovan Semancik
 *
 */
public class IntermediateException extends RuntimeException {

	public IntermediateException() {
		super();
	}

	public IntermediateException(String message, Throwable cause) {
		super(message, cause);
	}

	public IntermediateException(String message) {
		super(message);
	}

	public IntermediateException(Throwable cause) {
		super(cause);
	}

}
