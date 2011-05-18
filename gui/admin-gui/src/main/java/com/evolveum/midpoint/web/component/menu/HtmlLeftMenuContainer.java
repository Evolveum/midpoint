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
package com.evolveum.midpoint.web.component.menu;

import java.io.IOException;

import javax.faces.component.FacesComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import com.icesoft.faces.component.ext.HtmlOutputLabel;

/**
 * 
 * @author lazyman
 *
 */
@FacesComponent(value = "HtmlLeftMenuContainer")
public class HtmlLeftMenuContainer extends HtmlOutputLabel {

	public static final String ATTR_NAME = "name";

	@Override
	public void encodeBegin(FacesContext context) throws IOException {
		String name = (String) getAttributes().get(ATTR_NAME);
		ResponseWriter writer = context.getResponseWriter();
		writer.startElement("div", null);
		writer.startElement("h3", this);
		writer.writeText(name, null);
		writer.endElement("h3");
		writer.startElement("ul", null);
	}
	
	@Override
	public void encodeEnd(FacesContext context) throws IOException {
		ResponseWriter writer = context.getResponseWriter();
		writer.endElement("ul");	
		writer.endElement("div");
	}
}
