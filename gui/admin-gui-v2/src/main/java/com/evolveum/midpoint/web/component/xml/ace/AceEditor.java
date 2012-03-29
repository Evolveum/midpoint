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

package com.evolveum.midpoint.web.component.xml.ace;

import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;

public class AceEditor<T> extends TextArea<T> {

    public static final String F_READONLY = "readonly";
    private static final String EDITOR_SUFFIX = "_edit";
    private static final String THEME = "textmate";
    private static final String MODE = "xml";
    private String editorId;
    private String width = "100%";
    private String height = "auto";
    private boolean readonly = false;

    public AceEditor(String id, IModel<T> model) {
        super(id, model);
        this.editorId = getMarkupId() + EDITOR_SUFFIX;
        setOutputMarkupId(true);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        response.renderJavaScriptReference(new PackageResourceReference(AceEditor.class, "ace-script.js"));
        response.renderJavaScriptReference(new PackageResourceReference(AceEditor.class, "mode-xml.js"));
        response.renderJavaScriptReference(new PackageResourceReference(AceEditor.class, "textmate.js"));
        response.renderCSSReference(new PackageResourceReference(AceEditor.class, "style-ace.css"));

        /*IHeaderContributor header = JSLib.getHeaderContribution(VersionDescriptor.alwaysLatest(Library.JQUERY));
        header.renderHead(response);*/
        response.renderOnLoadJavaScript(createOnLoadJavascript());
    }

    private String createOnLoadJavascript() {
        StringBuilder script = new StringBuilder();
        script.append("if ($('#").append(editorId).append("').length == 0) {");
        script.append("$(\"<div id='").append(editorId).append("'></div>\").insertAfter($('#")
        .append(this.getMarkupId()).append("')); ");
        /*script.append("jQuery(\"<div id='").append(editorId).append("' class='aceEditor' style='width: ")
                .append(width).append("; height: ").append(height).append(";'></div>\").insertAfter(jQuery('#")
                .append(this.getMarkupId()).append("')); ");*/
        script.append(" $('#").append(editorId).append("').text($('#").append(this.getMarkupId())
                .append("').val());");
        script.append("var ").append(editorId).append(" = ace.edit(\"").append(editorId).append("\"); ");
        script.append(editorId).append(".setTheme(\"ace/theme/").append(THEME).append("\");");
        script.append("var XmlMode = require(\"ace/mode/").append(MODE).append("\").Mode; ");
        script.append(editorId).append(".getSession().setMode(new XmlMode());");
        script.append("$('#").append(this.getMarkupId()).append("').hide();");
        script.append(editorId).append(".setShowPrintMargin(false); ");
        script.append(editorId).append(".setReadOnly(").append(readonly).append("); ");
        script.append(editorId).append(".on('blur', function() { ");
        script.append("$('#").append(getMarkupId()).append("').val(").append(editorId)
                .append(".getSession().getValue()); ");
        script.append("$('#").append(getMarkupId()).append("').trigger('onBlur'); });");
        
        script.append(" }");
        //System.out.println(script.toString());
        return script.toString();
    }

    public String getWidth() {
        return width;
    }

    public void setWidth(String width) {
        if (width == null || width.isEmpty()) {
            throw new IllegalArgumentException("Width must not be null or empty.");
        }
        this.width = width;
    }

    public String getHeight() {
        return height;
    }

    public void setHeight(String height) {
        if (height == null || height.isEmpty()) {
            throw new IllegalArgumentException("Height must not be null or empty.");
        }
        this.height = height;
    }

    public boolean isReadonly() {
        return readonly;
    }

    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }
}
