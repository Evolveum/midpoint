/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.data.provider;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

public abstract class SelectableBeanDataProvider<T extends Serializable> extends BaseSearchDataProvider<T, SelectableBean<T>>
        implements ISelectableDataProvider<SelectableBean<T>> {

    private static final Trace LOGGER = TraceManager.getTrace(SelectableBeanContainerDataProvider.class);
    private static final String DOT_CLASS = SelectableBeanContainerDataProvider.class.getName() + ".";
    private static final String OPERATION_SEARCH_OBJECTS = DOT_CLASS + "searchObjects";
    private static final String OPERATION_COUNT_OBJECTS = DOT_CLASS + "countObjects";

    private boolean emptyListOnNullQuery = false;
    private Collection<SelectorOptions<GetOperationOptions>> options;
    private Set<T> selected = new HashSet<>();

    // it seems that if connector doesn't support counting, provisioning return null. the default before was 0
    // which resulted to the empty list of object. Therefore we need to set this the default count to  "undefined"
    // at least for the shadows.
    private int defaultCountIfNull = 0;

    //This is quick hack how to disable counting for each preview panel on dashboard..
    // probably the solution will be to work directly with panel configuration
    private boolean isForPreview;

    private boolean useObjectCounting = true;

    private boolean export;

    public Set<T> getSelected() {
        return selected;
    }

    public void clearSelectedObjects() {
        selected.clear();
    }

    public SelectableBeanDataProvider(Component component, @NotNull IModel<Search<T>> search, Set<T> selected, boolean useDefaultSortingField) {
        super(component, search, false, useDefaultSortingField);

        if (selected != null) {
            this.selected = selected;
        }
    }

    @Override
    public Iterator<SelectableBean<T>> internalIterator(long offset, long pageSize) {
        LOGGER.trace("begin::iterator() offset {} pageSize {}.", offset, pageSize);

        preprocessSelectedData();

        OperationResult result = new OperationResult(OPERATION_SEARCH_OBJECTS);
        try {
            ObjectPaging paging = createPaging(offset, pageSize);
            Task task = getPageBase().createSimpleTask(OPERATION_SEARCH_OBJECTS);

            ObjectQuery query = getQuery();
            if (query == null) {
                if (emptyListOnNullQuery) {
                    return Collections.emptyIterator();
                }
                query = PrismContext.get().queryFactory().createQuery();
            }
            query.setPaging(paging);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Query {} with {}", getType().getSimpleName(), query.debugDump());
            }

            Collection<SelectorOptions<GetOperationOptions>> options = getSearchOptions();

            if (ResourceType.class.equals(getType()) && (options == null || options.isEmpty())) {
                options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
            }
            GetOperationOptionsBuilder optionsBuilder = getOperationOptionsBuilder(options);
            optionsBuilder = postProcessOptions(optionsBuilder);
            optionsBuilder.mergeFrom(getDistinctRelatedOptions());

            getAvailableData().addAll(createDataObjectWrappers(getType(), query, optionsBuilder.build(), task, result));

        } catch (Exception ex) {
            setupUserFriendlyMessage(result, ex);
            result.recordFatalError(getPageBase().createStringResource("ObjectDataProvider.message.listObjects.fatalError").getString(), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't list objects", ex);
            return handleNotSuccessOrHandledErrorInIterator(result);
        } finally {
            result.computeStatusIfUnknown();
        }

        LOGGER.trace("end::iterator() {}", result);
        return getAvailableData().iterator();
    }

    protected GetOperationOptionsBuilder postProcessOptions(GetOperationOptionsBuilder optionsBuilder) {
        return optionsBuilder;
    }

    protected List<SelectableBean<T>> createDataObjectWrappers(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result)
            throws CommonException {
        List<T> list = searchObjects(type, query, options, task, result);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Query {} resulted in {} objects", type.getSimpleName(), list.size());
        }

        return list.stream()
                .map(object -> createDataObjectWrapper(object))
                .collect(Collectors.toList());
    }

    private Iterator<SelectableBean<T>> handleNotSuccessOrHandledErrorInIterator(OperationResult result) {
        LOGGER.trace("handling non-success result {}", result);
        // page.showResult() will not work here. We are too deep in the rendering now.
        // Also do NOT re-throw not redirect to the error page. That will break the page.
        // Just return a SelectableBean that indicates the error.
        List<SelectableBean<T>> errorList = new ArrayList<>(1);
        SelectableBean<T> bean = createDataObjectWrapperForError();
        bean.setResult(result);
        errorList.add(bean);
        return errorList.iterator();
    }

    protected SelectableBean<T> createDataObjectWrapperForError() {
        return new SelectableBeanImpl<>();
    }

    protected abstract List<T> searchObjects(Class<T> type,
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options,
            Task task, OperationResult result) throws CommonException;

    public SelectableBean<T> createDataObjectWrapper(T obj) {
        SelectableBean<T> selectable = new SelectableBeanImpl<>(Model.of(obj));

        for (T s : getSelected()) {
            if (match(s, obj)) {
                selectable.setSelected(true);
            }
        }

        return selectable;
    }

    protected abstract boolean match(T selectedValue, T foundValue);

    private void preprocessSelectedData() {
        preprocessSelectedDataInternal();
        getAvailableData().clear();
    }

    protected void preprocessSelectedDataInternal() {
        for (SelectableBean<T> available : getAvailableData()) {
            if (available.isSelected() && available.getValue() != null) {
                ((Set) selected).add(available.getValue());
            }
        }

        for (SelectableBean<T> available : getAvailableData()) {
            if (!available.isSelected()) {
                selected.remove(available.getValue());
            }
        }
    }

    protected final Collection<SelectorOptions<GetOperationOptions>> getSearchOptions() {
        if (getCompiledObjectCollectionView() != null && getCompiledObjectCollectionView().getOptions() != null
                && !getCompiledObjectCollectionView().getOptions().isEmpty()) {
            return getCompiledObjectCollectionView().getOptions();
        }
        Collection<SelectorOptions<GetOperationOptions>> options = getOptions();

        if (options == null) {
            if (ResourceType.class.equals(getType()) || ShadowType.class.equals(getType())) {
                options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
            }
        } else {
            if (ResourceType.class.equals(getType()) || ShadowType.class.equals(getType())) {
                GetOperationOptions root = SelectorOptions.findRootOptions(options);
                if (root != null) {
                    root.setNoFetch(Boolean.TRUE);
                }
            }
        }
        return options;
    }

    public Collection<SelectorOptions<GetOperationOptions>> getOptions() {
        return options;
    }


    @Override
    protected int internalSize() {
        LOGGER.trace("begin::internalSize()");
        if (!isUseObjectCounting() || isForPreview) {
            return Integer.MAX_VALUE;
        }
        int count = 0;
        Task task = getPageBase().createSimpleTask(OPERATION_COUNT_OBJECTS);
        OperationResult result = task.getResult();
        try {
            Collection<SelectorOptions<GetOperationOptions>> currentOptions = GetOperationOptions.merge( getSearchOptions(), null);
            Integer counted = countObjects(getType(), getQuery(), currentOptions, task, result);
            count = defaultIfNull(counted, defaultCountIfNull);
        } catch (Exception ex) {
            setupUserFriendlyMessage(result, ex);
            result.recordFatalError(getPageBase().createStringResource("ObjectDataProvider.message.countObjects.fatalError").getString(), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't count objects", ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        if (!WebComponentUtil.isSuccessOrHandledError(result) && !result.isNotApplicable()) {
            getPageBase().showResult(result);
            // Let us do nothing. The error will be shown on the page and a count of 0 will be used.
            // Redirecting to the error page does more harm than good (see also MID-4306).
        }

        LOGGER.trace("end::internalSize(): {}", count);
        return count;
    }

    protected abstract Integer countObjects(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> currentOptions,
            Task task, OperationResult result)
            throws CommonException;

    public boolean isUseObjectCounting() {
        CompiledObjectCollectionView guiObjectListViewType = getCompiledObjectCollectionView();
        if (guiObjectListViewType != null && guiObjectListViewType.isDisableCounting() != null) {
            return !guiObjectListViewType.isDisableCounting();
        }
        return true;
    }


    public void setOptions(Collection<SelectorOptions<GetOperationOptions>> options) {
        this.options = options;
    }

    public boolean isEmptyListOnNullQuery() {
        return emptyListOnNullQuery;
    }

    public void setEmptyListOnNullQuery(boolean emptyListOnNullQuery) {
        this.emptyListOnNullQuery = emptyListOnNullQuery;
    }

    public void setDefaultCountIfNull(int defaultCountIfNull) {
        this.defaultCountIfNull = defaultCountIfNull;
    }

    public void setForPreview(boolean forPreview) {
        isForPreview = forPreview;
    }

    public boolean isExport() {
        return export;
    }

    public void setExport(boolean export) {
        this.export = export;
    }

}
