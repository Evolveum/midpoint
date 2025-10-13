/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.assignment;

import com.evolveum.midpoint.repo.sqale.qmodel.ref.MReference;

import com.querydsl.core.types.dsl.BooleanExpression;

/**
 * Querydsl "row bean" type related to {@link QAssignmentReference}.
 */
public class MAssignmentReference extends MReference {

    public Long assignmentCid;
    public Long metadataCid;

    @Override
    public String toString() {
        return "MAssignmentReference{" +
                "ownerOid=" + ownerOid +
                ", assignmentCid=" + assignmentCid +
                ", referenceType=" + referenceType +
                ", targetOid=" + targetOid +
                ", targetType=" + targetType +
                ", relationId=" + relationId +
                ", metadataCid=" + metadataCid +
                '}';
    }

    interface Owner {

        /**
         * Returns condition for owning reference
         * @param ref
         * @return
         */
        BooleanExpression owns(QAssignmentReference ref);
    }
}
