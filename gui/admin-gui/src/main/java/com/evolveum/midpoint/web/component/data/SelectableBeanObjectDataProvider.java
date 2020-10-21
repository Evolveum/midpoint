/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.data;

import java.util.*;

import com.evolveum.midpoint.util.exception.*;

import org.apache.wicket.Component;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author lazyman
 * @author semancik
 */
public class SelectableBeanObjectDataProvider<O extends ObjectType> extends SelectableBeanContainerDataProvider<O> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(SelectableBeanObjectDataProvider.class);

    public SelectableBeanObjectDataProvider(Component component, Class<? extends O> type, Set<? extends O> selected) {
        super(component, type, selected, true);
    }

    public List<SelectableBean<O>> createDataObjectWrappers(Class<? extends O> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        List<PrismObject<? extends O>> list = (List) getModel().searchObjects(type, query, options, task, result);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Query {} resulted in {} objects", type.getSimpleName(), list.size());
        }

        List<SelectableBean<O>> data = new ArrayList<SelectableBean<O>>();
        for (PrismObject<? extends O> object : list) {
            data.add(createDataObjectWrapper(object.asObjectable()));
        }

        return data;
    }

    protected SelectableBean<O> getNewSelectableBean() {
        return new SelectableBeanImpl<>();
    }

    public SelectableBean<O> createDataObjectWrapper(O obj) {
        SelectableBean<O> selectable = new SelectableBeanImpl<>(obj);

        if (!WebComponentUtil.isSuccessOrHandledError(obj.getFetchResult())) {
            try {
                selectable.setResult(obj.getFetchResult());
            } catch (SchemaException e) {
                throw new SystemException(e.getMessage(), e);
            }
        }
        for (O s : getSelected()) {
            if (s.getOid().equals(obj.getOid())) {
                selectable.setSelected(true);
            }
        }

        return selectable;
    }

    @Override
    protected Integer countObjects(Class<? extends O> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> currentOptions, Task task, OperationResult result) throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        return getModel().countObjects(type, getQuery(), currentOptions, task, result);
    }
}
