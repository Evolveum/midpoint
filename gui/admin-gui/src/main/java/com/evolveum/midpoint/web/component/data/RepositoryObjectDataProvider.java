/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.data;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.schema.ObjectOperationOption;
import com.evolveum.midpoint.schema.ObjectOperationOptions;
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;

/**
 * @author lazyman
 */
public class RepositoryObjectDataProvider<T extends ObjectType>
        extends BaseSortableDataProvider<SelectableBean<T>> {

    private static final String DOT_CLASS = RepositoryObjectDataProvider.class.getName() + ".";
    private static final String OPERATION_SEARCH_OBJECTS = DOT_CLASS + "searchObjects";
    private static final String OPERATION_COUNT_OBJECTS = DOT_CLASS + "countObjects";

    private static final Trace LOGGER = TraceManager.getTrace(RepositoryObjectDataProvider.class);
    private Class<T> type;

    public RepositoryObjectDataProvider(PageBase page, Class<T> type) {
        super(page, true);

        setType(type);
    }

    @Override
    public Iterator<SelectableBean<T>> iterator(int first, int count) {
        LOGGER.trace("begin::iterator() from {} count {}.", new Object[]{first, count});
        getAvailableData().clear();

        OperationResult result = new OperationResult(OPERATION_SEARCH_OBJECTS);
        try {
            ObjectPaging paging = createPaging(first, count);
			ObjectQuery query = getQuery();
			if (query == null) {
				query = new ObjectQuery();
			}
			query.setPaging(paging);

            List<PrismObject<T>> list = getModel().searchObjects(type, query,
                    ObjectOperationOptions.createCollection(new PropertyPath(), ObjectOperationOption.RAW),
                    getPage().createSimpleTask(OPERATION_SEARCH_OBJECTS), result);
            for (PrismObject<T> object : list) {
                getAvailableData().add(new SelectableBean<T>(object.asObjectable()));
            }

            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't list objects.", ex);
        }

        if (!result.isSuccess()) {
            getPage().showResultInSession(result);
        }

        LOGGER.trace("end::iterator()");
        return getAvailableData().iterator();
    }

    @Override
    protected int internalSize() {
        LOGGER.trace("begin::internalSize()");
        int count = 0;
        OperationResult result = new OperationResult(OPERATION_COUNT_OBJECTS);
        try {
            count = getModel().countObjects(type, getQuery(),
                    ObjectOperationOptions.createCollection(new PropertyPath(), ObjectOperationOption.RAW),
                    getPage().createSimpleTask(OPERATION_COUNT_OBJECTS), result);

            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't count objects.", ex);
        }

        if (!result.isSuccess()) {
            getPage().showResultInSession(result);
        }
        LOGGER.trace("end::internalSize()");
        return count;
    }

    public void setType(Class<T> type) {
        Validate.notNull(type);
        this.type = type;
    }

    @Override
    protected CachedSize getCachedSize(Map<Serializable, CachedSize> cache) {
        return cache.get(new TypedCacheKey(getQuery(), type));
    }

    @Override
    protected void addCachedSize(Map<Serializable, CachedSize> cache, CachedSize newSize) {
        cache.put(new TypedCacheKey(getQuery(), type), newSize);
    }
}
