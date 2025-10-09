/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.resource;

import java.util.UUID;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdministrativeAvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAdministrativeStateType;

/**
 * Querydsl "row bean" type related to {@link QResource}.
 */
public class MResource extends MObject {

    public ResourceAdministrativeStateType businessAdministrativeState;
    // administrativeOperationalState/administrativeAvailabilityStatus
    public AdministrativeAvailabilityStatusType administrativeOperationalStateAdministrativeAvailabilityStatus;
    // operationalState/lastAvailabilityStatus
    public AvailabilityStatusType operationalStateLastAvailabilityStatus;
    public UUID connectorRefTargetOid;
    public MObjectType connectorRefTargetType;
    public Integer connectorRefRelationId;
    public Boolean template;
    // column and item are both called "abstract" which is a Java keyword
    public Boolean abstractValue;

    public UUID superRefTargetOid;
    public MObjectType superRefTargetType;
    public Integer superRefRelationId;
}
