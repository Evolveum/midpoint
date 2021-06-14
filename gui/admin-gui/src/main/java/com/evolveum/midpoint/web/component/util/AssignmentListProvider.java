/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.util;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AssignmentListProvider extends MultivalueContainerListDataProvider<AssignmentType> {

    public AssignmentListProvider(Component component, @NotNull IModel<Search<AssignmentType>> search, IModel<List<PrismContainerValueWrapper<AssignmentType>>> model) {
        super(component, search, model);
    }

    @Override
    protected List<PrismContainerValueWrapper<AssignmentType>> searchThroughList() {
        List<PrismContainerValueWrapper<AssignmentType>> filteredList = super.searchThroughList();
        return postFilter(filteredList);
    }

    protected List<PrismContainerValueWrapper<AssignmentType>> postFilter(List<PrismContainerValueWrapper<AssignmentType>> assignmentList) {
        return assignmentList;
    }

    @Override
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
}
