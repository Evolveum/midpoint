/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.cases.workitem;

import com.evolveum.midpoint.repo.sqale.qmodel.ref.MReference;

/**
 * Querydsl "row bean" type related to {@link QCaseWorkItemReference}.
 */
public class MCaseWorkItemReference extends MReference {

    public Long workItemCid;

    @Override
    public String toString() {
        return "MCaseWorkItemReference{" +
                "ownerOid=" + ownerOid +
                ", workItemCid=" + workItemCid +
                ", referenceType=" + referenceType +
                ", targetOid=" + targetOid +
                ", targetType=" + targetType +
                ", relationId=" + relationId +
                '}';
    }
}
