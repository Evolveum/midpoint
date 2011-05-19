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

package com.evolveum.midpoint.web.component.button;

import com.sun.faces.renderkit.RenderKitUtils;
import com.sun.faces.renderkit.html_basic.CommandLinkRenderer;
import java.io.IOException;
import javax.el.ValueExpression;
import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlCommandLink;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.FacesRenderer;

/**
 * 
 * @author Vilo Repan
 */
@FacesRenderer(componentFamily = HtmlCommandLink.COMPONENT_FAMILY, rendererType = "HtmlButtonRenderer")
public class HtmlButtonRenderer extends CommandLinkRenderer {

	private String styleClasses;
	private Object value;
	private ValueExpression valueExpression;

	@Override
	public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
		HtmlButton link = (HtmlButton) component;
		value = link.getValue();
		styleClasses = link.getStyleClass();
		valueExpression = link.getValueExpression("value");

		link.setValue(null);
		link.setValueExpression("value", null);
		link.setStyleClass(styleClasses + " " + link.getButtonType());

		ResponseWriter writer = context.getResponseWriter();
		writer.startElement("div", component);
		writer.writeAttribute("class", "buttons", null);

		super.encodeBegin(context, component);
	}

	@Override
	public void encodeChildren(FacesContext context, UIComponent component) throws IOException {
		HtmlButton link = (HtmlButton) component;

		ResponseWriter writer = context.getResponseWriter();
		String src = RenderKitUtils.getImageSource(context, component, "img");
		if (src != null && !src.isEmpty()) {
			writer.startElement("img", component);
			writer.writeAttribute("style", "border: 0px none;", null);
			writer.writeAttribute("src", src, null);
			writer.endElement("img");
		}

		super.encodeChildren(context, component);

		writer.startElement("span", component);
		writer.writeAttribute("class", link.getButtonType(), null);

		if (valueExpression != null) {
			writer.writeText((String) valueExpression.getValue(context.getELContext()), null);
		} else {
			writer.writeText(value, null);
		}
		writer.endElement("span");
	}

	@Override
	public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
		super.encodeEnd(context, component);

		ResponseWriter writer = context.getResponseWriter();
		writer.endElement("div");

		HtmlButton link = (HtmlButton) component;
		link.setValue(value);
		link.setStyleClass(styleClasses);
		link.setValueExpression("value", valueExpression);
	}
}
