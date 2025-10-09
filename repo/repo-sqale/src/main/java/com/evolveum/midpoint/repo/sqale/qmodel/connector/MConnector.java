/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.connector;

import java.util.UUID;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;

/**
 * Querydsl "row bean" type related to {@link QConnector}.
 */
public class MConnector extends MObject {

    public String connectorBundle;
    public String connectorType;
    public String connectorVersion;
    public Integer frameworkId;
    public UUID connectorHostRefTargetOid;
    public MObjectType connectorHostRefTargetType;
    public Integer connectorHostRefRelationId;
    public Integer[] targetSystemTypes;
    public Boolean available;

    public String displayNameOrig;
    public String displayNameNorm;
}
