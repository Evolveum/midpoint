/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
