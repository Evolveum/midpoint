/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.task;

import java.util.UUID;

import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExecutionModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PredefinedConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import com.querydsl.core.types.dsl.BooleanExpression;

public class MAffectedObjects extends MContainer {

    public Integer activityId;

    // Object container
    public MObjectType type;
    public UUID archetypeRefTargetOid;
    public MObjectType archetypeRefTargetType;
    public Integer archetypeRefRelationId;

    // ResourceObject container
    public Integer objectClassId;
    public UUID resourceRefTargetOid;
    public MObjectType resourceRefTargetType;
    public Integer resourceRefRelationId;
    public String intent;
    public String tag;
    public ShadowKindType kind;

    public ExecutionModeType executionMode;
    public PredefinedConfigurationType predefinedConfigurationToUse;
}
