/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data;

import java.io.Serializable;
import java.util.*;

import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;

import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.web.page.error.PageError;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;

/**
 * @author lazyman
 */
public class ObjectDataProvider<W extends Serializable, T extends ObjectType>
        extends BaseSortableDataProvider<W> {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectDataProvider.class);
    private static final String DOT_CLASS = ObjectDataProvider.class.getName() + ".";
    private static final String OPERATION_SEARCH_OBJECTS = DOT_CLASS + "searchObjects";
    private static final String OPERATION_COUNT_OBJECTS = DOT_CLASS + "countObjects";

    private Set<T> selected = new HashSet<>();

    private Class<T> type;
    private Collection<SelectorOptions<GetOperationOptions>> options;

    public ObjectDataProvider(Component component, Class<T> type) {
        super(component, true);

        Validate.notNull(type);
        this.type = type;
    }

    public List<T> getSelectedData() {
        for (Serializable s : super.getAvailableData()){
            if (s instanceof SelectableBeanImpl) {
                SelectableBeanImpl<T> selectable = (SelectableBeanImpl<T>) s;
                if (selectable.isSelected() && selectable.getValue() != null) {
                    selected.add(selectable.getValue());
                }
            }
        }
        List<T> allSelected = new ArrayList<>();
        allSelected.addAll(selected);
        return allSelected;
    }

    // Here we apply the distinct option. It is easier and more reliable to apply it here than to do at all the places
    // where options for this provider are defined.
    private Collection<SelectorOptions<GetOperationOptions>> getOptionsToUse() {
        return GetOperationOptions.merge(getPrismContext(), options, getDistinctRelatedOptions());
    }


    @Override
    public Iterator<W> internalIterator(long first, long count) {
        LOGGER.trace("begin::iterator() from {} count {}.", new Object[]{first, count});

        for (W available : getAvailableData()){
            if (available instanceof SelectableBeanImpl){
                SelectableBeanImpl<T> selectableBean = (SelectableBeanImpl<T>) available;
                if (selectableBean.isSelected() && selectableBean.getValue() != null) {
                    selected.add(selectableBean.getValue());
                }
            }
        }

        for (W available : getAvailableData()) {
            if (available instanceof SelectableBeanImpl) {
                SelectableBeanImpl<T> selectableBean = (SelectableBeanImpl<T>) available;
                if (!selectableBean.isSelected()) {
                    if (selected.contains(selectableBean.getValue())) {
                        selected.remove(selectableBean.getValue());
                    }
                }
            }
        }

        getAvailableData().clear();

        OperationResult result = new OperationResult(OPERATION_SEARCH_OBJECTS);
        try {
            ObjectPaging paging = createPaging(first, count);
            Task task = getPage().createSimpleTask(OPERATION_SEARCH_OBJECTS);

            ObjectQuery query = getQuery();
            if (query == null){
                query = getPrismContext().queryFactory().createQuery();
            }
            query.setPaging(paging);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Query {} with {}", type.getSimpleName(), query.debugDump());
            }

            List<PrismObject<T>> list = getModel().searchObjects(type, query, getOptionsToUse(), task, result);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Query {} resulted in {} objects", type.getSimpleName(), list.size());
            }

            for (PrismObject<T> object : list) {
                getAvailableData().add(createDataObjectWrapper(object));
            }
        } catch (Exception ex) {
            result.recordFatalError(getPage().createStringResource("ObjectDataProvider.message.listObjects.fatalError").getString(), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't list objects", ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        if (!WebComponentUtil.isSuccessOrHandledError(result)) {
            handleNotSuccessOrHandledErrorInIterator(result);
        }

        LOGGER.trace("end::iterator()");
        return getAvailableData().iterator();
    }

    @Override
    protected boolean checkOrderingSettings() {
        return true;
    }

    protected void handleNotSuccessOrHandledErrorInIterator(OperationResult result){
        getPage().showResult(result);
        throw new RestartResponseException(PageError.class);
    }

    public W createDataObjectWrapper(PrismObject<T> obj) {
        SelectableBeanImpl<T> selectable = new SelectableBeanImpl<>(obj.asObjectable());
        if (selected.contains(obj.asObjectable())){
            selectable.setSelected(true);
        }
        return (W) selectable ;
    }

    @Override
    protected int internalSize() {
        LOGGER.trace("begin::internalSize()");
        int count = 0;
        OperationResult result = new OperationResult(OPERATION_COUNT_OBJECTS);
        try {
            Task task = getPage().createSimpleTask(OPERATION_COUNT_OBJECTS);
            count = getModel().countObjects(type, getQuery(), getOptionsToUse(), task, result);
        } catch (Exception ex) {
            result.recordFatalError(getPage().createStringResource("ObjectDataProvider.message.countObjects.fatalError").getString(), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't count objects", ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        if (!WebComponentUtil.isSuccessOrHandledError(result) && !OperationResultStatus.NOT_APPLICABLE.equals(result.getStatus())) {
            getPage().showResult(result);
            throw new RestartResponseException(PageError.class);
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

    public void setType(Class<T> type) {
        Validate.notNull(type, "Class must not be null.");
        this.type = type;

        clearCache();
    }

    public Collection<SelectorOptions<GetOperationOptions>> getOptions() {
        return options;
    }

    public void setOptions(Collection<SelectorOptions<GetOperationOptions>> options) {
        this.options = options;
    }
}
