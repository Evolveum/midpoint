/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.data.provider;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;

import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.factory.wrapper.PrismContainerWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.page.error.PageError;

/**
 * Created by honchar
 */
public class ContainerListDataProvider<C extends Containerable> extends BaseSearchDataProvider<C, PrismContainerValueWrapper<C>>
        implements ISelectableDataProvider<PrismContainerValueWrapper<C>> {

    private static final Trace LOGGER = TraceManager.getTrace(ContainerListDataProvider.class);
    private static final String DOT_CLASS = ContainerListDataProvider.class.getName() + ".";
    private static final String OPERATION_SEARCH_CONTAINERS = DOT_CLASS + "searchContainers";
    private static final String OPERATION_COUNT_CONTAINERS = DOT_CLASS + "countContainers";

    private final Collection<SelectorOptions<GetOperationOptions>> options;

    public ContainerListDataProvider(Component component, @NotNull IModel<Search<C>> search) {
        this(component, search, null);
    }

    public ContainerListDataProvider(Component component, @NotNull IModel<Search<C>> search, Collection<SelectorOptions<GetOperationOptions>> options) {
        super(component, search, false, false);
        this.options = options;
    }

    @Override
    public Iterator<? extends PrismContainerValueWrapper<C>> internalIterator(long first, long count) {
        LOGGER.trace("begin::iterator() from {} count {}.", first, count);
        getAvailableData().clear();
        return doRepositoryIteration(first, count);
    }

    protected Iterator<? extends PrismContainerValueWrapper<C>> doRepositoryIteration(long first, long count) {
        Task task = getPageBase().createSimpleTask(OPERATION_SEARCH_CONTAINERS);
        OperationResult result = task.getResult();
        try {
            ObjectPaging paging = createPaging(first, count);

            ObjectQuery query = getQuery();
            if (query == null) {
                query = getPrismContext().queryFactory().createQuery();
            }
            query.setPaging(paging);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Query {} with {}", getType().getSimpleName(), query.debugDump());
            }

            List<C> list = WebModelServiceUtils.searchContainers(getType(), query, options, result, getPageBase());

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Query {} resulted in {} containers", getType().getSimpleName(), list.size());
            }

            for (C object : list) {
                PrismContainerValueWrapper<C> wrapper = createWrapper(object, task, result);
                if (wrapper != null) {
                    getAvailableData().add(wrapper);
                }
            }
        } catch (Exception ex) {
            result.recordFatalError(getPageBase().createStringResource("ContainerListDataProvider.message.listContainers.fatalError").getString(), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't list containers", ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        if (!WebComponentUtil.isSuccessOrHandledError(result)) {
            getPageBase().showResult(result);
        }

        LOGGER.trace("end::iterator()");
        return getAvailableData().iterator();
    }

    @SuppressWarnings("unchecked")
    protected PrismContainerValueWrapper<C> createWrapper(C object, Task task, OperationResult result) throws SchemaException {
        WrapperContext context = new WrapperContext(task, result);
        PrismContainerWrapperFactory<C> factory = getPageBase().findContainerWrapperFactory(object.asPrismContainerValue().getDefinition());
        return (PrismContainerValueWrapper<C>) factory.createValueWrapper(null, object.asPrismContainerValue(), ValueStatus.NOT_CHANGED, context);
    }

    @Override
    protected int internalSize() {
        LOGGER.trace("begin::internalSize()");
        int count = 0;
        OperationResult result = new OperationResult(OPERATION_COUNT_CONTAINERS);
        try {
            count = WebModelServiceUtils.countContainers(getType(), getQuery(), options, getPageBase());
        } catch (Exception ex) {
            result.recordFatalError(getPageBase().createStringResource("ContainerListDataProvider.message.listContainers.fatalError").getString(), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't count containers", ex);
        } finally {
            result.computeStatusIfUnknown();
        }

        if (!WebComponentUtil.isSuccessOrHandledError(result)) {
            getPageBase().showResult(result);
            return 0;
//            throw new RestartResponseException(PageError.class);
        }

        LOGGER.trace("end::internalSize(): {}", count);
        return count;
    }
}
