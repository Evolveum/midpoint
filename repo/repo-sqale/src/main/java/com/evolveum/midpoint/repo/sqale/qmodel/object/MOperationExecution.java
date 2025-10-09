/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.object;

import java.time.Instant;
import java.util.UUID;

import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainerWithFullObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationExecutionRecordTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

/**
 * Querydsl "row bean" type related to {@link QOperationExecution}.
 */
public class MOperationExecution extends MContainerWithFullObject {

    public OperationResultStatusType status;
    public OperationExecutionRecordTypeType recordType;
    public UUID initiatorRefTargetOid;
    public MObjectType initiatorRefTargetType;
    public Integer initiatorRefRelationId;
    public UUID taskRefTargetOid;
    public MObjectType taskRefTargetType;
    public Integer taskRefRelationId;
    public Instant timestamp;

}
