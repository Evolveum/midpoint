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

package com.evolveum.midpoint.web.util;

import java.text.MessageFormat;
import java.util.ResourceBundle;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.FaultType;

/**
 * 
 * @author lazyman
 */
public abstract class FacesUtils {

	private static final Trace TRACE = TraceManager.getTrace(FacesUtils.class);

	public static String getBundleKey(String bundleName, String key, Object[] arguments) {
		if (arguments == null) {
			return getBundleKey(bundleName, key);
		}
		
		MessageFormat format = new MessageFormat(getBundleKey(bundleName, key));
		return format.format(arguments);
	}
	
	public static String getBundleKey(String bundleName, String key) {
		ResourceBundle bundle = FacesContext.getCurrentInstance().getApplication()
				.getResourceBundle(FacesContext.getCurrentInstance(), bundleName);

		if (bundle == null) {
			TRACE.warn("Couldn't get resource bundle '" + bundleName + "' and look for key '" + key + "'.");
			return "!" + key + "!";
		}		

		return bundle.getString(key);
	}

	public static void addSuccessMessage(String msg) {
		addMessage(FacesMessage.SEVERITY_INFO, msg);
	}

	public static void addErrorMessage(String msg) {
		addMessage(FacesMessage.SEVERITY_ERROR, msg);
	}

	private static void addMessage(FacesMessage.Severity severity, String msg) {
		final FacesMessage facesMsg = new FacesMessage(severity, msg, msg);
		FacesContext ctx = FacesContext.getCurrentInstance();
		if (null != ctx) {
			ctx.addMessage(null, facesMsg);
		}
	}

	public static String getRequestParameter(String name) {
		return (String) FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap()
				.get(name);
	}

	public static String getMessageFromFault(
			com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage fault) {
		return getMessageFromFault(fault.getFaultInfo(), fault.getMessage(), fault.getCause());
	}

	public static String getMessageFromFault(
			com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage fault) {
		return getMessageFromFault(fault.getFaultInfo(), fault.getMessage(), fault.getCause());
	}

	private static String getMessageFromFault(FaultType faultType, String faultMessage, Throwable ex) {
		if (!StringUtils.isEmpty(faultMessage)) {
			return faultMessage;
		}

		StringBuilder builder = new StringBuilder();
		builder.append(faultMessage);

		boolean messageAdded = false;
		if (faultType != null && !StringUtils.isEmpty(faultType.getMessage())) {
			builder.append(": ");
			builder.append(faultType.getMessage());
			messageAdded = true;
		}

		if (!messageAdded && ex != null) {
			builder.append(": ");
			builder.append(ex.getMessage());
		}

		return builder.toString();
	}
}
