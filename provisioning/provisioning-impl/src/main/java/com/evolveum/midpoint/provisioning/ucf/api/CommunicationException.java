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
package com.evolveum.midpoint.provisioning.ucf.api;

/**
 * Connector communication exception.
 * 
 * May happen in case of various network communication errors, including
 * (but not limited to) connection refused and timeouts.
 * 
 * TODO
 * 
 * @author Radovan Semancik
 *
 */
public class CommunicationException extends Exception {

	/**
	 * Creates a new instance of <code>CommunicationException</code> without detail message.
	 */
	public CommunicationException() {
	}

	/**
	 * Constructs an instance of <code>CommunicationException</code> with the specified detail message.
	 * @param msg the detail message.
	 */
	public CommunicationException(String msg) {
		super(msg);
	}
}
