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

package com.evolveum.midpoint.web.security;

import java.util.Date;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;

/**
 * 
 * @author lazyman
 *
 */
public class SessionListener implements HttpSessionListener {

	private static final Trace TRACE = TraceManager.getTrace(SessionListener.class);

	@Override
	public void sessionCreated(HttpSessionEvent se) {
		HttpSession session = se.getSession();

		StringBuilder builder = new StringBuilder();
		builder.append("Session(");
		builder.append(session.getId());
		builder.append(") created in");
		builder.append(new Date(session.getCreationTime()));
		builder.append(", expiration time: ");
		builder.append(session.getMaxInactiveInterval());
		builder.append(" sec.");

		TRACE.warn(builder.toString());
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent se) {
		HttpSession session = se.getSession();

		StringBuilder builder = new StringBuilder();
		builder.append("Session(");
		builder.append(session.getId());
		builder.append(") destroyed, last accessed: ");
		builder.append(new Date(session.getLastAccessedTime()));

		TRACE.warn(builder.toString());
	}
}
