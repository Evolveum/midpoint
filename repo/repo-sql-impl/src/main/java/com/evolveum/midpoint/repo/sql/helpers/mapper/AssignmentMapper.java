/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        if (FocusType.F_ASSIGNMENT.equals(path.getFirstName())) {
            ass.setAssignmentOwner(RAssignmentOwner.FOCUS);
        } else {
            ass.setAssignmentOwner(RAssignmentOwner.ABSTRACT_ROLE);
        }

        RObject owner = (RObject) context.getOwner();

        try {
            RAssignment.copyFromJAXB(input, ass, owner, context.getRepositoryContext());
        } catch (DtoTranslationException ex) {
            throw new SystemException("Couldn't translate assignment to entity", ex);
        }

        return ass;
    }
}
