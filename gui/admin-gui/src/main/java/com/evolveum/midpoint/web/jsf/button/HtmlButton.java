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
package com.evolveum.midpoint.web.jsf.button;

import java.io.IOException;

import javax.el.ValueExpression;
import javax.faces.component.FacesComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import com.icesoft.faces.component.ext.HtmlCommandLink;
import com.sun.faces.renderkit.RenderKitUtils;

/**
 * 
 * @author lazyman
 * 
 */
@FacesComponent("HtmlButton")
public class HtmlButton extends HtmlCommandLink {

	private static final String ATTR_VALUE = "value";
	private static final String ATTR_IMG = "img";
	private static final String ATTR_BUTTON_TYPE = "buttonType";
	private static final String ATTR_ENABLED = "enabled";
	private static final String BUTTON_TYPE_DEFAULT = "regular";
	private static final String BUTTON_TYPE_DISABLED = "disabled";

	private String value;
	private ValueExpression valueExpr;

	@Override
	public boolean getRendersChildren() {
		return true;
	}

	@Override
	public void encodeBegin(FacesContext context) throws IOException {
		ValueExpression styles = context.getApplication().getExpressionFactory()
				.createValueExpression(getButtonType(context), String.class);
		setValueExpression("styleClass", styles);

		ResponseWriter writer = context.getResponseWriter();
		writer.startElement("div", null);
		writer.writeAttribute("class", "buttons", null);

		// HACK
		value = (String) getAttributes().get(ATTR_VALUE);
		valueExpr = getValueExpression(ATTR_VALUE);
		getAttributes().put(ATTR_VALUE, "");
		setValueExpression(ATTR_VALUE, null);

		if (isEnabled(context)) {
			super.encodeBegin(context);
		} else {
			writer.startElement("a", null);
			writer.writeAttribute("href", "#", null);
			writer.writeAttribute("class", BUTTON_TYPE_DISABLED, null);
		}
	}

	@Override
	public void encodeChildren(FacesContext context) throws IOException {
		ResponseWriter writer = context.getResponseWriter();
		String src = RenderKitUtils.getImageSource(context, this, ATTR_IMG);
		if (src != null && !src.isEmpty()) {
			writer.startElement("img", null);
			writer.writeAttribute("style", "border: 0px none;", null);
			writer.writeAttribute("src", src, null);
			writer.endElement("img");
		}

		String buttonType = getButtonType(context);
		String value = getValue(context);

		writer.startElement("span", null);
		writer.writeAttribute("class", buttonType, null);
		writer.writeText(value, null);
		writer.endElement("span");
	}

	@Override
	public void encodeEnd(FacesContext context) throws IOException {
		ResponseWriter writer = context.getResponseWriter();
		if (isEnabled(context)) {
			super.encodeEnd(context);
		} else {
			writer.endElement("a");
		}
		writer.endElement("div");

		getAttributes().put(ATTR_VALUE, value);
		setValueExpression(ATTR_VALUE, valueExpr);
	}

	private boolean isEnabled(FacesContext context) {
		ValueExpression expr = getValueExpression(ATTR_ENABLED);
		if (expr == null) {
			String value = (String) getAttributes().get(ATTR_ENABLED);
			if (value != null) {
				return Boolean.parseBoolean(value);
			}
			return true;
		}

		Boolean enabled = (Boolean) expr.getValue(context.getELContext());
		if (enabled == null) {
			return true;
		}

		return enabled.booleanValue();
	}

	private String getValue(FacesContext context) {
		if (valueExpr == null) {
			return value;
		}

		return (String) valueExpr.getValue(context.getELContext());
	}

	private String getButtonType(FacesContext context) {
		if (!isEnabled(context)) {
			return "disabled";
		}

		ValueExpression buttonType = getValueExpression(ATTR_BUTTON_TYPE);
		if (buttonType == null) {
			return BUTTON_TYPE_DEFAULT;
		}
		return (String) buttonType.getValue(context.getELContext());
	}
}
