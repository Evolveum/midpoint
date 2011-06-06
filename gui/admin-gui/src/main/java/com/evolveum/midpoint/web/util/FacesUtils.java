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

import javax.faces.application.Application;
import javax.faces.application.FacesMessage;
import javax.faces.application.ProjectStage;
import javax.faces.context.FacesContext;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.result.OperationResultFactory;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.web.jsf.messages.MidPointMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.FaultType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;

/**
 * 
 * @author lazyman
 */
public abstract class FacesUtils {

	private static final Trace TRACE = TraceManager.getTrace(FacesUtils.class);

	public static String getRequestParameter(String name) {
		if (StringUtils.isEmpty(name)) {
			throw new IllegalArgumentException("Attribute name can't be null.");
		}
		return (String) FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap()
				.get(name);
	}

	public static String translateKey(String key, Object[] arguments) {
		if (arguments == null) {
			return translateKey(key);
		}

		MessageFormat format = new MessageFormat(translateKey(key));
		return format.format(arguments);
	}

	public static String translateKey(String key) {
		if (key == null) {
			throw new IllegalArgumentException("Key can't be null");
		}
		Application application = FacesContext.getCurrentInstance().getApplication();

		String translation = null;
		if (ProjectStage.Development.equals(application.getProjectStage())) {
			translation = "!" + key + "!";
		} else {
			translation = key;
		}

		try {
			ResourceBundle bundle = ResourceBundle.getBundle(application.getMessageBundle(), FacesContext
					.getCurrentInstance().getViewRoot().getLocale());

			if (bundle != null) {
				translation = bundle.getString(key);
			} else {
				TRACE.warn("Couldn't find key '" + key + "'.");

			}
		} catch (Exception ex) {
			TRACE.warn("Couldn't get resource bundle, reason: " + ex.getMessage());
		}

		return translation;
	}

	public static void addWarnMessage(String msg) {
		addWarnMessage(msg, null);
	}

	public static void addSuccessMessage(String msg) {
		addSuccessMessage(msg, null);
	}

	public static void addErrorMessage(String msg) {
		addErrorMessage(msg, null);
	}

	public static void addWarnMessage(String msg, Exception ex) {
		addMessage(FacesMessage.SEVERITY_WARN, msg, ex);
	}

	public static void addSuccessMessage(String msg, Exception ex) {
		addMessage(FacesMessage.SEVERITY_INFO, msg, ex);
	}

	public static void addErrorMessage(String msg, Exception ex) {
		addMessage(FacesMessage.SEVERITY_ERROR, msg, ex);
	}

	public static void addWarnMessageWithResult(String msg, OperationResultType result) {
		addMessage(FacesMessage.SEVERITY_WARN, msg, result);
	}

	public static void addSuccessMessageWithResult(String msg, OperationResultType result) {
		addMessage(FacesMessage.SEVERITY_INFO, msg, result);
	}

	public static void addErrorMessageWithResult(String msg, OperationResultType result) {
		addMessage(FacesMessage.SEVERITY_ERROR, msg, result);
	}

	private static void addMessage(FacesMessage.Severity severity, String msg, Exception ex) {
		FacesMessage message = null;
		if (ex == null) {
			message = new FacesMessage(severity, msg, null);
			FacesContext ctx = FacesContext.getCurrentInstance();
			if (null != ctx) {
				ctx.addMessage(null, message);
			}

			return;
		}
		OperationResultType result = OperationResultFactory.createOperationResult("Unknown",
				OperationResultStatusType.FATAL_ERROR, ex.getMessage(), ex.getMessage());
		message = new MidPointMessage(severity, msg, null, null);
	}

	private static void addMessage(FacesMessage.Severity severity, String msg, OperationResultType result) {
		MidPointMessage message = new MidPointMessage(severity, msg, null, result);
		FacesContext ctx = FacesContext.getCurrentInstance();
		if (null != ctx) {
			ctx.addMessage(null, message);
		}
	}

	private static void fillExceptionMessages(MidPointMessage message, Throwable ex) {
		// if (ex == null || message.getSubMessages().size() > 4) {
		// return;
		// }
		// if (ex instanceof WebModelException) {
		// WebModelException webException = (WebModelException) ex;
		// message.getSubMessages().add(webException.getTitle());
		// } else if (ex instanceof FaultMessage) {
		// FaultMessage fault = (FaultMessage) ex;
		// message.getSubMessages().add(getMessage(fault.getMessage(),
		// fault.getFaultInfo()));
		// } else if (ex instanceof
		// com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage)
		// {
		// FaultType fault =
		// ((com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage)
		// ex)
		// .getFaultInfo();
		// message.getSubMessages().add(getMessage(ex.getMessage(), fault));
		// } else {
		// message.getSubMessages().add(ex.getMessage());
		// }

		fillExceptionMessages(message, ex.getCause());
	}

	private static void fillMessageFromOperationResult(MidPointMessage message,
			OperationResultType operationResult) {

	}

	private static String getMessage(String faultMessage, FaultType faultType) {
		StringBuilder builder = new StringBuilder();
		if (!StringUtils.isEmpty(faultMessage)) {
			builder.append(faultMessage);
		}
		if (faultType != null && !StringUtils.isEmpty(faultType.getMessage())) {
			if (builder.length() != 0) {
				builder.append(" Reason: ");
			}
			builder.append(faultType.getMessage());
		}
		return builder.toString();
	}

	@Deprecated
	public static String getMessageFromFault(FaultMessage fault) {
		return getMessageFromFault(fault.getFaultInfo(), fault.getMessage(), fault.getCause());
	}

	@Deprecated
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
