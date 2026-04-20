/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import java.io.Serializable;
import java.util.Objects;

import com.evolveum.midpoint.gui.impl.component.search.panel.AbstractSearchItemPanel;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.util.SelectableRow;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxModeType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractSearchItemWrapper<T> implements Serializable, SelectableRow {

    public static final String F_SELECTED = "selected";
    public static final String F_VALUE = "value.value";
    public static final String F_DISPLAYABLE_VALUE = "value";
    public static final String F_APPLY_FILTER = "applyFilter";

    private DisplayableValue<T> value;
    private boolean applyFilter;

    /**
     * flag that stores whether wrapper item is "selected" via checkbox in "More" popup in search bar
     */
    private boolean selected;
    /**
     * information about whether wrapper item (or in case of UI some panel) is visible in search bar
     */
    private boolean visible;
    /**
     * whether wrapper item (or in case of UI some panel) can be removed from search bar
     *
     * todo rename to canHide
     */
    private boolean canConfigure = true;
    private SearchFilterType predefinedFilter;
    private ExpressionType filterExpression;

    String parameterName;
    Class<T> parameterValueType;

    public abstract Class<? extends AbstractSearchItemPanel> getSearchItemPanelClass();

    @NotNull
    public abstract IModel<String> getName();

    @NotNull
    public abstract IModel<String> getHelp();

    @NotNull
    public abstract IModel<String> getTitle();

    public abstract DisplayableValue<T> getDefaultValue();

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isEnabled() {
        return true;
    }

    public boolean canRemoveSearchItem() {
        return canConfigure;
    }

    public void setCanConfigure(boolean canConfigure) {
        this.canConfigure = canConfigure;
    }

    public DisplayableValue<T> getValue() {
        if (value == null) {
            setValue(getDefaultValue());
        }
        return value;
    }

    public void clearValue() {
        setValue(getDefaultValue());
    }

    public String getParameterName() {
        return parameterName;
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

    public Class<T> getParameterValueType() {
        return parameterValueType;
    }

    public void setParameterValueType(Class<T> parameterValueType) {
        this.parameterValueType = parameterValueType;
    }

    public void setValue(DisplayableValue<T> value) {
        this.value = value;
    }

    public boolean isApplyFilter(SearchBoxModeType searchBoxMode) {
        return applyPredefinedFilter() || isVisible();
    }

    public boolean applyPredefinedFilter() {
        return getPredefinedFilter() != null && applyFilter;
    }

    public void setApplyFilter(boolean applyFilter) {
        this.applyFilter = applyFilter;
    }

    @Override
    public boolean isSelected() {
        return selected;
    }

    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public SearchFilterType getPredefinedFilter() {
        return predefinedFilter;
    }

    public void setPredefinedFilter(SearchFilterType predefinedFilter) {
        this.predefinedFilter = predefinedFilter;
    }

    public ExpressionType getFilterExpression() {
        return filterExpression;
    }

    public void setFilterExpression(ExpressionType filterExpression) {
        this.filterExpression = filterExpression;
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
