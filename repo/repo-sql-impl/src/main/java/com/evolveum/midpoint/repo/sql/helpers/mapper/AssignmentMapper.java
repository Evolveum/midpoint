/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers.mapper;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignment;
import com.evolveum.midpoint.repo.sql.data.common.other.RAssignmentOwner;
import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class AssignmentMapper extends ContainerMapper<AssignmentType, RAssignment> {

    @Override
    public RAssignment map(AssignmentType input, MapperContext context) {
        RAssignment ass = new RAssignment();

        ItemDelta delta = context.getDelta();
        ItemPath path = delta.getPath().namedSegmentsOnly();
        if (path.startsWithName(FocusType.F_ASSIGNMENT)) {
            ass.setAssignmentOwner(RAssignmentOwner.FOCUS);
        } else {
            ass.setAssignmentOwner(RAssignmentOwner.ABSTRACT_ROLE);
        }

        RObject owner = (RObject) context.getOwner();

        try {
            RAssignment.fromJaxb(input, ass, owner, context.getRepositoryContext());
        } catch (DtoTranslationException ex) {
            throw new SystemException("Couldn't translate assignment to entity", ex);
        }

        return ass;
    }
}
