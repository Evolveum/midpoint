/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.node;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NodeOperationalStateType;

/**
 * Querydsl "row bean" type related to {@link QNode}.
 */
public class MNode extends MObject {

    public String nodeIdentifier;
    public NodeOperationalStateType operationalState;
}
