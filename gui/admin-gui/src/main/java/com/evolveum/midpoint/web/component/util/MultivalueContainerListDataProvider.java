/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.util;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

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
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;

/**
 * @author katkav
 */
public class MultivalueContainerListDataProvider<C extends Containerable>
        extends BaseSortableDataProvider<PrismContainerValueWrapper<C>> {

    private final IModel<List<PrismContainerValueWrapper<C>>> model;
    private final boolean sortable; // just to ensure backward compatibility with existing usages

    public MultivalueContainerListDataProvider(
            Component component, IModel<List<PrismContainerValueWrapper<C>>> model) {
        this(component, model, false);
    }

    public MultivalueContainerListDataProvider(Component component,
            IModel<List<PrismContainerValueWrapper<C>>> model, boolean sortable) {
        super(component);

        Validate.notNull(model);
        this.model = model;
        this.sortable = sortable;
    }

    @Override
    public Iterator<? extends PrismContainerValueWrapper<C>> internalIterator(long first, long count) {
        getAvailableData().clear();

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
                getAvailableData().add(list.get(WebComponentUtil.safeLongToInteger(i)));
            }
        }

        return getAvailableData().iterator();
    }

    @SuppressWarnings("unchecked")
    protected <V extends Comparable<V>> void sort(List<PrismContainerValueWrapper<C>> list) {
        list.sort((o1, o2) -> {
            SortParam<String> sortParam = getSort();
            String propertyName = sortParam.getProperty();
            V prop1, prop2;
            try {
                prop1 = (V) PropertyUtils.getProperty(o1.getRealValue(), propertyName);
                prop2 = (V) PropertyUtils.getProperty(o2.getRealValue(), propertyName);
            } catch (RuntimeException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new SystemException("Couldn't sort the object list: " + e.getMessage(), e);
            }
            int comparison = ObjectUtils.compare(prop1, prop2, true);
            return sortParam.isAscending() ? comparison : -comparison;
        });
    }

    @Override
    protected int internalSize() {
        List<PrismContainerValueWrapper<C>> list = searchThroughList();
        if (list == null) {
            return 0;
        }

        return list.size();
    }

    public List<PrismContainerValueWrapper<C>> getSelectedData() {
        return getAvailableData().stream().filter(a -> a.isSelected()).collect(Collectors.toList());
    }

    protected List<PrismContainerValueWrapper<C>> searchThroughList() {
        List<PrismContainerValueWrapper<C>> list = model.getObject();

        if (list == null || list.isEmpty()) {
            return null;
        }

        if (getQuery() == null || getQuery().getFilter() == null) {
            return list;
        }

        List<PrismContainerValueWrapper<C>> filtered = list.stream().filter(a -> {
            try {
                return ObjectQuery.match(a.getRealValue(), getQuery().getFilter(), getPage().getMatchingRuleRegistry());
            } catch (SchemaException e) {
                throw new TunnelException(e.getMessage());
            }
        }).collect(Collectors.toList());
        return filtered;

    }

}
