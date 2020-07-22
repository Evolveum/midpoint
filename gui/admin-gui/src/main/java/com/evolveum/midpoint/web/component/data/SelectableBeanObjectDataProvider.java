/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import static com.evolveum.midpoint.schema.DefinitionProcessingOption.FULL;
import static com.evolveum.midpoint.schema.DefinitionProcessingOption.ONLY_IF_EXISTS;

import java.io.Serializable;
import java.util.*;

import com.evolveum.midpoint.web.component.util.SelectableListDataProvider;

import org.apache.commons.lang.Validate;
import org.apache.wicket.Component;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author lazyman
 * @author semancik
 */
public class SelectableBeanObjectDataProvider<O extends ObjectType> extends SelectableListDataProvider<SelectableBean<O>, O> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(SelectableBeanObjectDataProvider.class);
    private static final String DOT_CLASS = SelectableBeanObjectDataProvider.class.getName() + ".";
    private static final String OPERATION_SEARCH_OBJECTS = DOT_CLASS + "searchObjects";
    private static final String OPERATION_COUNT_OBJECTS = DOT_CLASS + "countObjects";

    private Set<? extends O> selected = new HashSet<>();

    private boolean emptyListOnNullQuery = false;
    private boolean useObjectCounting = true;

    // we use special options when exporting to CSV (due to bulk nature of the operation)
    private boolean export;

    // it seems that if connector doesn't support counting, provisioning return null. the default before was 0
    // which resulted to the empty list of object. Therefore we need to set this the default count to  "undefined"
    // at least for the shadows.
    private int defaultCountIfNull = 0;

    private Class<? extends O> type;
    private Collection<SelectorOptions<GetOperationOptions>> options;

    public SelectableBeanObjectDataProvider(Component component, Class<? extends O> type, Set<? extends O> selected) {
        super(component, false, true);

        Validate.notNull(type);
        if (selected != null) {
            this.selected = selected;
        }
        this.type = type;
    }

    public void clearSelectedObjects() {
        selected.clear();
    }

