/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.data.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.model.SelectableObjectModel;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
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

    private boolean isMemberPanel = false;

    public SelectableBeanObjectDataProvider(Component component, IModel<Search<O>> search, Set<O> selected) {
        super(component, search, selected, true);
    }

    public SelectableBeanObjectDataProvider(Component component, Set<O> selected) {
        super(component, Model.of(), selected, true);
    }

    public List<SelectableBean<O>> createDataObjectWrappers(Class<? extends O> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        List<PrismObject<? extends O>> list = (List) getModelService().searchObjects(type, query, options, task, result);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Query {} resulted in {} objects", type.getSimpleName(), list.size());
        }

        List<SelectableBean<O>> data = new ArrayList<>();
        for (PrismObject<? extends O> object : list) {
            data.add(createDataObjectWrapper(object.asObjectable()));
        }

        return data;
    }

    public SelectableBean<O> createDataObjectWrapper(O obj) {
        SelectableObjectModel<O> model = new SelectableObjectModel<O>(obj, getOptions()) {
            @Override
            protected O load() {
                PageBase pageBase = getPageBase();
                Task task = pageBase.createSimpleTask("load object");
                OperationResult result = task.getResult();
                PrismObject<O> object = WebModelServiceUtils.loadObject(getType(), getOid(), getOptions(), pageBase, task, result);
                result.computeStatusIfUnknown();
                return object.asObjectable();
            }
        };
        SelectableBean<O> selectable = new SelectableBeanImpl<>(model);
        //TODO result

        for (O s : getSelected()) {
            if (s.getOid().equals(obj.getOid())) {
                selectable.setSelected(true);
                model.setSelected(true);
            }
        }

        return selectable;
    }

    @Override
    protected Integer countObjects(Class<? extends O> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> currentOptions, Task task, OperationResult result) throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        return getModelService().countObjects(type, getQuery(), currentOptions, task, result);
    }

    @Override
    public boolean isOrderingDisabled() {
        CompiledObjectCollectionView guiObjectListViewType = getCompiledObjectCollectionView();
        if (guiObjectListViewType != null) {
            if (isMemberPanel()) {
                if (guiObjectListViewType.getAdditionalPanels() != null &&
                        guiObjectListViewType.getAdditionalPanels().getMemberPanel() != null &&
                        guiObjectListViewType.getAdditionalPanels().getMemberPanel().isDisableSorting() != null) {
                    return guiObjectListViewType.getAdditionalPanels().getMemberPanel().isDisableSorting();
                }
            } else {
                if (guiObjectListViewType.isDisableSorting() != null) {
                    return guiObjectListViewType.isDisableSorting();
                }
            }
        }
        return false;
    }

    public boolean isUseObjectCounting() {
        CompiledObjectCollectionView guiObjectListViewType = getCompiledObjectCollectionView();
        if (guiObjectListViewType != null) {
            if (isMemberPanel()) {
                if (guiObjectListViewType.getAdditionalPanels() != null &&
                        guiObjectListViewType.getAdditionalPanels().getMemberPanel() != null &&
                        guiObjectListViewType.getAdditionalPanels().getMemberPanel().isDisableCounting() != null) {
                    return !guiObjectListViewType.getAdditionalPanels().getMemberPanel().isDisableCounting();
                }
            } else {
                if (guiObjectListViewType.isDisableCounting() != null) {
                    return !guiObjectListViewType.isDisableCounting();
                }
            }
        }
        return true;
    }

    @Override
    public IModel<SelectableBean<O>> model(SelectableBean<O> object) {
        return new Model<>(object);
//        return new SelectableObjectModel<>(object) {
//
//            @Override
//            protected PageBase getPageBase() {
//                return SelectableBeanObjectDataProvider.this.getPageBase();
//            }
//        };
    }

    protected boolean isMemberPanel() {
        return isMemberPanel;
    }

    public void setIsMemberPanel(boolean isMemberPanel) {
        this.isMemberPanel = isMemberPanel;
    }

    @Override
    protected List<O> searchObjects(Class<? extends O> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result) throws CommonException {
        return getModelService().searchObjects(type, query, options, task, result).map(prismObject -> prismObject.asObjectable());
    }

    @Override
    public void detach() {
        super.detach();
        getAvailableData().clear();
    }
}
