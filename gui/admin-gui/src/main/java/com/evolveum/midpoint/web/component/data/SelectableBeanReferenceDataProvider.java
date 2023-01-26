/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.Referencable;
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
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;

import static com.evolveum.midpoint.schema.DefinitionProcessingOption.FULL;
import static com.evolveum.midpoint.schema.DefinitionProcessingOption.ONLY_IF_EXISTS;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * @author lazyman
 * @author semancik
 */
public class SelectableBeanReferenceDataProvider extends BaseSearchDataProvider<ObjectReferenceType, SelectableBean<ObjectReferenceType>>
        implements ISelectableDataProvider<SelectableBean<ObjectReferenceType>>{
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(SelectableBeanReferenceDataProvider.class);
    private static final String DOT_CLASS = SelectableBeanReferenceDataProvider.class.getName() + ".";
    private static final String OPERATION_SEARCH_OBJECTS = DOT_CLASS + "searchObjects";
    private static final String OPERATION_COUNT_OBJECTS = DOT_CLASS + "countObjects";

    private Set<? extends ObjectReferenceType> selected = new HashSet<>();

    private boolean emptyListOnNullQuery = false;
    private boolean useObjectCounting = true;
//    private CompiledObjectCollectionView objectCollectionView;

    // we use special options when exporting to CSV (due to bulk nature of the operation)
    private boolean export;

    // it seems that if connector doesn't support counting, provisioning return null. the default before was 0
    // which resulted to the empty list of object. Therefore we need to set this the default count to  "undefined"
    // at least for the shadows.
    private int defaultCountIfNull = 0;

    private Collection<SelectorOptions<GetOperationOptions>> options;

    //This is quick hack how to disable counting for each preview panel on dashboard..
    // probably the solution will be to work directly with panel configuration
    private boolean isForPreview;

    public SelectableBeanReferenceDataProvider(Component component, @NotNull IModel<Search<ObjectReferenceType>> search, Set<? extends ObjectReferenceType> selected, boolean useDefaultSortingField) {
        super(component, (IModel) search, false, useDefaultSortingField);

        if (selected != null) {
            this.selected = selected;
        }
    }

    public void clearSelectedObjects() {
        selected.clear();
    }

    private void preprocessSelectedData() {
        preprocessSelectedDataInternal();
        getAvailableData().clear();
    }

    protected void preprocessSelectedDataInternal() {
        for (SelectableBean<ObjectReferenceType> available : getAvailableData()) {
            if (available.isSelected() && available.getValue() != null) {
                ((Set) selected).add(available.getValue());
            }
        }

        for (SelectableBean<ObjectReferenceType> available : getAvailableData()) {
            if (!available.isSelected()) {
                selected.remove(available.getValue());
            }
        }
    }

    @Override
    protected boolean checkOrderingSettings() {
        return true;
    }

    @Override
    public Iterator<SelectableBean<ObjectReferenceType>> internalIterator(long offset, long pageSize) {
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

            Collection<SelectorOptions<GetOperationOptions>> options = getOptions();

            GetOperationOptionsBuilder optionsBuilder = getOperationOptionsBuilder(options);
            if (export) {
                // TODO also for other classes
                if (ShadowType.class.equals(getType())) {
                    optionsBuilder = optionsBuilder
                            .definitionProcessing(ONLY_IF_EXISTS)
                            .item(ShadowType.F_FETCH_RESULT).definitionProcessing(FULL)
                            .item(ShadowType.F_AUXILIARY_OBJECT_CLASS).definitionProcessing(FULL);
                }
            }
            optionsBuilder.mergeFrom(getDistinctRelatedOptions());

            getAvailableData().addAll(createDataObjectWrappers(getType(), query, optionsBuilder.build(), task, result));

        } catch (Exception ex) {
            result.recordFatalError(getPageBase().createStringResource("ObjectDataProvider.message.listObjects.fatalError").getString(), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't list objects", ex);
            return handleNotSuccessOrHandledErrorInIterator(result);
        } finally {
            result.computeStatusIfUnknown();
        }

        LOGGER.trace("end::iterator() {}", result);
        return getAvailableData().iterator();
    }
    public List<SelectableBean<ObjectReferenceType>> createDataObjectWrappers(Class<? extends ObjectReferenceType> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result)
            throws CommonException {
        List<ObjectReferenceType> list = searchObjects(type, query, options, task, result);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Query {} resulted in {} objects", type.getSimpleName(), list.size());
        }

        List<SelectableBean<ObjectReferenceType>> data = new ArrayList<>();
        for (ObjectReferenceType object : list) {
            data.add(createDataObjectWrapper(object));
        }
        return data;
    }

    protected List<ObjectReferenceType> searchObjects(Class<? extends ObjectReferenceType> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result)
            throws CommonException {
        return getModelService().searchReferences(query, options,task, result);
    }

    protected Iterator<SelectableBean<ObjectReferenceType>> handleNotSuccessOrHandledErrorInIterator(OperationResult result) {
        LOGGER.trace("handling non-success result {}", result);
        // page.showResult() will not work here. We are too deep in the rendering now.
        // Also do NOT re-throw not redirect to the error page. That will break the page.
        // Just return a SelectableBean that indicates the error.
        List<SelectableBean<ObjectReferenceType>> errorList = new ArrayList<>(1);
        SelectableBean<ObjectReferenceType> bean = new SelectableBeanImpl<>();
        bean.setResult(result);
        errorList.add(bean);
        return errorList.iterator();
    }

    public SelectableBean<ObjectReferenceType> createDataObjectWrapper(ObjectReferenceType obj) {
        SelectableBean<ObjectReferenceType> selectable = new SelectableBeanImpl<>(Model.of(obj));

        for (ObjectReferenceType s : selected) {
            if (s.asReferenceValue().equals(obj.asReferenceValue())) {
                selectable.setSelected(true);
            }
        }

        return selectable;
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
            Collection<SelectorOptions<GetOperationOptions>> currentOptions = GetOperationOptions.merge(PrismContext.get(), getOptions(),
                    null);
            Integer counted = countObjects(getQuery(), currentOptions, task, result);
            count = defaultIfNull(counted, defaultCountIfNull);
        } catch (Exception ex) {
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

    protected Integer countObjects(ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> currentOptions, Task task, OperationResult result)
            throws CommonException {
        return getModelService().countReferences(query, currentOptions, task, result);
    }

    @Override
    protected CachedSize getCachedSize(Map<Serializable, CachedSize> cache) {
        return cache.get(new TypedCacheKey(getQuery(), getType()));
    }

    @Override
    protected void addCachedSize(Map<Serializable, CachedSize> cache, CachedSize newSize) {
        cache.put(new TypedCacheKey(getQuery(), getType()), newSize);
    }

//    public void setType(Class<C> type) {
//        Validate.notNull(type, "Class must not be null.");
//        this.type = type;
//
//        clearCache();
//    }

    protected boolean isUseObjectCounting() {
        CompiledObjectCollectionView guiObjectListViewType = getCompiledObjectCollectionView();
        if (guiObjectListViewType != null && guiObjectListViewType.isDisableCounting() != null) {
            return !guiObjectListViewType.isDisableCounting();
        }
        return true;
    }

    public Collection<SelectorOptions<GetOperationOptions>> getOptions() {
        if (getCompiledObjectCollectionView() != null && getCompiledObjectCollectionView().getOptions() != null
                && !getCompiledObjectCollectionView().getOptions().isEmpty()) {
            return getCompiledObjectCollectionView().getOptions();
        }

        Collection<SelectorOptions<GetOperationOptions>> options = this.options;

        if (options == null) {
            if (ResourceType.class.equals(getType())) {
                options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
            }
        } else {
            if (ResourceType.class.equals(getType())) {
                GetOperationOptions root = SelectorOptions.findRootOptions(options);
                root.setNoFetch(Boolean.TRUE);
            }
        }
        return options;

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

    public boolean isExport() {
        return export;
    }

    public void setExport(boolean export) {
        this.export = export;
    }

    public void setDefaultCountIfNull(int defaultCountIfNull) {
        this.defaultCountIfNull = defaultCountIfNull;
    }

    public Set<? extends ObjectReferenceType> getSelected() {
        return selected;
    }

    @Override
    public boolean isOrderingDisabled() {
        CompiledObjectCollectionView guiObjectListViewType = getCompiledObjectCollectionView();
        if (guiObjectListViewType != null && guiObjectListViewType.isDisableSorting() != null) {
            return guiObjectListViewType.isDisableSorting();
        }
        return false;
    }

    public void setForPreview(boolean forPreview) {
        isForPreview = forPreview;
    }
}
