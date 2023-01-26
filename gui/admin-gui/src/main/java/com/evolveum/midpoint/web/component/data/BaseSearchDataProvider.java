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

import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.gui.impl.component.search.Search;

/**
 * @author lazyman
 */
public abstract class BaseSearchDataProvider<C extends Serializable, T extends Serializable>
        extends BaseSortableDataProvider<T> {

    private final IModel<Search<C>> search;

    //TODO why do we need this? variables are serialized, in case
    // of bug objects (e.g. prism objects) it might have impact on performance
    private final Map<String, Object> variables = new HashMap<>();

    private Class<C> oldType;

    private CompiledObjectCollectionView objectCollectionView;

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

    protected IModel<Search<C>> getSearchModel() {
        return search;
    }

    @Override
    public ObjectQuery getQuery() {
        return search.getObject() == null ? null : search.getObject().createObjectQuery(getVariables(), getPageBase(), getCustomizeContentQuery());
    }

    protected VariablesMap getVariables() {
        VariablesMap expVariables = new VariablesMap();
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            if (entry.getValue() == null) {
                expVariables.put(entry.getKey(), null, Object.class);
            } else {
                expVariables.put(entry.getKey(), entry.getValue(), entry.getValue().getClass());
            }
        }
        return expVariables.isEmpty() ? null : expVariables;
    }

    protected ObjectQuery getCustomizeContentQuery() {
        return null;
    }

    public Class<C> getType() {
        return search.getObject() == null ? null : (Class<C>) search.getObject().getTypeClass();
    }

    @Override
    public long size() {
        if (search.getObject() != null && !search.getObject().getTypeClass().equals(oldType) && isUseCache()) {
            clearCache();
            oldType = (Class<C>) search.getObject().getTypeClass();
        }
        return super.size();
    }

    public void addQueryVariables(String name, Object value) {
        this.variables.put(name, value);
    }


    protected CompiledObjectCollectionView getCompiledObjectCollectionView() {
        return objectCollectionView;
    }

    public void setCompiledObjectCollectionView(CompiledObjectCollectionView objectCollectionView) {
        this.objectCollectionView = objectCollectionView;
    }

    @Override
    public void detach() {
        super.detach();
        search.detach();
    }
}
