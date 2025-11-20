/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.summary;

import java.io.Serializable;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

public abstract class NumberOfObjectsWidgetDto implements Serializable {

    private final @NotNull String titleCssIcon;
    private final @NotNull IModel<String> title;
    private final @NotNull IModel<Integer> number;
    private final @NotNull IModel<String> listButtonLabel;

    protected NumberOfObjectsWidgetDto(
            @NotNull String titleCssIcon,
            @NotNull IModel<String> title,
            @NotNull IModel<Integer> number,
            @NotNull IModel<String> listButtonLabel) {
        this.titleCssIcon = titleCssIcon;
        this.title = title;
        this.number = number;
        this.listButtonLabel = listButtonLabel;
    }

    public @NotNull String getTitleCssIcon() {
        return titleCssIcon;
    }

    public @NotNull IModel<String> getTitle() {
        return title;
    }

    public @NotNull IModel<Integer> getNumber() {
        return number;
    }

    public @NotNull IModel<String> getListButtonLabel() {
        return listButtonLabel;
    }

    public abstract void openListOfObjects(AjaxRequestTarget target);
}
