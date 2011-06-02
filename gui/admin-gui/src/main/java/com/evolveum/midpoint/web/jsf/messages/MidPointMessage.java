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
package com.evolveum.midpoint.web.jsf.messages;

import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;

/**
 * 
 * @author lazyman
 * 
 */
public class MidPointMessage extends FacesMessage {

	private static final long serialVersionUID = 8320638234262514369L;
	private List<String> subMessages;

	public MidPointMessage(Severity severity, String summary, String detail) {
		super(severity, summary, detail);
	}

	public List<String> getSubMessages() {
		if (subMessages == null) {
			subMessages = new ArrayList<String>();
		}
		return subMessages;
	}
}
