/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.common;

import java.util.UUID;

/**
 * Querydsl "row bean" type related to {@link QContainer}.
 */
public class MContainer {

    public UUID ownerOid;
    public Integer cid;
}
