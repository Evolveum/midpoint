/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.util;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.evolveum.midpoint.web.component.data.ISelectableDataProvider;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.Validate;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;

import org.apache.wicket.model.Model;

/**
 * @author lazyman
 */
public class SelectableListDataProvider<W extends Serializable, T extends Serializable>
        extends BaseSortableDataProvider<W> implements ISelectableDataProvider<W> {

    private final IModel<List<T>> model;

    public SelectableListDataProvider(Component Component, IModel<List<T>> model) {
        super(Component);

        Validate.notNull(model);
        this.model = model;
    }

    public SelectableListDataProvider(Component Component, boolean useCache, boolean useDefaultSortingField) {
        super(Component, useCache, useDefaultSortingField);
        this.model = Model.ofList(Collections.EMPTY_LIST);
    }

    protected IModel<List<T>> getModel() {
        return model;
    }

    @Override
    public Iterator<W> internalIterator(long first, long count) {

        getAvailableData().clear();

        List<T> list = getListFromModel();
        sort(list);

        if (list != null) {
            for (long i = first; i < first + count; i++) {
                if (i < 0 || i >= list.size()) {
                    throw new ArrayIndexOutOfBoundsException(
                            "Trying to get item on index " + i + " but list size is " + list.size());
                }
                getAvailableData().add(createObjectWrapper(list.get(WebComponentUtil.safeLongToInteger(i))));
            }
        }

        return getAvailableData().iterator();
    }

    protected List<T> getListFromModel() {
        return getModel().getObject();
    }

    //TODO clenaup, coppied from MultivalueCOntainerListDataProvider
    @SuppressWarnings("unchecked")
    protected <V extends Comparable<V>> void sort(List<T> list) {
        if (getSort() == null) {
            return;
        }

        list.sort((o1, o2) -> {
            SortParam<String> sortParam = getSort();
            String propertyName = sortParam.getProperty();
            V prop1 = getPropertyValue(o1, propertyName);
            V prop2 = getPropertyValue(o2, propertyName);

            int comparison = ObjectUtils.compare(prop1, prop2, true);
            return sortParam.isAscending() ? comparison : -comparison;
        });
    }

    private <V extends Comparable<V>> V getPropertyValue(T o1, String propertyName) {
        try {
            return (V) PropertyUtils.getProperty(o1, propertyName);
        } catch (RuntimeException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            return null;
        }
    }

    protected W createObjectWrapper(T object) {
        return (W) new SelectableBeanImpl<>(Model.of(object));
    }

    @Override
    protected int internalSize() {
        List<T> list = getListFromModel();
        if (list == null) {
            return 0;
        }

        return list.size();
    }

}
