/*
 * Copyright (c) 2010-2019 Evolveum
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

import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.repo.sql.data.common.RObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignment;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignmentReference;
import com.evolveum.midpoint.repo.sql.data.common.other.RCReferenceOwner;
import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import javax.xml.namespace.QName;

/**
 * Created by Viliam Repan (lazyman).
 */
public class AssignmentReferenceMapper extends ReferenceMapper<RAssignmentReference> {

    @Override
    public RAssignmentReference map(Referencable input, MapperContext context) {
        ObjectReferenceType objectRef = buildReference(input);

        ObjectTypeUtil.normalizeRelation(objectRef, context.getRelationRegistry());

        RAssignment owner = (RAssignment) context.getOwner();

        QName name = context.getDelta().getPath().lastName().asSingleName();
        RCReferenceOwner refType = null;
        if (MetadataType.F_CREATE_APPROVER_REF.equals(name)) {
            refType = RCReferenceOwner.CREATE_APPROVER;
        } else if (MetadataType.F_MODIFY_APPROVER_REF.equals(name)) {
            refType = RCReferenceOwner.MODIFY_APPROVER;
        }

        RAssignmentReference ref = new RAssignmentReference();
        ref.setOwner(owner);
        ref.setReferenceType(refType);

        RObjectReference.copyFromJAXB(objectRef, ref, context.getRelationRegistry());

        return ref;
    }
}

