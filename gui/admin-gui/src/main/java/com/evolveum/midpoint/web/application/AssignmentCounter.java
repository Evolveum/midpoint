/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.application;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.prism.PrismObject;

import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;

import java.util.List;

public class AssignmentCounter extends SimpleCounter {

    public AssignmentCounter(ObjectDetailsModels objectDetailsModels) {
        super(objectDetailsModels);
    }

    public int count() {
        PrismObjectWrapper<?> modelObject = (PrismObjectWrapper<?>) getObjectDetailsModels().getObjectWrapperModel().getObject();
        AssignmentHolderType object = (AssignmentHolderType) modelObject.getObject().asObjectable();

        List<AssignmentType> assignments = object.getAssignment();
        int count = 0;
        for (AssignmentType assignment : assignments) {
            if (WebComponentUtil.isArchetypeAssignment(assignment)) {
                continue;
            }
            count++;
        }
        return count;
    }
}
