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
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;

import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

/**
 * @author lazyman
 */
public class SelectableListDataProvider<W extends Serializable, T extends Serializable>
        extends BaseSortableDataProvider<W> implements ISelectableDataProvider<T, W> {

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
                getAvailableData().add(createObjectWrapper(list.get(WebComponentUtil.safeLongToInteger(i))));
            }
        }

        return getAvailableData().iterator();
    }

    protected W createObjectWrapper(T object) {
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

    @Override
    public List<T> getSelectedRealObjects() {
        List<T> allSelected = new ArrayList<>();
        for (Serializable s : super.getAvailableData()) {
            if (s instanceof Selectable) {
                Selectable<T> selectable = (Selectable<T>) s;
                if (selectable.isSelected() && selectable.getValue() != null) {
                    allSelected.add(selectable.getValue());
                }
            }
        }
        return allSelected;
    }

    @Override
    public @NotNull List<W> getSelectedObjects() {
        List<W> allSelected = new ArrayList<>();
        for (Serializable s : super.getAvailableData()) {
            if (s instanceof Selectable) {
                Selectable selectable = (Selectable) s;
                if (selectable.isSelected()) {
                    allSelected.add((W)selectable);
                }
            }
        }
        return allSelected;
    }
}
