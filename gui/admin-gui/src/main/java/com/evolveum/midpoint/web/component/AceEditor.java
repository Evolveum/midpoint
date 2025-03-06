/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.gui.api.component.form.TextArea;
import com.evolveum.midpoint.prism.PrismContext;

import com.evolveum.midpoint.web.util.ExpressionUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.session.SessionStorage;

public class AceEditor extends TextArea<String> {

    public enum Mode {

        XML(PrismContext.LANG_XML, "ace/mode/xml"),
        JSON(PrismContext.LANG_JSON, "ace/mode/json"),
        YAML(PrismContext.LANG_YAML, "ace/mode/yaml"),
        GROOVY(ExpressionUtil.Language.GROOVY.getLanguage(), "ace/mode/groovy"),
        PYTHON(ExpressionUtil.Language.PYTHON.getLanguage(), "ace/mode/python"),
        VELOCITY(ExpressionUtil.Language.VELOCITY.getLanguage(), "ace/mode/velocity"),
        JAVASCRIPT(ExpressionUtil.Language.JAVASCRIPT.getLanguage(), "ace/mode/javascript");

        public String language;

        public String module;

        Mode(String language, String module) {
            this.language = language;
            this.module = module;
        }

        public static Mode forLanguage(String language) {
            if (StringUtils.isEmpty(language)) {
                return null;
            }

            for (Mode mode : values()) {
                if (mode.language.equals(language)) {
                    return mode;
                }
            }

            return null;
        }
    }

    private IModel<Boolean> readonly = new Model(false);

    private boolean resizeToMaxHeight = true;

    private int minHeight = 200;
    private int height = minHeight;
    private Mode mode = Mode.XML;

    public AceEditor(String id, IModel<String> model) {
        super(id, model);
        setOutputMarkupId(true);
    }

    @Override
    protected boolean shouldTrimInput() {
        return false;
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        StringBuilder sb = new StringBuilder();
        sb.append("window.MidPointAceEditor.initEditor('").append(getMarkupId());
        sb.append("',").append(readonly.getObject());
        sb.append(",").append(isResizeToMaxHeight());
        sb.append(",").append(getHeight());
        sb.append(",").append(getMinHeight());
        sb.append(",").append(mode != null ? "'" + mode.module + "'" : "''");

        boolean dark = false;
        Session session = getSession();
        if (session instanceof MidPointAuthWebSession) {
            MidPointAuthWebSession maws = (MidPointAuthWebSession) session;
            dark = maws.getSessionStorage().getMode() == SessionStorage.Mode.DARK;
        }
        sb.append(",").append(dark);
        sb.append(");");

        response.render(OnDomReadyHeaderItem.forScript(sb.toString()));
    }

    public void updateMode(AjaxRequestTarget target, Mode mode) {
        setMode(mode);

        String module = mode!= null? mode.module : "";

        target.appendJavaScript("window.MidPointAceEditor.changeMode('" + getMarkupId() + "','" + module + "');");
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

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public void setModeForDataLanguage(@Nullable String dataLanguage) {
        Mode mode = Mode.forLanguage(dataLanguage);
        setMode(mode);
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
