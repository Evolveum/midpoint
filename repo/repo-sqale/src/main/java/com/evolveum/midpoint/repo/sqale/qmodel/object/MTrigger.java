/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.object;

import java.time.Instant;

import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainer;

/**
 * Querydsl "row bean" type related to {@link QTrigger}.
 */
public class MTrigger extends MContainer {

    public Integer handlerUriId;
    public Instant timestampValue;
}
