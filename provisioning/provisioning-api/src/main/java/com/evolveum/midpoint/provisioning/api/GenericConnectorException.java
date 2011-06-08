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
package com.evolveum.midpoint.provisioning.api;

/**
 * Generic indistinguishable error of a connector framework.
 * 
 * Please do not use this exception if possible!
 * Only errors that cannot be categorized or that are not
 * expected at all should use this exception.
 * 
 * This is RUNTIME exception. As this error is generic and
 * we cannot distinguish any details, there is no hope that
 * an interface client can do anything with it. So don't even
 * bother catching it.
 * 
 * @author Radovan Semancik
 */
public class GenericConnectorException extends RuntimeException {
	private static final long serialVersionUID = 4718501022689239025L;

	public GenericConnectorException() {
	}

	public GenericConnectorException(String message) {
		super(message);
	}

	public GenericConnectorException(Throwable cause) {
		super(cause);
	}

	public GenericConnectorException(String message, Throwable cause) {
		super(message, cause);
	}

}