//    @NotNull
//    public List<O> getSelectedObjects() {
//        preprocessSelectedDataInternal();
//        for (SelectableBean<O> selectable : super.getAvailableData()) {
//            if (selectable.isSelected() && selectable.getValue() != null) {
//                ((Set) selected).add(selectable.getValue());
//            }
//        }
//        return new ArrayList<>(selected);
//    }

    private void preprocessSelectedData() {
        preprocessSelectedDataInternal();
        getAvailableData().clear();
    }

    private void preprocessSelectedDataInternal() {
        for (SelectableBean<O> available : getAvailableData()) {
            if (available.isSelected() && available.getValue() != null) {
                ((Set) selected).add(available.getValue());
            }
        }

        for (SelectableBean<O> available : getAvailableData()) {
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
    public Iterator<SelectableBean<O>> internalIterator(long offset, long pageSize) {
        LOGGER.trace("begin::iterator() offset {} pageSize {}.", new Object[] { offset, pageSize });

        preprocessSelectedData();

        OperationResult result = new OperationResult(OPERATION_SEARCH_OBJECTS);
        try {
            ObjectPaging paging = createPaging(offset, pageSize);
            Task task = getPage().createSimpleTask(OPERATION_SEARCH_OBJECTS);

            ObjectQuery query = getQuery();
            if (query == null) {
                if (emptyListOnNullQuery) {
                    return Collections.emptyIterator();
                }
                query = getPrismContext().queryFactory().createQuery();
            }
            query.setPaging(paging);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Query {} with {}", type.getSimpleName(), query.debugDump());
            }

            if (ResourceType.class.equals(type) && (options == null || options.isEmpty())) {
                options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
            }
            GetOperationOptionsBuilder optionsBuilder = getOperationOptionsBuilder(options);
            if (export) {
                // TODO also for other classes
                if (ShadowType.class.equals(type)) {
                    optionsBuilder = optionsBuilder
                            .definitionProcessing(ONLY_IF_EXISTS)
                            .item(ShadowType.F_FETCH_RESULT).definitionProcessing(FULL)
                            .item(ShadowType.F_AUXILIARY_OBJECT_CLASS).definitionProcessing(FULL);
                }
            }
            optionsBuilder.mergeFrom(getDistinctRelatedOptions());
            List<PrismObject<? extends O>> list = (List) getModel().searchObjects(type, query, optionsBuilder.build(), task, result);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Query {} resulted in {} objects", type.getSimpleName(), list.size());
            }

            for (PrismObject<? extends O> object : list) {
                getAvailableData().add(createDataObjectWrapper(object.asObjectable()));
            }
        } catch (Exception ex) {
            result.recordFatalError(getPage().createStringResource("ObjectDataProvider.message.listObjects.fatalError").getString(), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't list objects", ex);
            return handleNotSuccessOrHandledErrorInIterator(result);
        } finally {
            result.computeStatusIfUnknown();
        }

        LOGGER.trace("end::iterator() {}", result);
        return getAvailableData().iterator();
    }

    protected Iterator<SelectableBean<O>> handleNotSuccessOrHandledErrorInIterator(OperationResult result) {
        LOGGER.trace("handling non-success result {}", result);
        // page.showResult() will not work here. We are too deep in the rendering now.
        // Also do NOT re-throw not redirect to to error page. That will break the page.
        // Just return a SelectableBean that indicates the error.
        List<SelectableBean<O>> errorList = new ArrayList<>(1);
        SelectableBean<O> bean = new SelectableBeanImpl<>();
        bean.setResult(result);
        errorList.add(bean);
        return errorList.iterator();
    }

    public SelectableBean<O> createDataObjectWrapper(O obj) {
        SelectableBean<O> selectable = new SelectableBeanImpl<>(obj);

        if (!WebComponentUtil.isSuccessOrHandledError(obj.getFetchResult())) {
            try {
                selectable.setResult(obj.getFetchResult());
            } catch (SchemaException e) {
                throw new SystemException(e.getMessage(), e);
            }
        }
        for (O s : selected) {
            if (s.getOid().equals(obj.getOid())) {
                selectable.setSelected(true);
            }
        }

        return selectable;
    }

    @Override
    protected int internalSize() {
        LOGGER.trace("begin::internalSize()");
        if (!isUseObjectCounting()) {
            return Integer.MAX_VALUE;
        }
        int count = 0;
        Task task = getPage().createSimpleTask(OPERATION_COUNT_OBJECTS);
        OperationResult result = task.getResult();
        try {
            Collection<SelectorOptions<GetOperationOptions>> currentOptions = GetOperationOptions.merge(getPrismContext(), options, getDistinctRelatedOptions());
            Integer counted = getModel().countObjects(type, getQuery(), currentOptions, task, result);
            count = defaultIfNull(counted, defaultCountIfNull);
        } catch (Exception ex) {
            result.recordFatalError(getPage().createStringResource("ObjectDataProvider.message.countObjects.fatalError").getString(), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't count objects", ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        if (!WebComponentUtil.isSuccessOrHandledError(result) && !result.isNotApplicable()) {
            getPage().showResult(result);
            // Let us do nothing. The error will be shown on the page and a count of 0 will be used.
            // Redirecting to the error page does more harm than good (see also MID-4306).
        }

        LOGGER.trace("end::internalSize(): {}", count);
        return count;
    }

    @Override
    protected CachedSize getCachedSize(Map<Serializable, CachedSize> cache) {
        return cache.get(new TypedCacheKey(getQuery(), type));
    }

    @Override
    protected void addCachedSize(Map<Serializable, CachedSize> cache, CachedSize newSize) {
        cache.put(new TypedCacheKey(getQuery(), type), newSize);
    }

    public void setType(Class<O> type) {
        Validate.notNull(type, "Class must not be null.");
        this.type = type;

        clearCache();
    }

    protected boolean isUseObjectCounting() {
        return useObjectCounting;
    }

    public void setUseObjectCounting(boolean useCounting) {
        this.useObjectCounting = useCounting;
    }

    public Collection<SelectorOptions<GetOperationOptions>> getOptions() {
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
}
