package com.evolveum.midpoint.web.jsf.syntax;

import java.io.IOException;

import javax.el.ValueExpression;
import javax.faces.application.Resource;
import javax.faces.component.FacesComponent;
import javax.faces.component.html.HtmlInputHidden;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.apache.commons.lang.StringUtils;

@FacesComponent("AceXmlInput")
public class AceXmlInput extends HtmlInputHidden {

	public static final String ATTR_VALUE = "value";
	public static final String ATTR_WIDTH = "width";
	public static final String ATTR_HEIGHT = "height";
	public static final String ATTR_READONLY = "readonly";

	@Override
	public void encodeEnd(FacesContext context) throws IOException {
		super.encodeEnd(context);

		String width = (String) getAttributeValue(AceXmlInput.ATTR_WIDTH, context);
		if (StringUtils.isEmpty(width) || !width.matches("[0-9]+")) {
			width = "978";
		}
		String height = (String) getAttributeValue(AceXmlInput.ATTR_HEIGHT, context);
		if (StringUtils.isEmpty(height) || !height.matches("[0-9]+")) {
			height = "350";
		}

		ResponseWriter writer = context.getResponseWriter();
		writer.startElement("div", null);

		StringBuilder style = new StringBuilder();
		style.append("display: block; width: ");
		style.append(width);
		style.append("px; height: ");
		style.append(height);
		style.append("px; border: 1px solid #000;");
		writer.writeAttribute("style", style.toString(), null);
		writer.startElement("div", null);
		writer.writeAttribute("id", getClientId() + "Real", null);
		style = new StringBuilder();
		style.append("position:absolute; width:");
		style.append(width);
		style.append("px; height:");
		style.append(height);
		style.append("px;");
		writer.writeAttribute("style", style.toString(), null);

		ValueExpression valueExpr = getValueExpression(AceXmlInput.ATTR_VALUE);
		if (valueExpr != null) {
			String value = (String) valueExpr.getValue(context.getELContext());
			if (value == null) {
				value = "";
			}
			writer.writeText(value, null);
		}
		writer.endElement("div");
		writer.endElement("div");

		writeJavaScriptElement(context, writer, "ace.js");
		writeJavaScriptElement(context, writer, "mode-xml.js");
		writeJavaScriptElement(context, writer, "theme-eclipse.js");
		writeJavaScriptElement(context, writer, "ace-component.js");

		Object object = getAttributeValue(AceXmlInput.ATTR_READONLY, context);
		Boolean readonly = false;
		if (object instanceof String) {
			readonly = Boolean.valueOf((String) object);
		} else if (object instanceof Boolean) {
			readonly = (Boolean) object;
		}

		writer.startElement("script", null);
		writer.writeAttribute("type", "text/javascript", null);
		StringBuilder script = new StringBuilder();
		script.append("if (!isDefined('editors')) {\n");
		script.append("\twindow.editors = {};\n");
		script.append("}\n");
		script.append("window.editors['");
		script.append(getClientId());
		script.append("'] = ");
		script.append(readonly.booleanValue());
		script.append(";\n");
		
//		writer.writeText(script.toString(), null);
//		writer.endElement("script");
//
//		writer.startElement("script", null);
//		writer.writeAttribute("type", "text/javascript", null);
//		
//		script = new StringBuilder();
		
		script.append("ice.onLoad(function() {\n");
		// script.append("\talert(\"After load: editable: \" + !" +
		// readonly.booleanValue() + ");\n");
		script.append("\tloadEditor(\"" + getClientId() + "\");\n");
		script.append("\tice.onAfterUpdate(function(updates) {\n");
		// script.append("\talert(\"After update: editable: \" + !" +
		// readonly.booleanValue() + ");");
		script.append("\t\tloadEditor(\"" + getClientId() + "\");\n");
		script.append("\t});\n");
		script.append("});\n");

		writer.writeText(script.toString(), null);
		writer.endElement("script");
	}

	private Object getAttributeValue(String attribute, FacesContext context) {
		ValueExpression expr = getValueExpression(attribute);
		if (expr != null) {
			return expr.getValue(context.getELContext());
		}

		return getAttributes().get(attribute);
	}

	private void writeJavaScriptElement(FacesContext context, ResponseWriter writer, String resource)
			throws IOException {
		Resource res = context.getApplication().getResourceHandler().createResource(resource, "js/ace");
		writer.startElement("script", null);
		writer.writeAttribute("type", "text/javascript", null);
		writer.writeAttribute("src", res.getRequestPath(), null);
		writer.endElement("script");
	}
}
