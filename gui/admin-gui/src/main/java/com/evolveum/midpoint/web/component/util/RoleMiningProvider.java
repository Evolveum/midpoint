/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.util;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;

import com.evolveum.midpoint.gui.impl.component.data.provider.BaseSortableDataProvider;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.Validate;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class RoleMiningProvider<T extends Serializable> extends BaseSortableDataProvider<T> {

    private final IModel<List<T>> model;
    private final boolean sortable;

    public static final String F_NAME_ROLE = "roleObject";
    public static final String F_NAME_USER = "userObject";
    public static final String F_NAME_USER_TYPE = "userObjectType";
    public static final String F_NAME_ROLE_TYPE = "roleObjectType";

    public RoleMiningProvider(Component component, IModel<List<T>> model, boolean sortable) {
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
        SortParam<String> sortParam = getSort();
        String propertyName = sortParam.getProperty();
        if (F_NAME_USER.equals(propertyName)) {
            list.sort((o1, o2) -> {

                String prop3, prop4;
                try {
                    PrismObject<UserType> object1 = (PrismObject<UserType>) PropertyUtils.getProperty(o1, propertyName);
                    prop3 = String.valueOf(object1.getName());
                    PrismObject<UserType> object2 = (PrismObject<UserType>) PropertyUtils.getProperty(o2, propertyName);
                    prop4 = String.valueOf(object2.getName());

                } catch (RuntimeException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new SystemException("Couldn't sort the object list: " + e.getMessage(), e);
                }
                int comparison = ObjectUtils.compare(prop3, prop4, true);
                return sortParam.isAscending() ? comparison : -comparison;

            });
        } else if (F_NAME_USER_TYPE.equals(propertyName)) {
            list.sort((o1, o2) -> {

                String prop3, prop4;
                try {
                    UserType object1 = (UserType) PropertyUtils.getProperty(o1, propertyName);
                    prop3 = String.valueOf(object1.getName());
                    UserType object2 = (UserType) PropertyUtils.getProperty(o2, propertyName);
                    prop4 = String.valueOf(object2.getName());
                } catch (RuntimeException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new SystemException("Couldn't sort the object list: " + e.getMessage(), e);
                }
                int comparison = ObjectUtils.compare(prop3, prop4, true);
                return sortParam.isAscending() ? comparison : -comparison;

            });
        } else if (F_NAME_ROLE.equals(propertyName)) {
            list.sort((o1, o2) -> {
                String prop3, prop4;
                try {
                    PrismObject<RoleType> object1 = (PrismObject<RoleType>) PropertyUtils.getProperty(o1, propertyName);
                    prop3 = String.valueOf(object1.getName());
                    PrismObject<RoleType> object2 = (PrismObject<RoleType>) PropertyUtils.getProperty(o2, propertyName);
                    prop4 = String.valueOf(object2.getName());
                } catch (RuntimeException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new SystemException("Couldn't sort the object list: " + e.getMessage(), e);
                }
                int comparison = ObjectUtils.compare(prop3, prop4, true);
                return sortParam.isAscending() ? comparison : -comparison;

            });
        } else if (F_NAME_ROLE_TYPE.equals(propertyName)) {
            list.sort((o1, o2) -> {

                String prop3, prop4;
                try {
                    RoleType object1 = (RoleType) PropertyUtils.getProperty(o1, propertyName);
                    prop3 = String.valueOf(object1.getName());
                    RoleType object2 = (RoleType) PropertyUtils.getProperty(o2, propertyName);
                    prop4 = String.valueOf(object2.getName());
                } catch (RuntimeException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new SystemException("Couldn't sort the object list: " + e.getMessage(), e);
                }
                int comparison = ObjectUtils.compare(prop3, prop4, true);
                return sortParam.isAscending() ? comparison : -comparison;

            });
        } else {
            list.sort((o1, o2) -> {

                V prop1, prop2;
                try {

                    prop1 = (V) PropertyUtils.getProperty(o1, propertyName);
                    prop2 = (V) PropertyUtils.getProperty(o2, propertyName);

                } catch (RuntimeException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new SystemException("Couldn't sort the object list: " + e.getMessage(), e);
                }
                int comparison = ObjectUtils.compare(prop1, prop2, true);
                return sortParam.isAscending() ? comparison : -comparison;
            });
        }

    }

    protected int internalSize() {
        List<T> list = model.getObject();
        if (list == null) {
            return 0;
        }

        return list.size();
    }

}
