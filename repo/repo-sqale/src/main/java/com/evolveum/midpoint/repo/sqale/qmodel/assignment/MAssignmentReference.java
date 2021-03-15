/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.assignment;

import com.evolveum.midpoint.repo.sqale.qmodel.ref.MReference;

/**
 * Querydsl "row bean" type related to {@link QAssignmentReference}.
 */
public class MAssignmentReference extends MReference {

    public Long assignmentCid;

    @Override
    public String toString() {
        return "MAssignmentReference{" +
                "ownerOid=" + ownerOid +
                ", assignmentCid=" + assignmentCid +
                ", referenceType=" + referenceType +
                ", targetOid=" + targetOid +
                ", targetType=" + targetType +
                ", relationId=" + relationId +
                '}';
    }
}
