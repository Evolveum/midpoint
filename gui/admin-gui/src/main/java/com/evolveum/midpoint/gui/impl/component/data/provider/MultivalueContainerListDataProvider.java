/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.data.provider;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.Search;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.Validate;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;

/**
 * @author katkav
 */
public class MultivalueContainerListDataProvider<C extends Containerable> extends BaseSearchDataProvider<C, PrismContainerValueWrapper<C>>
        implements ISelectableDataProvider<PrismContainerValueWrapper<C>> {

    private static final Trace LOGGER = TraceManager.getTrace(MultivalueContainerListDataProvider.class);
    private final IModel<List<PrismContainerValueWrapper<C>>> model;
    private final boolean sortable; // just to ensure backward compatibility with existing usages
    private static final String OPERATION_INTERNAL_ITERATOR = MultivalueContainerListDataProvider.class.getName() + ".internalIterator";

    public MultivalueContainerListDataProvider(
            Component component, @NotNull IModel<Search<C>> search, IModel<List<PrismContainerValueWrapper<C>>> model) {
        this(component, search, model, false);
    }

    public MultivalueContainerListDataProvider(Component component,
                                               @NotNull IModel<Search<C>> search, IModel<List<PrismContainerValueWrapper<C>>> model, boolean sortable) {
        super(component, search);

        Validate.notNull(model);
        this.model = model;
        this.sortable = sortable;
    }

    @Override
    public Iterator<? extends PrismContainerValueWrapper<C>> internalIterator(long first, long count) {
        getAvailableData().clear();

        OperationResult result = new OperationResult(OPERATION_INTERNAL_ITERATOR );

        try {
            List<PrismContainerValueWrapper<C>> list = searchThroughList();

            if (sortable && getSort() != null) {
                sort(list);
            }
            if (list != null) {
                for (long i = first; i < first + count; i++) {
                    if (i < 0 || i >= list.size()) {
                        throw new ArrayIndexOutOfBoundsException("Trying to get item on index " + i
                                + " but list size is " + list.size());
                    }
                    PrismContainerValueWrapper<C> valueWrapper = list.get(WebComponentUtil.safeLongToInteger(i));
                    postProcessWrapper(valueWrapper);
                    getAvailableData().add(valueWrapper);
                }
            }

        } catch (Exception e) {
            result.recordFatalError(e.getMessage(),e);
            return handleNotSuccessOrHandledErrorInIterator(result);
        } finally {
            result.computeStatusIfUnknown();
        }

        return getAvailableData().iterator();
    }

    protected Iterator<PrismContainerValueWrapper<C>> handleNotSuccessOrHandledErrorInIterator(OperationResult result) {
        List<PrismContainerValueWrapper<C>> errorList = new ArrayList<>(1);
        getPageBase().showResult(result);
        return errorList.iterator();
    }

    protected void postProcessWrapper(PrismContainerValueWrapper<C> valueWrapper) {
        // nothig to do, use when e.g. references needs to be resolved, etc..
    }

    private <V extends Comparable<V>> V getPropertyValue(PrismContainerValueWrapper<C> o1, String propertyName) {
        try {
            return (V) PropertyUtils.getProperty(o1.getRealValue(), propertyName);
        } catch (RuntimeException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    protected <V extends Comparable<V>> void sort(List<PrismContainerValueWrapper<C>> list) {
        list.sort((o1, o2) -> {
            SortParam<String> sortParam = getSort();
            String propertyName = sortParam.getProperty();
            V prop1 = getPropertyValue(o1, propertyName);
            V prop2 = getPropertyValue(o2, propertyName);

            int comparison = ObjectUtils.compare(prop1, prop2, true);
            return sortParam.isAscending() ? comparison : -comparison;
        });
    }

    @Override
    protected int internalSize() {
        OperationResult result = new OperationResult(OPERATION_INTERNAL_ITERATOR );
        try {
            List<PrismContainerValueWrapper<C>> list = searchThroughList();
            if (list == null) {
                return 0;
            }
            return list.size();
        } catch (Exception e) {
            result.recordFatalError(e.getMessage(),e);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't count objects", e);
            handleNotSuccessOrHandledErrorInIterator(result);
            return 0;
        }

    }


    protected List<PrismContainerValueWrapper<C>> searchThroughList() {
        List<PrismContainerValueWrapper<C>> list = model.getObject();

        if (list == null || list.isEmpty()) {
            return null;
        }

        ObjectQuery query = getQuery();
        if (query == null || query.getFilter() == null) {
            return list;
        }

        List<PrismContainerValueWrapper<C>> filtered = list.stream().filter(valueWrapper -> {
            try {
                return ObjectQuery.match(valueWrapper.getRealValue(), query.getFilter(), getPageBase().getMatchingRuleRegistry());
            } catch (SchemaException e) {
                throw new TunnelException(e.getMessage());
            }
        }).collect(Collectors.toList());
        return filtered;
    }

    @Override
    public void clearCache() {
        super.clearCache();
        model.detach();
    }

    public IModel<List<PrismContainerValueWrapper<C>>> getModel() {
        return model;
    }

}
