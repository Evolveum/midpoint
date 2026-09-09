/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse;

import java.io.Serial;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * Chapters of one help tab: a pager to walk through them and the text of the selected one.
 *
 * The pager is shown only when there is more than one chapter, so a single untitled chapter
 * renders as plain text.
 */
public class HelpChaptersPanel extends BasePanel<List<HelpChapter>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_PAGER_FORM = "pagerForm";
    private static final String ID_PREVIOUS_CHAPTER = "previousChapter";
    private static final String ID_NEXT_CHAPTER = "nextChapter";
    private static final String ID_CHAPTER_COUNTER = "chapterCounter";
    private static final String ID_CHAPTER_SELECT = "chapterSelect";
    private static final String ID_CHAPTER_TITLE = "chapterTitle";
    private static final String ID_CHAPTER_CONTENT = "chapterContent";

    private int selectedChapter = 0;

    public HelpChaptersPanel(String id, @NotNull IModel<List<HelpChapter>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);

        initLayout();
    }

    private void initLayout() {
        add(createPagerForm());
        add(createChapterTitle());
        add(createChapterContent());
    }

    private @NotNull Form<Void> createPagerForm() {
        Form<Void> form = new Form<>(ID_PAGER_FORM);
        form.setOutputMarkupId(true);
        form.add(new VisibleBehaviour(() -> getChapters().size() > 1));

        form.add(createPreviousChapterLink());
        form.add(createChapterCounter());
        form.add(createChapterSelect());
        form.add(createNextChapterLink());

        return form;
    }

    private @NotNull AjaxLink<Void> createPreviousChapterLink() {
        AjaxLink<Void> previous = new AjaxLink<>(ID_PREVIOUS_CHAPTER) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                HelpChaptersPanel.this.previousChapter();
                target.add(HelpChaptersPanel.this);
            }
        };
        previous.add(new VisibleBehaviour(() -> getSelectedChapterIndex() > 0));
        return previous;
    }

    private @NotNull AjaxLink<Void> createNextChapterLink() {
        AjaxLink<Void> next = new AjaxLink<>(ID_NEXT_CHAPTER) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                HelpChaptersPanel.this.nextChapter();
                target.add(HelpChaptersPanel.this);
            }
        };
        next.add(new VisibleBehaviour(() -> getSelectedChapterIndex() < getChapters().size() - 1));
        return next;
    }

    private @NotNull Label createChapterCounter() {
        return new Label(ID_CHAPTER_COUNTER,
                () -> (getSelectedChapterIndex() + 1) + "/" + getChapters().size());
    }

    private @NotNull DropDownChoice<HelpChapter> createChapterSelect() {
        DropDownChoice<HelpChapter> select = new DropDownChoice<>(
                ID_CHAPTER_SELECT,
                createSelectedChapterModel(),
                getModel(),
                createChapterRenderer());
        select.setNullValid(false);
        select.add(new AjaxFormComponentUpdatingBehavior("change") {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(HelpChaptersPanel.this);
            }
        });
        return select;
    }

    private @NotNull IChoiceRenderer<HelpChapter> createChapterRenderer() {
        return new IChoiceRenderer<>() {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public Object getDisplayValue(HelpChapter chapter) {
                return chapter.getTitle();
            }

            @Override
            public String getIdValue(HelpChapter chapter, int index) {
                return String.valueOf(index);
            }
        };
    }

    private @NotNull IModel<HelpChapter> createSelectedChapterModel() {
        return new IModel<>() {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public HelpChapter getObject() {
                return getSelectedChapter();
            }

            @Override
            public void setObject(HelpChapter chapter) {
                int index = getChapters().indexOf(chapter);
                if (index >= 0) {
                    selectedChapter = index;
                }
            }
        };
    }

    private @NotNull Label createChapterTitle() {
        Label chapterTitle = new Label(ID_CHAPTER_TITLE, () -> chapterTitle());
        chapterTitle.add(new VisibleBehaviour(() -> !chapterTitle().isEmpty()));
        return chapterTitle;
    }

    private @NotNull MultiLineLabel createChapterContent() {
        MultiLineLabel content = new MultiLineLabel(ID_CHAPTER_CONTENT, () -> {
            HelpChapter chapter = getSelectedChapter();
            return chapter != null ? chapter.getContentModel().getObject() : "";
        });
        content.setEscapeModelStrings(false);
        return content;
    }

    private void previousChapter() {
        if (getSelectedChapterIndex() > 0) {
            selectedChapter = getSelectedChapterIndex() - 1;
        }
    }

    private void nextChapter() {
        if (getSelectedChapterIndex() < getChapters().size() - 1) {
            selectedChapter = getSelectedChapterIndex() + 1;
        }
    }

    private String chapterTitle() {
        HelpChapter chapter = getSelectedChapter();
        return chapter != null ? chapter.getTitle() : "";
    }

    private int getSelectedChapterIndex() {
        return Math.min(selectedChapter, Math.max(getChapters().size() - 1, 0));
    }

    private @Nullable HelpChapter getSelectedChapter() {
        List<HelpChapter> chapters = getChapters();
        return chapters.isEmpty() ? null : chapters.get(getSelectedChapterIndex());
    }

    private @NotNull List<HelpChapter> getChapters() {
        List<HelpChapter> chapters = getModelObject();
        return chapters != null ? chapters : List.of();
    }
}
