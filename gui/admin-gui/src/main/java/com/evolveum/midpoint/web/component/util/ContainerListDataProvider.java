/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.util;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.factory.PrismContainerWrapperFactoryImpl;
import com.evolveum.midpoint.gui.impl.factory.WrapperContext;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.page.error.PageError;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by honchar
 */
public class ContainerListDataProvider<C extends Containerable> extends BaseSortableDataProvider<PrismContainerValueWrapper<C>> {

    private static final Trace LOGGER = TraceManager.getTrace(ContainerListDataProvider.class);
    private static final String DOT_CLASS = ObjectDataProvider.class.getName() + ".";
    private static final String OPERATION_SEARCH_CONTAINERS = DOT_CLASS + "searchContainers";
    private static final String OPERATION_COUNT_CONTAINERS = DOT_CLASS + "countContainers";

    private Class<C> type;
    private Collection<SelectorOptions<GetOperationOptions>> options;

    public ContainerListDataProvider(Component component, Class<C> type) {
        this(component, type, null);
    }


    public ContainerListDataProvider(Component component, Class<C> type, Collection<SelectorOptions<GetOperationOptions>> options) {
        super(component, false, false);
        this.type = type;
        this.options = options;
    }

    @Override
    public Iterator<? extends PrismContainerValueWrapper<C>> internalIterator(long first, long count) {
        LOGGER.trace("begin::iterator() from {} count {}.", new Object[]{first, count});
        getAvailableData().clear();

        OperationResult result = new OperationResult(OPERATION_SEARCH_CONTAINERS);
        try {
            ObjectPaging paging = createPaging(first, count);
            Task task = getPage().createSimpleTask(OPERATION_SEARCH_CONTAINERS);

            ObjectQuery query = getQuery();
            if (query == null){
                query = getPrismContext().queryFactory().createQuery();
            }
            query.setPaging(paging);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Query {} with {}", type.getSimpleName(), query.debugDump());
            }

            List<C> list = getModel().searchContainers(type, query, options, task, result);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Query {} resulted in {} containers", type.getSimpleName(), list.size());
            }

            PrismContainerWrapperFactoryImpl containerFactory = new PrismContainerWrapperFactoryImpl();

            for (C object : list) {
                WrapperContext context = new WrapperContext(task, result);
                getAvailableData().add(containerFactory.createContainerValueWrapper(null, object.asPrismContainerValue(), ValueStatus.NOT_CHANGED, context));
            }
        } catch (Exception ex) {
            result.recordFatalError(getPage().createStringResource("ContainerListDataProvider.message.listContainers.fatalError").getString(), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't list containers", ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        if (!WebComponentUtil.isSuccessOrHandledError(result)) {
            getPage().showResult(result);
            throw new RestartResponseException(PageError.class);
        }

        LOGGER.trace("end::iterator()");
        return getAvailableData().iterator();
    }

//    @SuppressWarnings("unchecked")
//    protected <V extends Comparable<V>> void sort(List<PrismContainerValueWrapper<C>> list) {
//        Collections.sort(list, new Comparator<PrismContainerValueWrapper<C>>() {
//            @Override
//            public int compare(PrismContainerValueWrapper<C> o1, PrismContainerValueWrapper<C> o2) {
//                SortParam<String> sortParam = getSort();
//                String propertyName = sortParam.getProperty();
//                V prop1, prop2;
//                try {
//                    prop1 = (V) PropertyUtils.getProperty(o1.getRealValue(), propertyName);
//                    prop2 = (V) PropertyUtils.getProperty(o2.getRealValue(), propertyName);
//                } catch (RuntimeException|IllegalAccessException|InvocationTargetException |NoSuchMethodException e) {
//                    throw new SystemException("Couldn't sort the object list: " + e.getMessage(), e);
//                }
//                int comparison = ObjectUtils.compare(prop1, prop2, true);
//                return sortParam.isAscending() ? comparison : -comparison;
//            }
//        });
//    }

    @Override
    protected int internalSize() {
        LOGGER.trace("begin::internalSize()");
        int count = 0;
        OperationResult result = new OperationResult(OPERATION_COUNT_CONTAINERS);
        try {
            Task task = getPage().createSimpleTask(OPERATION_COUNT_CONTAINERS);
            count = getModel().countContainers(type, getQuery(), options, task, result);
        } catch (Exception ex) {
            result.recordFatalError(getPage().createStringResource("ContainerListDataProvider.message.listContainers.fatalError").getString(), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't count containers", ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        if (!WebComponentUtil.isSuccessOrHandledError(result)) {
            getPage().showResult(result);
            return 0;
//            throw new RestartResponseException(PageError.class);
        }

        LOGGER.trace("end::internalSize(): {}", count);
        return count;
    }

    public List<PrismContainerValueWrapper<C>> getSelectedData() {
        return getAvailableData().stream().filter(a -> a.isSelected()).collect(Collectors.toList());
    }

}
