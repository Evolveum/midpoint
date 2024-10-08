/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.application;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;


import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;

import java.util.List;

public class AssignmentCounter<AH extends AssignmentHolderType> extends SimpleCounter<AssignmentHolderDetailsModel<AH>, AH> {

    public AssignmentCounter() {
        super();
    }

    @Override
    public int count(AssignmentHolderDetailsModel<AH> objectDetailsModels, PageBase pageBase) {
        PrismObjectWrapper<AH> assignmentHolderWrapper = objectDetailsModels.getObjectWrapperModel().getObject();
        PrismContainerWrapper<AssignmentType> assignmentContainerWrapper = null;
        try {
            assignmentContainerWrapper = assignmentHolderWrapper.findContainer(AssignmentHolderType.F_ASSIGNMENT);
        } catch (SchemaException e) {
            return 0;
        }
        if (assignmentContainerWrapper == null) {
            return 0;
        }
        List<PrismContainerValueWrapper<AssignmentType>> values = assignmentContainerWrapper.getValues();

        int count = 0;
        for (PrismContainerValueWrapper<AssignmentType> assignmentVW : values) {
            if (isNewlyAddedAssignment(assignmentVW)) {
                continue;
            }
            AssignmentType assignment = assignmentVW.getRealValue();
            if (WebComponentUtil.isArchetypeAssignment(assignment) || WebComponentUtil.isDelegationAssignment(assignment)) {
                continue;
            }
            count++;
        }
        return count;
    }

    //check if the assignment was newly added
    //if true, then it is not counted
    private boolean isNewlyAddedAssignment(PrismContainerValueWrapper<AssignmentType> assignmentVW) {
        return ValueStatus.ADDED.equals(assignmentVW.getStatus());
    }
}
