/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;


import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxModeType;

import java.io.Serializable;
import java.util.Objects;

public abstract class AbstractSearchItemWrapper<T extends Serializable> implements Serializable {

    public static final String F_SELECTED = "selected";
    public static final String F_VALUE = "value.value";
    public static final String F_DISPLAYABLE_VALUE = "value";
    public static final String F_NAME = "name";
    public static final String F_HELP = "help";
    public static final String F_TITLE = "title";

    private DisplayableValue<T> value;
    private boolean applyFilter;
    private boolean selected;

    public abstract Class<? extends AbstractSearchItemPanel> getSearchItemPanelClass();

    public abstract String getName();

    public abstract String getHelp();

    public abstract String getTitle();

    public abstract DisplayableValue<T> getDefaultValue();

    public abstract ObjectFilter createFilter(PageBase pageBase, VariablesMap variables);

    public boolean isVisible() {
        return true;
    }

    public boolean isEnabled() {
        return true;
    }

    public boolean canRemoveSearchItem() {
        return true;
    }

    public DisplayableValue<T> getValue() {
        if (value == null) {
            setValue(getDefaultValue());
        }
        return value;
    }

    public void setValue(DisplayableValue<T> value) {
        this.value = value;
    }

    public boolean isApplyFilter(SearchBoxModeType searchBoxMode) {
        return applyFilter;
    }

    public void setApplyFilter(boolean applyFilter) {
        this.applyFilter = applyFilter;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public boolean equals(Object o) {
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash();
    }

}
