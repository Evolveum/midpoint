/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.focus;

import java.util.UUID;

import com.evolveum.midpoint.repo.sqale.jsonb.Jsonb;
import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QOperationExecution;

/**
 * Querydsl "row bean" type related to {@link QOperationExecution}.
 */
public class MFocusIdentity extends MContainer {

    public byte[] fullSource; // serialized <source> element
    public UUID sourceResourceRefTargetOid; // target type and relation is implied/fixed

    // extension/attribute-like storage
    public Jsonb itemsOriginal;
    public Jsonb itemsNormalized;
}
