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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import org.apache.commons.lang.Validate;

import java.util.Iterator;
import java.util.List;

/**
 * @author lazyman
 */
public class RepositoryObjectDataProvider<T extends ObjectType>
        extends BaseSortableDataProvider<SelectableBean<T>> {

    private static final Trace LOGGER = TraceManager.getTrace(RepositoryObjectDataProvider.class);
    private static final String DOT_CLASS = RepositoryObjectDataProvider.class.getName() + ".";
    private static final String OPERATION_SEARCH_OBJECTS = DOT_CLASS + "searchObjects";
    private static final String OPERATION_COUNT_OBJECTS = DOT_CLASS + "countObjects";

    private Class<T> type;

    public RepositoryObjectDataProvider(PageBase page, Class<T> type) {
        super(page);

        setType(type);
    }

    @Override
    public Iterator<SelectableBean<T>> iterator(int first, int count) {
        getAvailableData().clear();

        OperationResult result = new OperationResult(OPERATION_SEARCH_OBJECTS);
        try {
            PagingType paging = createPaging(first, count);

            List<PrismObject<T>> list = getRepository().searchObjects(type, getQuery(), paging, result);
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

        return getAvailableData().iterator();
    }

    @Override
    public int size() {
        int count = 0;
        OperationResult result = new OperationResult(OPERATION_COUNT_OBJECTS);
        try {
            count = getRepository().countObjects(type, getQuery(), result);

            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't count objects.", ex);
        }

        if (!result.isSuccess()) {
            getPage().showResultInSession(result);
        }

        return count;
    }

    public void setType(Class<T> type) {
        Validate.notNull(type);
        this.type = type;
    }
}
