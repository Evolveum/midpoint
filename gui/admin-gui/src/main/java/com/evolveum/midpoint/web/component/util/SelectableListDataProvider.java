/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.util;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.Validate;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;

import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

/**
 * @author lazyman
 */
public class SelectableListDataProvider<W extends SelectableBean, T extends Serializable>
        extends BaseSortableDataProvider<W> {

    private IModel<List<T>> model;

    public SelectableListDataProvider(Component Component, IModel<List<T>> model) {
        super(Component);

        Validate.notNull(model);
        this.model = model;
    }

    public SelectableListDataProvider(Component Component, boolean useCache, boolean useDefaultSortingField) {
        super(Component, useCache, useDefaultSortingField);
        this.model = Model.ofList(Collections.EMPTY_LIST);
    }

    @Override
    public Iterator<W> internalIterator(long first, long count) {

        getAvailableData().clear();

        List<T> list = model.getObject();
        if (list != null) {
            for (long i = first; i < first + count; i++) {
                if (i < 0 || i >= list.size()) {
                    throw new ArrayIndexOutOfBoundsException(
                            "Trying to get item on index " + i + " but list size is " + list.size());
                }
                getAvailableData().add(createDataObjectWrapper(list.get(WebComponentUtil.safeLongToInteger(i))));
            }
        }

        return getAvailableData().iterator();
    }

    protected W createDataObjectWrapper(T object) {
        return (W) new SelectableBeanImpl<>(object);
    }

    @Override
    protected int internalSize() {
        List<T> list = model.getObject();
        if (list == null) {
            return 0;
        }

        return list.size();
    }

    @NotNull
    public List<T> getSelectedObjects() {
        List<T> allSelected = new ArrayList<>();
        for (Serializable s : super.getAvailableData()) {
            if (s instanceof Selectable) {
                Selectable<SelectableBean<T>> selectable = (Selectable<SelectableBean<T>>) s;
                if (selectable.isSelected() && selectable.getValue() != null) {
                    allSelected.add(selectable.getValue().getValue());
                }
            }
        }

        return allSelected;
    }

//    @SuppressWarnings("unchecked")
//    protected <V extends Comparable<V>> void sort(List<T> list) {
//        Collections.sort(list, new Comparator<T>() {
//            @Override
//            public int compare(T o1, T o2) {
//                SortParam<String> sortParam = getSort();
//                String propertyName = sortParam.getProperty();
//                V prop1, prop2;
//                try {
//                    prop1 = (V) PropertyUtils.getProperty(o1, propertyName);
//                    prop2 = (V) PropertyUtils.getProperty(o2, propertyName);
//                } catch (RuntimeException|IllegalAccessException|InvocationTargetException|NoSuchMethodException e) {
//                    throw new SystemException("Couldn't sort the object list: " + e.getMessage(), e);
//                }
//                int comparison = ObjectUtils.compare(prop1, prop2, true);
//                return sortParam.isAscending() ? comparison : -comparison;
//            }
//        });
//    }

}
