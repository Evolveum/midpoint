/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.task;

import java.util.UUID;

import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

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

}
