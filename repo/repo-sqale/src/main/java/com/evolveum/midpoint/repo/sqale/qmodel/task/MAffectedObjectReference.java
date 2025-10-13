/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.task;

import com.evolveum.midpoint.repo.sqale.qmodel.ref.MReference;

import com.querydsl.core.types.dsl.BooleanExpression;

/**
 * Querydsl "row bean" type related to {@link QAffectedObjectReference}.
 */
public class MAffectedObjectReference extends MReference {

    public Long affectedObjectCid;

    @Override
    public String toString() {
        return "MAssignmentReference{" +
                "ownerOid=" + ownerOid +
                ", affectedObjectCid=" + affectedObjectCid +
                ", referenceType=" + referenceType +
                ", targetOid=" + targetOid +
                ", targetType=" + targetType +
                ", relationId=" + relationId +
                '}';
    }
}
