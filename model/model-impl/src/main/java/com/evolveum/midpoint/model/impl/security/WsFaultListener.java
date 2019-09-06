/*
 * Copyright (c) 2015-2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.security;

import javax.xml.soap.SOAPMessage;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.cxf.logging.FaultListener;
import org.apache.cxf.message.Message;
import org.apache.wss4j.common.ext.WSSecurityException;

public class WsFaultListener implements FaultListener {

	private static final Trace LOGGER = TraceManager.getTrace(WsFaultListener.class);

	private SecurityHelper securityHelper;

    public WsFaultListener(SecurityHelper securityHelper) {
        super();
        this.securityHelper = securityHelper;
    }

    @Override
	public boolean faultOccurred(Exception exception, String description, Message message) {
    	LOGGER.trace("Handling fault: {}: {} - {}-{}", new Object[]{exception, description, message, exception});
    	Object audited = message.getContextualProperty(SecurityHelper.CONTEXTUAL_PROPERTY_AUDITED_NAME);
    	if (audited != null && ((Boolean)audited)) {
    		return true;
    	}
    	if (exception instanceof PasswordCallbackException) {
    		return true;
    	}
    	if (exception.getCause() instanceof PasswordCallbackException) {
    		return true;
    	}
    	if (exception.getCause() != null && exception.getCause().getCause() instanceof PasswordCallbackException) {
    		return true;
    	}
		try {
			String auditMessage = exception.getMessage();
			if (exception.getClass() != null) {
				// Exception cause has much better message because CXF masks real messages in the SOAP faults.
				auditMessage = exception.getCause().getMessage();
			}
			SOAPMessage saajSoapMessage = message.getContent(SOAPMessage.class);
	    	String username = securityHelper.getUsernameFromMessage(saajSoapMessage);
	    	ConnectionEnvironment connEnv = ConnectionEnvironment.create(SchemaConstants.CHANNEL_WEB_SERVICE_URI);
			securityHelper.auditLoginFailure(username, null, connEnv, auditMessage);
		} catch (WSSecurityException e) {
			// Ignore
			LOGGER.trace("Exception getting username from soap message (probably safe to ignore)", e);
		} catch (Exception e) {
			LOGGER.error("Error auditing SOAP fault: "+e.getMessage(), e);
			// but otherwise ignore it
		}
    	return true;
	}


}
