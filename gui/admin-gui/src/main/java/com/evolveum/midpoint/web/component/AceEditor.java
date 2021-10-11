/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.prism.PrismContext;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class AceEditor extends TextArea<String> {

    public static final String MODE_XML = "ace/mode/xml";
    public static final String MODE_JSON = "ace/mode/json";
    public static final String MODE_YAML = "ace/mode/yaml";

    public static final Map<String,String> MODES = new HashMap<>();

    static {
        MODES.put(null, MODE_XML);
        MODES.put(PrismContext.LANG_XML, MODE_XML);
        MODES.put(PrismContext.LANG_JSON, MODE_JSON);
        MODES.put(PrismContext.LANG_YAML, MODE_YAML);
    }

    private IModel<Boolean> readonly = new Model(false);

    private boolean resizeToMaxHeight = true;

    private int minHeight = 200;
    private int height = minHeight;
    private String mode = MODE_XML;

    public AceEditor(String id, IModel<String> model) {
        super(id, model);
        setOutputMarkupId(true);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        StringBuilder sb = new StringBuilder();
        sb.append("initEditor('").append(getMarkupId());
        sb.append("',").append(readonly.getObject());
        sb.append(",").append(isResizeToMaxHeight());
        sb.append(",").append(getHeight());
        sb.append(",").append(getMinHeight());
        sb.append(",").append(mode != null ? "'"+mode+"'" : "null");
        sb.append(");");

        response.render(OnDomReadyHeaderItem.forScript(sb.toString()));
    }

    public int getMinHeight() {
        return minHeight;
    }

    public void setMinHeight(int minHeight) {
        this.minHeight = minHeight;
    }

    public boolean isResizeToMaxHeight() {
        return resizeToMaxHeight;
    }

    public void setResizeToMaxHeight(boolean resizeToMaxHeight) {
        this.resizeToMaxHeight = resizeToMaxHeight;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public void setModeForDataLanguage(@Nullable String dataLanguage) {
        setMode(MODES.get(dataLanguage));
    }

    public void setReadonly(boolean readonly) {
        this.readonly.setObject(readonly);
    }

    public void setReadonly(IModel<Boolean> readonly) {
        this.readonly = readonly;
    }

    public void refreshReadonly(AjaxRequestTarget target) {
        StringBuilder sb = new StringBuilder();
        sb.append("refreshReadonly('").append(getMarkupId()).append("',").append(readonly.getObject()).append(");");

        target.appendJavaScript(sb.toString());
    }

}
