/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.util;

import com.evolveum.midpoint.gui.api.factory.wrapper.PrismContainerWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.impl.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OwnedByFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class RepoAssignmentListProvider extends ContainerListDataProvider<AssignmentType> {


    private static final Trace LOGGER = TraceManager.getTrace(ContainerListDataProvider.class);
    private static final String DOT_CLASS = ContainerListDataProvider.class.getName() + ".";
    private static final String OPERATION_SEARCH_CONTAINERS = DOT_CLASS + "searchContainers";
    private static final String OPERATION_COUNT_CONTAINERS = DOT_CLASS + "countContainers";
    
    
    private final IModel<List<PrismContainerValueWrapper<AssignmentType>>> model;
    private final String oid;
    private final Class<? extends Objectable> objectType;
    private final ItemPath path;
    
    public RepoAssignmentListProvider(Component component, @NotNull IModel<Search<AssignmentType>> search, IModel<List<PrismContainerValueWrapper<AssignmentType>>> model, 
                Class<? extends Objectable>  objectType, String oid, ItemPath path) {
        super(component, search);
        this.model = model;
        this.oid = oid;
        this.objectType = objectType;
        this.path = path;
        //setSort(new SortParam("targetRef.targetName.orig", true));
    }

    @Override
    public Class<AssignmentType> getType() {
        return AssignmentType.class;
    }

    protected List<PrismContainerValueWrapper<AssignmentType>> postFilter(List<PrismContainerValueWrapper<AssignmentType>> assignmentList) {
        return assignmentList;
    }


    @Override
    protected PrismContainerValueWrapper<AssignmentType> createWrapper(AssignmentType object, Task task,
            OperationResult result) throws SchemaException {
        for (PrismContainerValueWrapper<AssignmentType> item : model.getObject()) {
            if (Objects.equals(item.getRealValue().getId(),object.getId())) {
                postProcessWrapper(item);
                return item;
            }
        }
        // FIXME: Should not happen? Could possibly, if user was modified in other session
        // or role has assignments and inducements and ownedBy filter is not supported
        // Lets silently skip the item for now (by returning null)
        return null;
    }
    
    protected void postProcessWrapper(PrismContainerValueWrapper<AssignmentType> valueWrapper) {
        AssignmentType assignmentType = valueWrapper.getRealValue();
        if (assignmentType == null) {
            return;
        }
        ObjectReferenceType targetRef = assignmentType.getTargetRef();
        if (targetRef == null || targetRef.getOid() == null || targetRef.getObject() != null) {
            return;
        }

        PrismObject<? extends ObjectType> object = WebModelServiceUtils.loadObject(targetRef, getPageBase());
        targetRef.asReferenceValue().setObject(object);
    }
    
    // We need to add oid selector to the query
    @Override
    public ObjectQuery getQuery() {
        var orig  = super.getQuery();
        ObjectFilter filter = orig != null ? orig.getFilter() : null;
        if (orig != null) {
            filter = orig.getFilter();
        }
        if (filter != null) {
            return QueryBuilder.queryFor(AssignmentType.class, getPrismContext())
                .filter(filter)
                .and()
                    .ownedBy(objectType, path)
                    .id(oid)
                .build();
        }
        return QueryBuilder.queryFor(AssignmentType.class, getPrismContext())
                    .ownedBy(objectType, path)
                    .id(oid)
                .build();
    }

    @Override
    public void detach() {
        for (PrismContainerValueWrapper<AssignmentType> assignment : getAvailableData()) {
            AssignmentType assignmentType = assignment.getRealValue();
            if (assignmentType == null) {
                continue;
            }
            ObjectReferenceType ref = assignmentType.getTargetRef();
            if (ref == null) {
                continue;
            }
            if (ref.getObject() != null) {
                ref.asReferenceValue().setObject(null);
            }
        }
    }
}
