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
 * <p>Content is either plain text or HTML. Plain text is rendered with the multi-line conversion
 * (newlines become {@code <br>}, blank lines become paragraph breaks). HTML content is rendered
 * as-is - the markup comes from a trusted source and must not be escaped or wrapped in extra
 * paragraph/line-break markup.
 */
public class HelpChapter implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    private final IModel<String> titleModel;
    private final IModel<String> contentModel;
    private final boolean html;

    public HelpChapter(@NotNull IModel<String> contentModel) {
        this(Model.of(""), contentModel, false);
    }

    public HelpChapter(
            @NotNull IModel<String> titleModel,
            @NotNull IModel<String> contentModel) {
        this(titleModel, contentModel, false);
    }

    public HelpChapter(
            @NotNull IModel<String> titleModel,
            @NotNull IModel<String> contentModel,
            boolean html) {
        this.titleModel = titleModel;
        this.contentModel = contentModel;
        this.html = html;
    }

    /**
     * @return {@code true} when the content is HTML to be rendered as-is, {@code false} when it
     *         is plain text to be multi-line-converted
     */
    public boolean isHtml() {
        return html;
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
