/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.util;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.Validate;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * @author lazyman
 */
public class ListDataProvider<T extends Serializable> extends BaseSortableDataProvider<T> {

    private IModel<List<T>> model;

    private boolean sortable;            // just to ensure backward compatibility with existing usages

    public ListDataProvider(Component component, IModel<List<T>> model) {
        this(component, model, false);
    }

    public ListDataProvider(Component component, IModel<List<T>> model, boolean sortable) {
        super(component);

        Validate.notNull(model);
        this.model = model;
        this.sortable = sortable;
    }

    @Override
    public Iterator<? extends T> internalIterator(long first, long count) {
        getAvailableData().clear();

        List<T> list = model.getObject();
        if (sortable && getSort() != null) {
            sort(list);
        }
        if (list != null) {
            long last = list.size() < (first + count) ? list.size() : (first + count);
            for (long i = first; i < last; i++) {
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
    protected <V extends Comparable<V>> void sort(List<T> list) {
        Collections.sort(list, new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                SortParam<String> sortParam = getSort();
                String propertyName = sortParam.getProperty();
                V prop1, prop2;
                try {
                    prop1 = (V) PropertyUtils.getProperty(o1, propertyName);
                    prop2 = (V) PropertyUtils.getProperty(o2, propertyName);
                } catch (RuntimeException|IllegalAccessException|InvocationTargetException|NoSuchMethodException e) {
                    throw new SystemException("Couldn't sort the object list: " + e.getMessage(), e);
                }
                int comparison = ObjectUtils.compare(prop1, prop2, true);
                return sortParam.isAscending() ? comparison : -comparison;
            }
        });
    }

    @Override
    protected int internalSize() {
        List<T> list = model.getObject();
        if (list == null) {
            return 0;
        }

        return list.size();
    }


}
