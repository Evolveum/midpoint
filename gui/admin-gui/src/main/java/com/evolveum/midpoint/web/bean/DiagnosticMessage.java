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

package com.evolveum.midpoint.web.bean;

import java.io.Serializable;
import java.util.Date;

import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.DiagnosticsMessageType;

/**
 * 
 * @author Katuska
 */
public class DiagnosticMessage implements Serializable {

	private static final long serialVersionUID = 7568834241228899775L;
	private String message;
	private String details;
	private Date timestamp;

	public DiagnosticMessage(DiagnosticsMessageType message) {
		if (message == null) {
			return;
		}

		setMessage(message.getMessage());
		setDetails(message.getDetails());
		if (message.getTimestamp() != null) {
			setTimestamp(message.getTimestamp().toGregorianCalendar().getTime());
		}
	}

	public String getDetails() {
		return details;
	}

	public void setDetails(String details) {
		this.details = details;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getTimestampString() {
		return FacesUtils.formatDate(getTimestamp());
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
}
