/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.prism.wrapper;

import com.evolveum.midpoint.model.api.context.AssignmentPath;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.web.page.admin.users.component.AssignmentInfoDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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
