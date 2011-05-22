package com.evolveum.midpoint.web.component.syntax;

import java.io.IOException;

import javax.el.ValueExpression;
import javax.faces.application.Resource;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.FacesRenderer;

import org.apache.commons.lang.StringUtils;

import com.sun.faces.renderkit.html_basic.HtmlBasicInputRenderer;

@FacesRenderer(componentFamily = "AceXmlInput", rendererType = "AceXmlInputRenderer")
public class AceXmlInputRenderer extends HtmlBasicInputRenderer {

	@Override
	public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
		super.encodeBegin(context, component);
		String width = (String) getAttributeValue(AceXmlInput.ATTR_WIDTH, context, component);
		if (StringUtils.isEmpty(width)) {
			width = "980";
		}
		String height = (String) getAttributeValue(AceXmlInput.ATTR_HEIGHT, context, component);
		if (StringUtils.isEmpty(height)) {
			height = "350";
		}

		ResponseWriter writer = context.getResponseWriter();
		writer.startElement("div", null);

		StringBuilder style = new StringBuilder();
		style.append("display: block; width: ");
		style.append(width);
		style.append("px; height: ");
		style.append(height);
		style.append("px;");
		writer.writeAttribute("style", style.toString(), null);
		writer.startElement("div", null);
		writer.writeAttribute("id", component.getClientId(), null);
		style = new StringBuilder();
		style.append("position:absolute; width:");
		style.append(width);
		style.append("px; height:");
		style.append(height);
		style.append("px;");
		writer.writeAttribute("style", style.toString(), null);

		ValueExpression valueExpr = component.getValueExpression(AceXmlInput.ATTR_VALUE);
		if (valueExpr != null) {		
			String value = (String) valueExpr.getValue(context.getELContext());
			writer.writeText(value, null);
		}
	}

	@Override
	public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
		ResponseWriter writer = context.getResponseWriter();
		writer.endElement("div");
		writer.endElement("div");

		writeJavaScriptElement(context, writer, "ace.js");
		writeJavaScriptElement(context, writer, "mode-xml.js");
		writeJavaScriptElement(context, writer, "theme-eclipse.js");

		writer.startElement("script", null);
		StringBuilder script = new StringBuilder();
		script.append("window.onload = function() {\n");
		script.append("\tvar editor = ace.edit(\"");
		script.append(component.getClientId());
		script.append("\");\n");
		script.append("\teditor.setTheme(\"ace/theme/eclipse\");\n");
		script.append("\tvar XmlMode = require(\"ace/mode/xml\").Mode;\n");
		script.append("\teditor.getSession().setMode(new XmlMode());\n");
		script.append("\tdocument.getElementById('");
		script.append(component.getClientId());
		script.append("').style.fontSize='13px';\n");

		String editable = (String) getAttributeValue(AceXmlInput.ATTR_EDITABLE, context, component);
		if (StringUtils.isEmpty(editable)) {
			editable = "true";
		}
		boolean readOnly = new Boolean(editable).booleanValue();
		script.append("\teditor.setReadOnly(");
		script.append(readOnly);
		script.append(");\n");
		script.append("};\n");
		writer.writeText(script.toString(), null);
		writer.endElement("script");

		super.encodeEnd(context, component);
	}

	private Object getAttributeValue(String attribute, FacesContext context, UIComponent component) {
		ValueExpression expr = component.getValueExpression(attribute);
		if (expr != null) {
			return expr.getValue(context.getELContext());
		}

		return component.getAttributes().get(attribute);
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
