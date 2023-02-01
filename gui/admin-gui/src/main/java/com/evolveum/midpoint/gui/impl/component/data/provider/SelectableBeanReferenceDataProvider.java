/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.data.provider;

import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.TypedCacheKey;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;

import static com.evolveum.midpoint.schema.DefinitionProcessingOption.FULL;
import static com.evolveum.midpoint.schema.DefinitionProcessingOption.ONLY_IF_EXISTS;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

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
