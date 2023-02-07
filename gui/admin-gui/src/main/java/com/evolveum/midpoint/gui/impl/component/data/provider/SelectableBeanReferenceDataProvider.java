/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.data.provider;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * @author lazyman
 * @author semancik
 */
public class SelectableBeanReferenceDataProvider extends SelectableBeanDataProvider<ObjectReferenceType> {
    private static final long serialVersionUID = 1L;
    public SelectableBeanReferenceDataProvider(Component component, @NotNull IModel<Search<ObjectReferenceType>> search, Set<ObjectReferenceType> selected, boolean useDefaultSortingField) {
        super(component, search, selected, useDefaultSortingField);
    }


    @Override
    protected boolean checkOrderingSettings() {
        return true;
    }

    @Override
    protected List<ObjectReferenceType> searchObjects(Class<ObjectReferenceType> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result) throws CommonException {
        return getModelService().searchReferences(query, options,task, result);
    }

    public SelectableBean<ObjectReferenceType> createDataObjectWrapper(ObjectReferenceType obj) {
        return new SelectableBeanImpl<>(Model.of(obj));
    }

    @Override
    protected Integer countObjects(Class<ObjectReferenceType> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> currentOptions, Task task, OperationResult result) throws CommonException {
        return getModelService().countReferences(query, currentOptions, task, result);
    }

    @Override
    protected boolean match(ObjectReferenceType selectedValue, ObjectReferenceType foundValue) {
        return selectedValue.equals(foundValue);
    }
}
