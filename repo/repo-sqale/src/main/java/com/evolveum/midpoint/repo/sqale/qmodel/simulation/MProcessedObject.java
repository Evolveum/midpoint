/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.simulation;

import java.util.UUID;

import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectProcessingStateType;

public class MProcessedObject extends MContainer {

    public UUID oid;
    public MObjectType objectType;
    public String nameOrig;
    public String nameNorm;

    public ObjectProcessingStateType state;
    public String[] metricIdentifiers;

    public byte[] fullObject;

    public byte[] objectBefore;
    public byte[] objectAfter;

    public String transactionId;

    public Long focusRecordId;

}
