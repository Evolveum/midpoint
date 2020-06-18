/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.helpers.mapper;

import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.repo.sql.data.common.RObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignment;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignmentReference;
import com.evolveum.midpoint.repo.sql.data.common.other.RCReferenceType;
import com.evolveum.midpoint.repo.sql.helpers.modify.MapperContext;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.QNameUtil;
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
        RCReferenceType refType;
        if (QNameUtil.match(name, MetadataType.F_CREATE_APPROVER_REF)) {
            refType = RCReferenceType.CREATE_APPROVER;
        } else if (QNameUtil.match(name, MetadataType.F_MODIFY_APPROVER_REF)) {
            refType = RCReferenceType.MODIFY_APPROVER;
        } else {
            // TODO a warning here?
            // TODO what about CASE_REVIEWER type?
            refType = null;
        }

        RAssignmentReference ref = new RAssignmentReference();
        ref.setOwner(owner);
        ref.setReferenceType(refType);

        RObjectReference.copyFromJAXB(objectRef, ref, context.getRelationRegistry());

        return ref;
    }
}

