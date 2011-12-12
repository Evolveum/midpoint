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

import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.wicketstuff.jslibraries.JSLib;
import org.wicketstuff.jslibraries.Library;
import org.wicketstuff.jslibraries.VersionDescriptor;

public class AceEditor<T> extends TextArea<T> {

    public static final String F_READONLY = "readonly";
    private static final String EDITOR_SUFFIX = "_edit";
    private static final String THEME = "eclipse";
    private static final String MODE = "xml";
    private String editorId;
    private String width = "1000px";
    private String height = "350px";
    private boolean readonly = false;

    public AceEditor(String id, IModel<T> model) {
        super(id, model);
        this.editorId = getMarkupId() + EDITOR_SUFFIX;
        setOutputMarkupId(true);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        response.renderCSSReference(new PackageResourceReference(AceEditor.class, "style-ace.css"));
        response.renderJavaScriptReference(new PackageResourceReference(AceEditor.class, "ace.js"));
        response.renderJavaScriptReference(new PackageResourceReference(AceEditor.class, "mode-xml.js"));
        response.renderJavaScriptReference(new PackageResourceReference(AceEditor.class, "theme-eclipse.js"));

        IHeaderContributor header = JSLib.getHeaderContribution(VersionDescriptor.alwaysLatest(Library.JQUERY));
        header.renderHead(response);
        response.renderOnLoadJavaScript(createOnLoadJavascript());
    }

    /**
     * if (jQuery('#aceEditor3_edit').length == 0) {
     * jQuery("<div id='aceEditor3_edit' class='aceEditor' style='width: 90%; height: 250px;'></div>")
     * .insertAfter(jQuery('#aceEditor3'));
     * jQuery('#aceEditor3_edit').text(jQuery('#aceEditor3').val());
     * aceEditor3_edit = ace.edit('aceEditor3_edit');
     * aceEditor3_edit.setTheme('ace/theme/eclipse');
     * var Mode = require('ace/mode/xml').Mode;
     * aceEditor3_edit.getSession().setMode(new Mode());
     * jQuery('#aceEditor3').hide();
     * aceEditor3_edit.setShowPrintMargin(false);
     * aceEditor3_edit.setReadOnly(false);
     * aceEditor3_edit.on('blur', function() {
     * jQuery('#aceEditor3').val(aceEditor3_edit.getSession().getValue());
     * jQuery('#aceEditor3').trigger('onBlur');
     * });
     * }
     */
    private String createOnLoadJavascript() {
        StringBuilder script = new StringBuilder();
        script.append("if (jQuery('#").append(editorId).append("').length == 0) {");
        script.append("jQuery(\"<div id='").append(editorId).append("' class='aceEditor' style='width: ")
                .append(width).append("; height: ").append(height).append(";'></div>\").insertAfter(jQuery('#")
                .append(this.getMarkupId()).append("')); ");
        script.append(" jQuery('#").append(editorId).append("').text(jQuery('#").append(this.getMarkupId())
                .append("').val());");
        script.append(editorId).append(" = ace.edit('").append(editorId).append("'); ");
        script.append(editorId).append(".setTheme('ace/theme/").append(THEME).append("');");
        script.append("var Mode = require('ace/mode/").append(MODE).append("').Mode; ");
        script.append(editorId).append(".getSession().setMode(new Mode());");
        script.append("jQuery('#").append(this.getMarkupId()).append("').hide();");
        script.append(editorId).append(".setShowPrintMargin(false); ");
        script.append(editorId).append(".setReadOnly(").append(readonly).append("); ");
        script.append(editorId).append(".on('blur', function() { ");
        script.append("jQuery('#").append(getMarkupId()).append("').val(").append(editorId)
                .append(".getSession().getValue()); ");
        script.append("jQuery('#").append(getMarkupId()).append("').trigger('onBlur'); });");

//        script.append("jQuery('#").append(this.getForm().getMarkupId()).append("').bind('submit', function() { jQuery('#').val(")
//                .append(EDITOR_ID)
//                .append(".getSession().getValue()); });");

        script.append(" }");
        System.out.println(script.toString());
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
