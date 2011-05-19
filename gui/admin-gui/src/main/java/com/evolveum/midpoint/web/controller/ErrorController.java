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
package com.evolveum.midpoint.web.controller;

import java.io.Serializable;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

/**
 * 
 * @author lazyman
 * 
 */
@Controller("error")
@Scope("request")
public class ErrorController implements Serializable {

	private static final long serialVersionUID = -5460046037767377287L;
	private static final String PARAM = "javax.servlet.error.exception";

	public String getTitle() {
		Throwable ex = getThrowable();
		if (ex == null) {
			return "Unknown";
		}

		return ex.getClass().getSimpleName();
	}

	public String getDescription() {
		Throwable ex = getThrowable();
		if (ex == null) {
			return "Unknown";
		}

		return ex.getMessage();
	}

	public String getOtherDescription() {
		Throwable ex = getThrowable();
		if (ex == null || ex.getCause() == null) {
			return "Unknown";
		}

		return ex.getCause().getMessage();
	}

	private Throwable getThrowable() {
		FacesContext context = FacesContext.getCurrentInstance();
		Map<String, Object> requests = context.getExternalContext().getRequestMap();
		return (Throwable) requests.get(PARAM);
	}
}
