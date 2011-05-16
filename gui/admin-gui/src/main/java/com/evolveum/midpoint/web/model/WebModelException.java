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
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.web.model;

/**
 * Simple exception for the Web GUI.
 * 
 * In GUI there is no need for many exception types. The GUI can only display
 * the error anyway, cannot automatically react. Therefore this exception has
 * properties that can be used to display the error in a human-readable way.
 * 
 * The standard "message" propety should be used as a short error description,
 * e.g. one or two lines of text explaining what had happened - in a
 * human-readable way. No stack traces, java class names and similar Java slang
 * please.
 * 
 * @author semancik
 */
public class WebModelException extends Exception {

	private static final long serialVersionUID = -4734939926502772217L;

	/**
	 * Creates a new instance of <code>WebModelException</code> without detail
	 * message.
	 */
	public WebModelException() {
	}

	/**
	 * Constructs an instance of <code>WebModelException</code> with the
	 * specified detail message.
	 * 
	 * @param msg
	 *            the detail message.
	 */
	public WebModelException(String msg) {
		super(msg);
	}

	public WebModelException(String msg, String title) {
		super(msg);
		this.title = title;
	}

	public WebModelException(String msg, String title, Throwable t) {
		super(msg, t);
		this.title = title;
	}

	protected String title;

	/**
	 * Get the value of error title.
	 * 
	 * This should be few words describing the error type. Should be short to
	 * fit into a window title.
	 * 
	 * @return the value of title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Set the value of title
	 * 
	 * This should be few words describing the error type. Should be short to
	 * fit into a window title.
	 * 
	 * @param title
	 *            new value of title
	 */
	public void setTitle(String title) {
		this.title = title;
	}
}
