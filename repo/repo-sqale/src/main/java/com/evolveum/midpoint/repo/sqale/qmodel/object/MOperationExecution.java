/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.object;

import java.time.Instant;
import java.util.UUID;

import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationExecutionRecordTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

/**
 * Querydsl "row bean" type related to {@link QOperationExecution}.
 */
public class MOperationExecution extends MContainer {

    public OperationResultStatusType status;
    public OperationExecutionRecordTypeType recordType;
    public UUID initiatorRefTargetOid;
    public MObjectType initiatorRefTargetType;
    public Integer initiatorRefRelationId;
    public UUID taskRefTargetOid;
    public MObjectType taskRefTargetType;
    public Integer taskRefRelationId;
    public Instant timestampValue;
}
