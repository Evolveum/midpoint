/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.lookuptable;

import java.time.Instant;
import java.util.UUID;

/**
 * Querydsl "row bean" type related to {@link QLookupTableRow}.
 */
public class MLookupTableRow {

    public UUID ownerOid;
    public int cid;
    public String rowKey;
    public String labelNorm;
    public String labelOrig;
    public String rowValue;
    public Instant lastChangeTimestamp;
}
