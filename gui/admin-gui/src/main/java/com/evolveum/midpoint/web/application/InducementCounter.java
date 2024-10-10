/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.application;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import java.util.List;

public class InducementCounter<AR extends AbstractRoleType> extends SimpleCounter<ObjectDetailsModels<AR>, AR> {

    public InducementCounter() {
        super();
    }

    @Override
    public int count(ObjectDetailsModels<AR> objectDetailsModels, PageBase pageBase) {
        PrismObjectWrapper<AR> abstractRole = objectDetailsModels.getObjectWrapperModel().getObject();
        PrismContainerWrapper<AssignmentType> inducementContainerWrapper = null;
        try {
            inducementContainerWrapper = abstractRole.findContainer(AbstractRoleType.F_INDUCEMENT);
        } catch (SchemaException e) {
            return 0;
        }
        if (inducementContainerWrapper == null) {
            return 0;
        }
        List<PrismContainerValueWrapper<AssignmentType>> values = inducementContainerWrapper.getValues();

        int count = 0;
        for (PrismContainerValueWrapper<AssignmentType> inducementVW : values) {
            if (isNewlyAddedInducement(inducementVW)) {
                continue;
            }
            AssignmentType inducement = inducementVW.getRealValue();
            if (WebComponentUtil.isArchetypeAssignment(inducement) || WebComponentUtil.isDelegationAssignment(inducement)) {
                continue;
            }
            count++;
        }
        return count;
    }

    //check if the assignment was newly added
    //if true, then it is not counted
    private boolean isNewlyAddedInducement(PrismContainerValueWrapper<AssignmentType> inducementVW) {
        return ValueStatus.ADDED.equals(inducementVW.getStatus());
    }
}
