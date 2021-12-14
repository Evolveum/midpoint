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
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.expression.VariablesMap;

/**
 * @author Viliam Repan (lazyman)
 */
public abstract class SearchItem<D extends AbstractSearchItemDefinition> implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Type {
        TEXT, BOOLEAN, ENUM, BROWSER, REFERENCE, FILTER, DATE, ITEM_PATH, OBJECT_COLLECTION
    }

    private final Search<Containerable> search;
    private D searchItemDefinition;

    private boolean searchItemDisplayed;
    private boolean fixed;
    private boolean editWhenVisible;
    private boolean applyFilter = true;
    private boolean selected = false;

    public SearchItem(Search search) {
        this.search = search;
    }

    public SearchItem(Search search, D searchItemDefinition) {
        this.search = search;
        this.searchItemDefinition = searchItemDefinition;
    }

    public abstract String getName();

    public abstract Type getSearchItemType();

    public abstract ObjectFilter createFilter(PageBase pageBase, VariablesMap variables);

    public abstract <S extends SearchItem, ASIP extends SearchItemPanel<S>> Class<ASIP> getSearchItemPanelClass();

    public D getSearchItemDefinition() {
        return searchItemDefinition;
    }

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

    public boolean isSearchItemDisplayed() {
        return searchItemDisplayed;
    }

    public void setSearchItemDisplayed(boolean searchItemDisplayed) {
        this.searchItemDisplayed = searchItemDisplayed;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isApplyFilter() {
        return applyFilter;
    }

    public void setApplyFilter(boolean applyFilter) {
        this.applyFilter = applyFilter;
    }

    public boolean shouldResetMoreModelOnSearchPerformed() {
        return false;
    }

    protected boolean canRemoveSearchItem() {
        return true;
    }

    public boolean isEnabled() {
        return true;
    }
}
