package com.evolveum.midpoint.repo.sqale.qmodel.assignment;

import com.evolveum.midpoint.repo.sqale.qmodel.metadata.MValueMetadata;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import com.querydsl.core.types.dsl.BooleanExpression;

public class MAssignmentMetadata extends MValueMetadata implements MAssignmentReference.Owner {

    public MObjectType ownerType;
    public Long assignmentCid;

    @Override
    public BooleanExpression owns(QAssignmentReference ref) {
        return ref.ownerOid.eq(ownerOid)
                .and(ref.assignmentCid.eq(assignmentCid))
                .and(ref.metadataCid.eq(cid));
    }

}
