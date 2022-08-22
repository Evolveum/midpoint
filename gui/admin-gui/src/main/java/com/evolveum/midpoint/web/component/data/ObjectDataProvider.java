/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.data;

import java.io.Serializable;
import java.util.*;

import com.evolveum.midpoint.gui.impl.component.search.Search;

import com.evolveum.midpoint.web.component.util.SelectableBean;

import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;

import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * @author lazyman
 */
public class ObjectDataProvider<W extends Serializable, O extends ObjectType>
        extends BaseSearchDataProvider<O, W> {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectDataProvider.class);
    private static final String DOT_CLASS = ObjectDataProvider.class.getName() + ".";
    private static final String OPERATION_SEARCH_OBJECTS = DOT_CLASS + "searchObjects";
    private static final String OPERATION_COUNT_OBJECTS = DOT_CLASS + "countObjects";

    private final Set<O> selected = new HashSet<>();

    private Collection<SelectorOptions<GetOperationOptions>> options;

    public ObjectDataProvider(Component component, IModel<Search<O>> search) {
        super(component, search, true);
    }

    public List<O> getSelectedData() {
        for (Serializable s : super.getAvailableData()) {
            if (s instanceof SelectableBean) {
                SelectableBean<O> selectable = (SelectableBean<O>) s;
                if (selectable.isSelected() && selectable.getValue() != null) {
                    selected.add(selectable.getValue());
                }
            }
        }
        List<O> allSelected = new ArrayList<>(selected);
        return allSelected;
    }

    // Here we apply the distinct option. It is easier and more reliable to apply it here than to do at all the places
    // where options for this provider are defined.
    protected Collection<SelectorOptions<GetOperationOptions>> getOptionsToUse() {
        return GetOperationOptions.merge(getPrismContext(), options, getDistinctRelatedOptions());
    }

    @Override
    public Iterator<W> internalIterator(long first, long count) {
        LOGGER.trace("begin::iterator() from {} count {}.", first, count);

        // todo what is this? why we're "storing" selection in iterator?
        // This most definitely should not be here, doesn't seem right.
        // what if someone calls provider to get data multiple times?
        // also provider therefore stores whole objects when being serialized...
        for (W available : getAvailableData()) {
            if (available instanceof SelectableBean) {
                SelectableBean<O> selectableBean = (SelectableBean<O>) available;
                if (selectableBean.isSelected() && selectableBean.getValue() != null) {
                    selected.add(selectableBean.getValue());
                }
                if (!selectableBean.isSelected()) {
                    selected.remove(selectableBean.getValue());
                }
            }
        }

        getAvailableData().clear();

        OperationResult result = new OperationResult(OPERATION_SEARCH_OBJECTS);
        try {
            ObjectPaging paging = createPaging(first, count);
            Task task = getPageBase().createSimpleTask(OPERATION_SEARCH_OBJECTS);

            ObjectQuery query = getQuery();
            if (query == null) {
                query = getPrismContext().queryFactory().createQuery();
            }
            query.setPaging(paging);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Query {} with {}", getType().getSimpleName(), query.debugDump());
            }

            List<PrismObject<O>> list = getModelService().searchObjects(getType(), query, getOptionsToUse(), task, result);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Query {} resulted in {} objects", getType().getSimpleName(), list.size());
            }

            for (PrismObject<O> object : list) {
                getAvailableData().add(createDataObjectWrapper(object));
            }
        } catch (Exception ex) {
            result.recordFatalError(getPageBase().createStringResource("ObjectDataProvider.message.listObjects.fatalError").getString(), ex);
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

    protected void handleNotSuccessOrHandledErrorInIterator(OperationResult result) {
        getPageBase().showResult(result);
        throw new RestartResponseException(PageError.class);
    }

    public W createDataObjectWrapper(PrismObject<O> obj) {
        SelectableBean<O> selectable = new SelectableBeanImpl<>(Model.of(obj.asObjectable()));
        if (selected.contains(obj.asObjectable())) {
            selectable.setSelected(true);
        }
        return (W) selectable;
    }

    @Override
    protected int internalSize() {
        LOGGER.trace("begin::internalSize()");
        int count = 0;
        OperationResult result = new OperationResult(OPERATION_COUNT_OBJECTS);
        try {
            Task task = getPageBase().createSimpleTask(OPERATION_COUNT_OBJECTS);
            count = getModelService().countObjects(getType(), getQuery(), getOptionsToUse(), task, result);
        } catch (Exception ex) {
            result.recordFatalError(getPageBase().createStringResource("ObjectDataProvider.message.countObjects.fatalError").getString(), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't count objects", ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        if (!WebComponentUtil.isSuccessOrHandledError(result) && !OperationResultStatus.NOT_APPLICABLE.equals(result.getStatus())) {
            getPageBase().showResult(result);
            throw new RestartResponseException(PageError.class);
        }

        LOGGER.trace("end::internalSize(): {}", count);
        return count;
    }

    @Override
    protected CachedSize getCachedSize(Map<Serializable, CachedSize> cache) {
        return cache.get(new TypedCacheKey(getQuery(), getType()));
    }

    @Override
    protected void addCachedSize(Map<Serializable, CachedSize> cache, CachedSize newSize) {
        cache.put(new TypedCacheKey(getQuery(), getType()), newSize);
    }

//    public void setType(Class<O> type) {
//        Validate.notNull(type, "Class must not be null.");
//        this.type = type;
//
//        clearCache();
//    }

    public Collection<SelectorOptions<GetOperationOptions>> getOptions() {
        return options;
    }

    public void setOptions(Collection<SelectorOptions<GetOperationOptions>> options) {
        this.options = options;
    }
}
