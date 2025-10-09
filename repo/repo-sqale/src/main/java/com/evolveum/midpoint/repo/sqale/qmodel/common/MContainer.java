/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
