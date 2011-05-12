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

package com.evolveum.midpoint.web.component.syntax;

import com.sun.faces.renderkit.html_basic.TextareaRenderer;
import java.io.IOException;
import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlInputTextarea;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.FacesRenderer;

/**
 *
 * @author Vilo Repan
 */
@FacesRenderer(componentFamily = HtmlInputTextarea.COMPONENT_FAMILY, rendererType = "InputSyntaxTextAreaRenderer")
public class InputSyntaxTextAreaRenderer extends TextareaRenderer {

    private static final String RESOURCE_PATH = "javax.faces.resource/codemirror/";
    private static final String JSF_EXTENSION = ".iface";

    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        appendLinkToScript(RESOURCE_PATH + "js/codemirror.js" + JSF_EXTENSION, writer, component);
//        appendLinkToScript(RESOURCE_PATH + "js/jsfcustom.js" + JSF_EXTENSION, writer, component);

        writer.startElement("link", component);
        writer.writeAttribute("type", "text/css", null);
        writer.writeAttribute("rel", "stylesheet", null);
        writer.writeAttribute("href", RESOURCE_PATH + "css/docs.css" + JSF_EXTENSION, null);
        writer.endElement("link");

        writer.startElement("div", component);
        writer.writeAttribute("class", "border", null);

        super.encodeBegin(context, component);
    }

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        super.encodeEnd(context, component);

        ResponseWriter writer = context.getResponseWriter();
        writer.endElement("div");

        String clientId = component.getClientId(context);
        StringBuilder builder = new StringBuilder();
        builder.append("var editor;\n");
        builder.append("window.setTimeout('createEditor()', 500);\n");
        builder.append("function createEditor() {\n");
        builder.append("editor = CodeMirror.fromTextArea('");
        builder.append(clientId);
        builder.append("', {\n");
        builder.append("height: \"700px\",\n");
        builder.append("width: \"600px\",\n");
        builder.append("parserfile: \"parsexml.js" + JSF_EXTENSION + "\",\n");
        builder.append("stylesheet: \"");
        builder.append(RESOURCE_PATH);
        builder.append("css/xmlcolors.css");
        builder.append(JSF_EXTENSION);
        builder.append("\",\n");
        builder.append("path: \"");
        builder.append(RESOURCE_PATH);
        builder.append("js/\",\n");
        builder.append("continuousScanning: 500,\n");
        builder.append("lineNumbers: true\n");
        builder.append("});\n");
        builder.append("}\n");
        builder.append("function updateTextarea() {\n");
        builder.append("$('");
        builder.append(clientId);
        builder.append("').value='';\n");
        builder.append("$('");
        builder.append(clientId);
        builder.append("').value = editor.getCode();\n");
        builder.append("}");

        writer.startElement("script", component);
        writer.writeAttribute("type", "text/javascript", null);
        writer.writeText(builder.toString(), null);
        writer.endElement("script");
    }

    private void appendLinkToScript(String path, ResponseWriter writer, UIComponent component) throws IOException {
        writer.startElement("script", component);
        writer.writeAttribute("type", "text/javascript", null);
        writer.writeAttribute("src", path, null);
        writer.endElement("script");
    }
}
