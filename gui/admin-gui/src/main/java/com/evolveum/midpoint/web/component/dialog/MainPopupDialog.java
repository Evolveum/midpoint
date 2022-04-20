/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.dialog;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * @author Viliam Repan (lazyman)
 * @author katkav
 */
public class MainPopupDialog extends ModalDialog {

    private static final long serialVersionUID = 1L;

    private static final String ID_TITLE = "title";

    public MainPopupDialog(String id) {
        super(id);
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

    public void setTitle(StringResourceModel title) {
        Label titleLabel = new Label(ID_TITLE, title);
        titleLabel.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(title.getString())));
        getDialogComponent().addOrReplace(titleLabel);
    }
}
