/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse;

import java.io.Serial;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * Content of the help drawer: a tab bar, a chapter pager and the chapter text.
 *
 * The tab bar is shown only when there is more than one tab and the pager only when the selected
 * tab has more than one chapter, so a single help text renders exactly as it did before.
 */
public class HelpContentPanel extends BasePanel<HelpContentModel> {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String ID_TABS = "tabs";
    private static final String ID_TAB = "tab";
    private static final String ID_TAB_LINK = "tabLink";
    private static final String ID_TAB_ICON = "tabIcon";
    private static final String ID_TAB_LABEL = "tabLabel";
    private static final String ID_PAGER_FORM = "pagerForm";
    private static final String ID_PREVIOUS_CHAPTER = "previousChapter";
    private static final String ID_NEXT_CHAPTER = "nextChapter";
    private static final String ID_CHAPTER_COUNTER = "chapterCounter";
    private static final String ID_CHAPTER_SELECT = "chapterSelect";
    private static final String ID_CHAPTER_TITLE = "chapterTitle";
    private static final String ID_CHAPTER_CONTENT = "chapterContent";

    public HelpContentPanel(String id, @NotNull IModel<HelpContentModel> model) {
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
        add(createTabsContainer());
        add(createPagerForm());
        add(createChapterTitle());
        add(createChapterContent());
    }

    private @NotNull WebMarkupContainer createTabsContainer() {
        WebMarkupContainer tabsContainer = new WebMarkupContainer(ID_TABS);
        tabsContainer.setOutputMarkupId(true);
        tabsContainer.add(createTabListView());
        tabsContainer.add(new VisibleBehaviour(() -> getModelObject().getTabs().size() > 1));
        return tabsContainer;
    }

    private @NotNull ListView<HelpTab> createTabListView() {
        return new ListView<>(ID_TAB, () -> getModelObject().getTabs()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<HelpTab> item) {
                item.add(createTabLink(item.getModelObject(), item.getIndex()));
            }
        };
    }

    private @NotNull AjaxLink<Void> createTabLink(HelpTab tab, int index) {
        AjaxLink<Void> link = new AjaxLink<>(ID_TAB_LINK) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                HelpContentPanel.this.getModelObject().selectTab(index);
                target.add(HelpContentPanel.this);
            }
        };
        link.add(AttributeAppender.append(
                "class", () -> getModelObject().isTabSelected(index) ? "active" : ""));
        link.add(createTabIcon(tab));
        link.add(new Label(ID_TAB_LABEL, tab.getTitleModel()));
        return link;
    }

    private @NotNull Component createTabIcon(HelpTab tab) {
        WebMarkupContainer icon = new WebMarkupContainer(ID_TAB_ICON);
        icon.add(AttributeAppender.append("class", tab.getIconCssClass()));
        icon.add(new VisibleBehaviour(() -> tab.getIconCssClass() != null));
        return icon;
    }

    private @NotNull Form<Void> createPagerForm() {
        Form<Void> form = new Form<>(ID_PAGER_FORM);
        form.setOutputMarkupId(true);
        form.add(new VisibleBehaviour(() -> getModelObject().getChapters().size() > 1));

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
                HelpContentPanel.this.getModelObject().previousChapter();
                target.add(HelpContentPanel.this);
            }
        };
        previous.add(new VisibleBehaviour(() -> getModelObject().hasPreviousChapter()));
        return previous;
    }

    private @NotNull AjaxLink<Void> createNextChapterLink() {
        AjaxLink<Void> next = new AjaxLink<>(ID_NEXT_CHAPTER) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                HelpContentPanel.this.getModelObject().nextChapter();
                target.add(HelpContentPanel.this);
            }
        };
        next.add(new VisibleBehaviour(() -> getModelObject().hasNextChapter()));
        return next;
    }

    private @NotNull Label createChapterCounter() {
        return new Label(ID_CHAPTER_COUNTER, () -> getModelObject().getChapterPosition());
    }

    private @NotNull DropDownChoice<HelpChapter> createChapterSelect() {
        IModel<List<HelpChapter>> chapters = () -> getModelObject().getChapters();

        DropDownChoice<HelpChapter> select = new DropDownChoice<>(
                ID_CHAPTER_SELECT,
                createSelectedChapterModel(),
                chapters,
                createChapterRenderer());
        select.setNullValid(false);
        select.add(new AjaxFormComponentUpdatingBehavior("change") {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(HelpContentPanel.this);
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
                return getModelObject().getSelectedChapter();
            }

            @Override
            public void setObject(HelpChapter chapter) {
                getModelObject().selectChapter(chapter);
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
            HelpChapter chapter = getModelObject().getSelectedChapter();
            return chapter != null ? chapter.getContentModel().getObject() : "";
        });
        content.setEscapeModelStrings(false);
        return content;
    }

    private String chapterTitle() {
        HelpChapter chapter = getModelObject().getSelectedChapter();
        return chapter != null ? chapter.getTitle() : "";
    }

}
