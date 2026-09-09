/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

/**
 * One tab of the help drawer, holding its own list of chapters.
 *
 * A single tab is rendered without the tab bar, so existing callers passing one help text keep
 * the original look.
 */
public class HelpTab implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    private final IModel<String> titleModel;
    private final String iconCssClass;
    private final List<HelpChapter> chapters;

    public HelpTab(@NotNull List<HelpChapter> chapters) {
        this(Model.of(""), chapters);
    }

    public HelpTab(
            @NotNull IModel<String> titleModel,
            @NotNull List<HelpChapter> chapters) {
        this(titleModel, null, chapters);
    }

    public HelpTab(
            @NotNull IModel<String> titleModel,
            String iconCssClass,
            @NotNull List<HelpChapter> chapters) {
        this.titleModel = titleModel;
        this.iconCssClass = iconCssClass;
        this.chapters = chapters;
    }

    public IModel<String> getTitleModel() {
        return titleModel;
    }

    public String getIconCssClass() {
        return iconCssClass;
    }

    public List<HelpChapter> getChapters() {
        return chapters;
    }
}
