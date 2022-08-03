/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.dialog;

import com.evolveum.midpoint.web.component.form.MidpointForm;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.jetbrains.annotations.NotNull;

/**
 * @author Viliam Repan (lazyman)
 * @author katkav
 */
public class MainPopupDialog extends ModalDialog {

    private static final long serialVersionUID = 1L;

    private static final String ID_TITLE = "title";
    private static final String ID_FOOTER = "footer";

    private IModel<String> title;

    public MainPopupDialog(String id) {
        super(id);

        initLayout();
    }

    private void initLayout() {
        Label titleLabel = new Label(ID_TITLE, () -> title != null ? title.getObject() : null);
        titleLabel.add(new VisibleBehaviour(() -> title != null && StringUtils.isNotEmpty(title.getObject())));
        getDialogComponent().add(titleLabel);

        WebMarkupContainer footer = new WebMarkupContainer(ID_FOOTER);
        footer.add(VisibleBehaviour.ALWAYS_INVISIBLE);
        getDialogComponent().add(footer);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);
    }

    @Override
    public ModalDialog open(AjaxRequestTarget target) {
        ModalDialog dialog = super.open(target);
        appendJS(target, "show");

        return dialog;
    }

    @Override
    public ModalDialog close(AjaxRequestTarget target) {
        appendJS(target, "hide");

        // overlay is handled (hidden) via javascript
        return this;
    }

    private void appendJS(AjaxRequestTarget target, String operation) {
        target.appendJavaScript("$(document).ready(function () { $('#" + get("overlay").getMarkupId() + "').modal('" + operation + "'); });");
    }

    public WebMarkupContainer getDialogComponent() {
        return (WebMarkupContainer) get("overlay").get("dialog");
    }

    public Component getContentComponent() {
        return getDialogComponent().get(ModalDialog.CONTENT_ID);
    }

    public String generateWidthHeightParameter(String width, String widthUnit, String height, String heightUnit) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotEmpty(width)) {
            sb.append("min-width: " + width);
            sb.append(StringUtils.isEmpty(widthUnit) ? "px" : widthUnit);
            sb.append("; ");
        }
        return sb.toString();
    }

    public void setTitle(IModel<String> title) {
        this.title = title;
    }

    public void setFooter(@NotNull Component footer) {
        if (!ID_FOOTER.equals(footer.getId())) {
            throw new IllegalArgumentException("Footer component id has to be " + ID_FOOTER + ", but real value is " + footer.getId());
        }

        getDialogComponent().addOrReplace(footer);
    }

    @Override
    protected WebMarkupContainer newDialog(String dialogId) {
        return new MidpointForm<>(dialogId);
    }
}
