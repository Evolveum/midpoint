/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.data;

import java.io.Serializable;
import java.util.*;

import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;

import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SelectableBean;
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

    private Class<T> type;
    private Collection<SelectorOptions<GetOperationOptions>> options;

    public ObjectDataProvider(Component component, Class<T> type) {
        super(component, true);

        Validate.notNull(type);
        this.type = type;
    }

    @Override
    public Iterator<W> internalIterator(long first, long count) {
        LOGGER.trace("begin::iterator() from {} count {}.", new Object[]{first, count});
        getAvailableData().clear();

        OperationResult result = new OperationResult(OPERATION_SEARCH_OBJECTS);
        try {
            ObjectPaging paging = createPaging(first, count);
            Task task = getPage().createSimpleTask(OPERATION_SEARCH_OBJECTS);
            
            ObjectQuery query = getQuery();
            if (query == null){
            	query = new ObjectQuery();
            }
            query.setPaging(paging);

            LOGGER.trace("Query {} with {}", type.getSimpleName(), query.debugDump());

            List<PrismObject<T>> list = getModel().searchObjects(type, query, options, task, result);
            for (PrismObject<T> object : list) {
                getAvailableData().add(createDataObjectWrapper(object));
            }
        } catch (Exception ex) {
            result.recordFatalError("Couldn't list objects.", ex);
            LoggingUtils.logException(LOGGER, "Couldn't list objects", ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        if (!WebMiscUtil.isSuccessOrHandledError(result)) {
            getPage().showResultInSession(result);
            throw new RestartResponseException(PageError.class);
        }

        LOGGER.trace("end::iterator()");
        return getAvailableData().iterator();
    }

    public W createDataObjectWrapper(PrismObject<T> obj) {
        return (W) new SelectableBean<T>(obj.asObjectable());
    }

    @Override
    protected int internalSize() {
        LOGGER.trace("begin::internalSize()");
        int count = 0;
        OperationResult result = new OperationResult(OPERATION_COUNT_OBJECTS);
        try {
            Task task = getPage().createSimpleTask(OPERATION_COUNT_OBJECTS);
            count = getModel().countObjects(type, getQuery(), options, task, result);
        } catch (Exception ex) {
            result.recordFatalError("Couldn't count objects.", ex);
            LoggingUtils.logException(LOGGER, "Couldn't count objects", ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        if (!WebMiscUtil.isSuccessOrHandledError(result)) {
            getPage().showResultInSession(result);
            throw new RestartResponseException(PageError.class);
        }

        LOGGER.trace("end::internalSize()");
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
