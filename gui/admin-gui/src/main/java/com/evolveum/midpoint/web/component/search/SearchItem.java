/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import java.io.Serializable;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.Containerable;

/**
 * @author Viliam Repan (lazyman)
 */
public abstract class SearchItem implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Type {
        TEXT, BOOLEAN, ENUM, BROWSER, REFERENCE, FILTER, DATE, ITEM_PATH, OBJECT_COLLECTION
    }

    private final Search<Containerable> search;

    private boolean fixed;
    private boolean editWhenVisible;
    private SearchItemDefinition definition;

    public SearchItem(Search search) {
        this.search = search;
    }

    public abstract String getName();

    public abstract Type getSearchItemType();

    protected String getTitle(PageBase pageBase) {
        return "";
    }

    public String getHelp(PageBase pageBase) {
        return "";
    }

    public Search<Containerable> getSearch() {
        return search;
    }

    public boolean isFixed() {
        return fixed;
    }

    public void setFixed(boolean fixed) {
        this.fixed = fixed;
    }

    public boolean isEditWhenVisible() {
        return editWhenVisible;
    }

    public void setEditWhenVisible(boolean editWhenVisible) {
        this.editWhenVisible = editWhenVisible;
    }

    public SearchItemDefinition getDefinition() {
        return definition;
    }

    public void setDefinition(SearchItemDefinition definition) {
        this.definition = definition;
    }

    protected boolean canRemoveSearchItem() {
        return true;
    }
}
