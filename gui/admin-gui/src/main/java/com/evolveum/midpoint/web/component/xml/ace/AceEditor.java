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

import org.apache.commons.lang.Validate;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.PackageResourceReference;

public class AceEditor<T> extends TextArea<T> {
	private static final long serialVersionUID = 7245952372881965464L;
	
	public static final String F_READONLY = "readonly";
    private static final String EDITOR_SUFFIX = "_edit";
    private static final String THEME = "textmate";
    private static final String MODE = "xml";
    private String editorId;
    private String width = "100%";
    private String height = "auto";
    private IModel<Boolean> readonly = new Model<Boolean>(false);

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

        response.renderOnLoadJavaScript(createOnLoadJavascript());
    }

    private String createOnLoadJavascript() {
    	String helpButton = "<a class='helpButton' href='https://github.com/ajaxorg/ace/wiki/Default-Keyboard-Shortcuts' target='_blank' title='Show keyboard shortcuts'></a>";
        StringBuilder script = new StringBuilder();
        script.append("if(false ==Wicket.Browser.isIELessThan9()) {")
        script.append("if ($('#").append(editorId).append("').length == 0) {");
        script.append("$(\"<div id='").append(editorId).append("'></div>\").insertAfter($('#")
                .append(this.getMarkupId()).append("')); ");
        script.append(" $('#").append(editorId).append("').text($('#").append(this.getMarkupId())
                .append("').val());");
        script.append("window.").append(editorId).append(" = ace.edit(\"").append(editorId).append("\"); ");
        script.append(editorId).append(".setTheme(\"ace/theme/").append(THEME).append("\");");
        script.append(editorId).append(".getSession().setMode('ace/mode/"+ MODE +"');");
        script.append("$('#").append(this.getMarkupId()).append("').hide();");
        script.append(editorId).append(".setShowPrintMargin(false); ");
        script.append(editorId).append(".setFadeFoldWidgets(false); ");
        script.append(editorId).append(".setReadOnly(").append(isReadonly()).append("); ");
        script.append(editorId).append(".on('blur', function() { ");
        script.append("$('#").append(getMarkupId()).append("').val(").append(editorId)
                .append(".getSession().getValue()); ");
        script.append("$('#").append(getMarkupId()).append("').trigger('onBlur'); });");

        script.append(" }");
        
        script.append("if(").append(isReadonly()).append(") {");
        script.append("$('.ace_scroller').css('background','#F4F4F4');");
        script.append(" } else {");
        script.append("$('.ace_scroller').css('background','#FFFFFF');");
        script.append(" }");
        script.append("$('#" + editorId + " textarea').attr('onkeydown','disablePaste(" + isReadonly() + ");');");
        script.append(setFocus(isReadonly()));
        script.append("$('#" + editorId + "').append(\"" + helpButton + "\");");
        script.append("if($.browser.msie){$('#" + editorId + "').find('.ace_gutter').hide();}");
        script.append("}")
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
        return readonly.getObject() != null ? readonly.getObject() : false;
    }

    public void setReadonly(boolean readonly) {
        this.readonly.setObject(readonly);
    }

    public void setReadonly(IModel<Boolean> readonly) {
        Validate.notNull(readonly, "Readonly model must not be null.");
        this.readonly = readonly;
    }
    
    public String setFocus(boolean isReadonly) {
    	StringBuilder builder = new StringBuilder();
    	if(!isReadonly){
            builder.append("window.");
            builder.append(editorId);
            builder.append(".focus();");
    	}
        return builder.toString();
    }

    public String createJavascriptEditableRefresh() {
        StringBuilder builder = new StringBuilder();
        builder.append("window.");
        builder.append(editorId);
        builder.append(".setReadOnly(");
        builder.append(isReadonly());
        builder.append("); ");

        builder.append("$('.ace_scroller').css('background','");
        if (isReadonly()) {
            builder.append("#F4F4F4");
        } else {
            builder.append("#FFFFFF");
        }
        builder.append("');");
        builder.append("$('#" + editorId + " textarea').attr('onkeydown','disablePaste(" + isReadonly() + ");');");
        builder.append(setFocus(isReadonly()));
        return builder.toString();
    }
}
