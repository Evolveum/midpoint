/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.data.provider;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.ObjectHandler;
import com.evolveum.midpoint.web.component.util.SelectableBean;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
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
    protected boolean match(C selectedValue, C foundValue) {
        return selectedValue.asPrismContainerValue().equivalent(foundValue.asPrismContainerValue());
    }

    @Override
    public boolean supportsIterativeExport() {
        return true;
    }

    /**
     * Streaming export using JDBC cursor-based streaming.
     * This method does not load all data into memory - uses true JDBC streaming.
     * Streaming is enabled by setting iterationPageSize to -1.
     */
    @Override
    public void exportIterative(
            ObjectHandler<SelectableBean<C>> handler,
            Task task,
            OperationResult result) throws CommonException {

        ObjectQuery query = getQuery();
        if (query == null) {
            query = PrismContext.get().queryFactory().createQuery();
        }
        // Set ordering from current sort settings (no offset/limit for full export)
        query.setPaging(createPaging(0, Integer.MAX_VALUE));

        // Enable JDBC streaming mode by setting iterationPageSize to -1
        Collection<SelectorOptions<GetOperationOptions>> streamingOptions =
                SelectorOptions.updateRootOptions(getSearchOptions(),
                        opt -> opt.setIterationPageSize(-1), GetOperationOptions::new);

        searchObjectsIterative(getType(), query,
                (object, opResult) -> {
                    SelectableBean<C> wrapper = createDataObjectWrapper(object);
                    return handler.handle(wrapper, opResult);
                },
                streamingOptions, task, result);
    }

    /**
     * Override this method to use a different iterative search implementation.
     * Default implementation uses ModelService.searchContainersIterative().
     */
    protected void searchObjectsIterative(Class<C> type, ObjectQuery query,
            ObjectHandler<C> handler,
            Collection<SelectorOptions<GetOperationOptions>> options,
            Task task, OperationResult result) throws CommonException {
        getModelService().searchContainersIterative(type, query, handler, options, task, result);
    }
}
