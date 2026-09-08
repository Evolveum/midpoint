/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * What the help drawer shows and where the reader currently is - the tabs together with the
 * selected tab and the selected chapter within it.
 *
 */
public class HelpContentModel implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    private final List<HelpTab> tabs;

    private int selectedTab = 0;
    private int selectedChapter = 0;

    public HelpContentModel(@NotNull List<HelpTab> tabs) {
        this.tabs = tabs;
    }

    public @NotNull List<HelpTab> getTabs() {
        return tabs;
    }

    public boolean isTabSelected(int index) {
        return index == selectedTab;
    }

    public void selectTab(int index) {
        selectedTab = index;
        selectedChapter = 0;
    }

    public @NotNull List<HelpChapter> getChapters() {
        if (tabs.isEmpty() || selectedTab >= tabs.size()) {
            return List.of();
        }
        return tabs.get(selectedTab).getChapters();
    }

    public int getSelectedChapterIndex() {
        return Math.min(selectedChapter, Math.max(getChapters().size() - 1, 0));
    }

    public @Nullable HelpChapter getSelectedChapter() {
        List<HelpChapter> chapters = getChapters();
        return chapters.isEmpty() ? null : chapters.get(getSelectedChapterIndex());
    }

    public void selectChapter(HelpChapter chapter) {
        int index = getChapters().indexOf(chapter);
        if (index >= 0) {
            selectedChapter = index;
        }
    }

    public boolean hasPreviousChapter() {
        return getSelectedChapterIndex() > 0;
    }

    public boolean hasNextChapter() {
        return getSelectedChapterIndex() < getChapters().size() - 1;
    }

    public void previousChapter() {
        if (hasPreviousChapter()) {
            selectedChapter = getSelectedChapterIndex() - 1;
        }
    }

    public void nextChapter() {
        if (hasNextChapter()) {
            selectedChapter = getSelectedChapterIndex() + 1;
        }
    }

    public String getChapterPosition() {
        return (getSelectedChapterIndex() + 1) + "/" + getChapters().size();
    }
}
