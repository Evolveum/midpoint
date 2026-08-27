/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.prism.wrapper;

import com.evolveum.midpoint.model.api.context.AssignmentPath;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author skublik
 *
 */
public interface AssignmentValueWrapper extends PrismContainerValueWrapper<AssignmentType> {

    boolean isDirectAssignment();
    void setDirectAssignment(boolean isDirect);
    ObjectType getAssignmentParent();
    void setAssignmentParent(AssignmentPath assignmentPath);

    @Override
    PrismContainerValue<AssignmentType> getNewValue();
}
