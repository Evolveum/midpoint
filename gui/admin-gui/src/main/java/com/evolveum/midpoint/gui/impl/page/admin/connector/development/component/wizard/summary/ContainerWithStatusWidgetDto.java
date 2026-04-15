/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.summary;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public abstract class ContainerWithStatusWidgetDto implements Serializable {

    private final @NotNull String titleCssIcon;
    private final @NotNull IModel<String> title;
    private final @NotNull IModel<String> statusCssClasses;
    private IModel<String> statusCssIcon;
    private final @NotNull IModel<String> status;
    private final @NotNull IModel<? extends ContainerWithStatusWidgetDetails> details;

    protected ContainerWithStatusWidgetDto(
            @NotNull String titleCssIcon,
            @NotNull IModel<String> title,
            @NotNull IModel<String> statusCssClasses,
            @NotNull IModel<String> status,
            @NotNull IModel<? extends ContainerWithStatusWidgetDetails> details) {
        this.titleCssIcon = titleCssIcon;
        this.title = title;
        this.statusCssClasses = statusCssClasses;
        this.status = status;
        this.details = details;
    }

    public ContainerWithStatusWidgetDto setStatusCssIcon(IModel<String> statusCssIcon) {
        this.statusCssIcon = statusCssIcon;
        return this;
    }

    public @NotNull String getTitleCssIcon() {
        return titleCssIcon;
    }

    public @NotNull IModel<String> getTitle() {
        return title;
    }

    public @NotNull IModel<String> getStatusCssClasses() {
        return statusCssClasses;
    }

    public IModel<String> getStatusCssIcon() {
        return statusCssIcon;
    }

    public @NotNull IModel<String> getStatus() {
        return status;
    }

    public @NotNull IModel<? extends ContainerWithStatusWidgetDetails> getDetails() {
        return details;
    }

    public abstract void editPerformed(AjaxRequestTarget target);

    public abstract boolean isEditButtonVisible();
}
