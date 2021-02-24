/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.web.component.search.Search;

/**
 * @author lazyman
 */
public abstract class BaseSearchDataProvider<C extends Containerable, T extends Serializable>
        extends BaseSortableDataProvider<T> {

    private final IModel<Search<C>> search;
    private final Map<String, Object> variables = new HashMap<>();

    private Class<C> oldType;

    public BaseSearchDataProvider(Component component, IModel<Search<C>> search) {
        this(component, search, false, true);
    }

    public BaseSearchDataProvider(Component component, IModel<Search<C>> search, boolean useCache) {
        this(component, search, useCache, true);
    }

    public BaseSearchDataProvider(Component component, IModel<Search<C>> search, boolean useCache, boolean useDefaultSortingField) {
        super(component, useCache, useDefaultSortingField);
        this.search = search;
        this.oldType = search.getObject() == null ? null : search.getObject().getTypeClass();
    }

    IModel<Search<C>> getSearchModel() {
        return search;
    }

    @Override
    public ObjectQuery getQuery() {
        VariablesMap expVariables = new VariablesMap();
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            if (entry.getValue() == null) {
                expVariables.put(entry.getKey(), null, Object.class);
            } else {
                expVariables.put(entry.getKey(), entry.getValue(), entry.getValue().getClass());
            }
        }
        return search.getObject() == null ? null : search.getObject().createObjectQuery(expVariables.isEmpty() ? null : expVariables, getPageBase(), getCustomizeContentQuery());
    }

    protected ObjectQuery getCustomizeContentQuery() {
        return null;
    }

    public Class<C> getType() {
        return search.getObject() == null ? null : search.getObject().getTypeClass();
    }

    @Override
    public long size() {
        if (search.getObject() != null && !search.getObject().getTypeClass().equals(oldType) && isUseCache()) {
            clearCache();
            oldType = search.getObject().getTypeClass();
        }
        return super.size();
    }

    public void addQueryVariables(String name, Object value) {
        this.variables.put(name, value);
    }

}
