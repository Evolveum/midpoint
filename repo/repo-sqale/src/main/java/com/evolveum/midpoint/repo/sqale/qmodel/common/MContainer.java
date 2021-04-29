/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.common;

import java.util.UUID;

import com.evolveum.midpoint.repo.sqale.SqaleUtils;

/**
 * Querydsl "row bean" type related to {@link QContainer}.
 */
public class MContainer {

    public UUID ownerOid;
    public Long cid;
    public MContainerType containerType;

    @Override
    public String toString() {
        return SqaleUtils.toString(this);
    }
}
