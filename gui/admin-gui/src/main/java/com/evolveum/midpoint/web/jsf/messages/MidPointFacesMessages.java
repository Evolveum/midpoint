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

import java.io.IOException;
import java.util.Iterator;

import javax.faces.application.FacesMessage;
import javax.faces.component.FacesComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import com.icesoft.faces.component.ext.HtmlMessages;

/**
 * 
 * @author lazyman
 * 
 */
@FacesComponent("MidPointFacesMessages")
public class MidPointFacesMessages extends HtmlMessages {

	@Override
	public void encodeBegin(FacesContext context) throws IOException {
		ResponseWriter writer = context.getResponseWriter();

		Iterator<FacesMessage> iterator = context.getMessages();
		while (iterator.hasNext()) {
			FacesMessage message = iterator.next();
			writer.startElement("p", null);
			writer.writeText(message.getSummary(), null);
			writer.endElement("p");
		}
		super.encodeBegin(context);
		System.out.println("encodeBegin");
	}
}
