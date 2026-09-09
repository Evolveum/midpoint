/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse;

import java.io.Serial;
import java.io.Serializable;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

/**
 * Single chapter of help content. Chapters of one {@link HelpTab} are navigated by the pager in
 * {@link HelpContentPanel}.
 *
 */
public class HelpChapter implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    private final IModel<String> titleModel;
    private final IModel<String> contentModel;

    public HelpChapter(@NotNull IModel<String> contentModel) {
        this(Model.of(""), contentModel);
    }

    public HelpChapter(
            @NotNull IModel<String> titleModel,
            @NotNull IModel<String> contentModel) {
        this.titleModel = titleModel;
        this.contentModel = contentModel;
    }

    public IModel<String> getTitleModel() {
        return titleModel;
    }

    public IModel<String> getContentModel() {
        return contentModel;
    }

    public String getTitle() {
        String title = titleModel.getObject();
        return title != null ? title : "";
    }
}
