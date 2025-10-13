/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.object;

import java.time.Instant;

import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainer;

/**
 * Querydsl "row bean" type related to {@link QTrigger}.
 */
public class MTrigger extends MContainer {

    public Integer handlerUriId;
    public Instant timestamp;
}
