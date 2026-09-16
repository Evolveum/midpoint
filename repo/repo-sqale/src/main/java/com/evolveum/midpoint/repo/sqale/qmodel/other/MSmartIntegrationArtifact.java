/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.other;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.role.MAbstractRole;
import com.evolveum.midpoint.repo.sqale.qmodel.role.QApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import java.util.UUID;

/**
 * Querydsl "row bean" type related to {@link QSmartIntegrationArtifact}.
 */
public class MSmartIntegrationArtifact extends MObject {

    public UUID resourceRefTargetOid;
    public MObjectType resourceRefTargetType;
    public Integer resourceRefRelationId;
    public Integer objectClassId;
    public ShadowKindType kind;
    public String intent;
    public Integer focusTypeId;
}
