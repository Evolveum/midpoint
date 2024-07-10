/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
