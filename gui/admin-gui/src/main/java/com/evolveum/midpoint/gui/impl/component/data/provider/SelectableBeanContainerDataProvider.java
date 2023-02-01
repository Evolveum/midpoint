/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.data.provider;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.web.component.data.TypedCacheKey;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;

/**
 * @author lazyman
 * @author semancik
 */
public class SelectableBeanContainerDataProvider<C extends Containerable> extends SelectableBeanDataProvider<C> {
    private static final long serialVersionUID = 1L;


    public SelectableBeanContainerDataProvider(Component component, @NotNull IModel<Search<C>> search, Set<C> selected, boolean useDefaultSortingField) {
        super(component, search, selected, useDefaultSortingField);
    }

    @Override
    protected boolean checkOrderingSettings() {
        return true;
    }

    @Override
    protected List<C> searchObjects(Class<C> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result) throws CommonException {
        return getModelService().searchContainers(type, query, options, task, result);
    }

    protected Integer countObjects(Class<C> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> currentOptions, Task task, OperationResult result)
            throws CommonException {
        return getModelService().countContainers(type, getQuery(), currentOptions, task, result);
    }

    @Override
    protected CachedSize getCachedSize(Map<Serializable, CachedSize> cache) {
        return cache.get(new TypedCacheKey(getQuery(), getType()));
    }

    @Override
    protected void addCachedSize(Map<Serializable, CachedSize> cache, CachedSize newSize) {
        cache.put(new TypedCacheKey(getQuery(), getType()), newSize);
    }

    @Override
    public IModel<SelectableBean<C>> model(SelectableBean<C> selectableBean) {
        return null;
    }

    @Override
    protected boolean match(C selectedValue, C foundValue) {
        return selectedValue.asPrismContainerValue().equivalent(foundValue.asPrismContainerValue());
    }
}
