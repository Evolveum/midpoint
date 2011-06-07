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
 * "Portions Copyrighted 2011 [name of copyright owner]"
 * 
 */
package com.evolveum.midpoint.provisioning.api;

/**
 * Error regarding schema.
 * 
 * E.g. Object class violation, missing object class, inconsistent schema, etc. 
 * 
 * @author Radovan Semancik
 *
 */
public class SchemaException extends Exception {
	private static final long serialVersionUID = -6016220825724355014L;

	public SchemaException() {
		super();
		// TODO Auto-generated constructor stub
	}

	public SchemaException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	public SchemaException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	public SchemaException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

}
