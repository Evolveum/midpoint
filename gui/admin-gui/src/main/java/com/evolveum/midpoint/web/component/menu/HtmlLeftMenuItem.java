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

import com.icesoft.faces.component.ext.HtmlCommandLink;

/**
 * 
 * @author lazyman
 *
 */
@FacesComponent(value = "HtmlLeftMenuItem")
public class HtmlLeftMenuItem extends HtmlCommandLink {

	public static final String ATTR_NAME = "name";
	public static final String ATTR_DESCRIPTION = "description";

	@Override
	public void encodeBegin(FacesContext context) throws IOException {
		ResponseWriter writer = context.getResponseWriter();
		writer.startElement("li", this);

		addActionListener(new LeftMenuActionListener());

		super.encodeBegin(context);
	}

	@Override
	public void encodeEnd(FacesContext context) throws IOException {
		super.encodeEnd(context);

		ResponseWriter writer = context.getResponseWriter();
		writer.endElement("li");
	}

	@Override
	public boolean getRendersChildren() {
		return true;
	}

	@Override
	public void encodeChildren(FacesContext context) throws IOException {
		String name = (String) getAttributes().get(ATTR_NAME);

		ResponseWriter writer = context.getResponseWriter();
		writer.writeText(name, null);

		super.encodeChildren(context);
	}
}
