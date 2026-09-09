/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse;

import java.io.Serial;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.tabs.IconPanelTab;
import com.evolveum.midpoint.web.component.TabbedPanel;

/**
 * Content of the help drawer - one tab per help source, each holding its own chapters.
 *
 * Several sources are shown by {@link TabbedPanel}, a single one goes straight to its chapters so
 * that help consisting of one text renders exactly as it did before.
 */
public class HelpContentPanel extends BasePanel<List<HelpTab>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CONTENT = "content";

    public HelpContentPanel(String id, @NotNull IModel<List<HelpTab>> model) {
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
        add(createContent());
    }

    private @NotNull Component createContent() {
        if (getTabs().size() > 1) {
            IModel<List<ITab>> tabs = this::createTabs;
            return new TabbedPanel<>(ID_CONTENT, tabs);
        }
        return new HelpChaptersPanel(ID_CONTENT, this::getChaptersOfSingleTab);
    }

    private @NotNull List<ITab> createTabs() {
        return getTabs().stream()
                .map(HelpContentPanel::createTab)
                .toList();
    }

    private static @NotNull ITab createTab(HelpTab tab) {
        return new IconPanelTab(tab.getTitleModel()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new HelpChaptersPanel(panelId, tab::getChapters);
            }

            @Override
            public IModel<String> getCssIconModel() {
                return tab::getIconCssClass;
            }
        };
    }

    private @NotNull List<HelpChapter> getChaptersOfSingleTab() {
        List<HelpTab> tabs = getTabs();
        return tabs.isEmpty() ? List.of() : tabs.get(0).getChapters();
    }

    private @NotNull List<HelpTab> getTabs() {
        List<HelpTab> tabs = getModelObject();
        return tabs != null ? tabs : List.of();
    }
}
