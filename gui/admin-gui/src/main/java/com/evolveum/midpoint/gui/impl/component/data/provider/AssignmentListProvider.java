/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.data.provider;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AssignmentListProvider extends MultivalueContainerListDataProvider<AssignmentType> {

    public AssignmentListProvider(Component component, @NotNull IModel<Search<AssignmentType>> search, IModel<List<PrismContainerValueWrapper<AssignmentType>>> model) {
        super(component, search, model, true);

        setSort(new SortParam("targetRef.targetName.orig", true));
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
