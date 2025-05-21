/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.dialog;

import java.io.Serial;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * @author Viliam Repan (lazyman)
 * @author katkav
 */
public class MainPopupDialog extends ModalDialog {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_TITLE = "title";
    private static final String ID_TITLE_ICON = "titleIcon";
    private static final String ID_FOOTER = "footer";
    private static final String ID_DIALOG = "dialog";

    private IModel<String> title;
    private IModel<String> titleIconClass;

    public MainPopupDialog(String id) {
        super(id);

        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer titleIcon = new WebMarkupContainer(ID_TITLE_ICON);
        titleIcon.add(new VisibleBehaviour(this::isTitleIconVisible));
        titleIcon.add(AttributeModifier.append("class", () -> titleIconClass != null ? titleIconClass.getObject() : null));
        getDialogComponent().add(titleIcon);

        Label titleLabel = new Label(ID_TITLE, () -> title != null ? title.getObject() : null);
        titleLabel.add(new VisibleBehaviour(this::isTitleVisible));
        getDialogComponent().add(titleLabel);

        WebMarkupContainer footer = new WebMarkupContainer(ID_FOOTER);
        footer.add(VisibleBehaviour.ALWAYS_INVISIBLE);
        getDialogComponent().add(footer);
    }

    private boolean isTitleVisible() {
        return title != null && StringUtils.isNotEmpty(title.getObject());
    }

    private boolean isTitleIconVisible() {
        return isTitleVisible() && titleIconClass != null && StringUtils.isNotEmpty(titleIconClass.getObject());
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

        String overlayId = get("overlay").getMarkupId();
        target.appendJavaScript(String.format("window.MidPointTheme.showModalWithRestoreFocus('%s')", overlayId));
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
        return (WebMarkupContainer) get("overlay").get(ID_DIALOG);
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

    public void setTitleIconClass(IModel<String> titleIconClass) {
        this.titleIconClass = titleIconClass;
    }

    public void setFooter(@NotNull Component footer) {
        if (!ID_FOOTER.equals(footer.getId())) {
            throw new IllegalArgumentException("Footer component id has to be " + ID_FOOTER + ", but real value is " + footer.getId());
        }

        getDialogComponent().addOrReplace(footer);
    }

    public void setTitleComponent(@NotNull Component titleComponent) {
        if (!ID_TITLE.equals(titleComponent.getId())) {
            throw new IllegalArgumentException("Title component id has to be " + ID_TITLE + ", but real value is " + titleComponent.getId());
        }

        getDialogComponent().addOrReplace(titleComponent);
    }

    @Override
    protected WebMarkupContainer newDialog(String dialogId) {
        return new MidpointForm<>(dialogId);
    }
}
