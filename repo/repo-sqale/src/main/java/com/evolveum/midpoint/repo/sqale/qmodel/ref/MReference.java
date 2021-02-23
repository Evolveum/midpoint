/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.ref;

import java.util.UUID;

/**
 * Querydsl "row bean" type related to {@link QReference} and its subtypes.
 */
public class MReference {

    public UUID ownerOid;
    public ReferenceType referenceType;
    public Integer relationId;
    public UUID targetOid;
    public Integer targetType;

    @Override
    public String toString() {
        return "MReference{" +
                "ownerOid=" + ownerOid +
                ", referenceType=" + referenceType +
                ", relationId=" + relationId +
                ", targetOid=" + targetOid +
                ", targetType=" + targetType +
                '}';
    }
}
