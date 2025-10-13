/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.focus;

import java.util.UUID;

import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainer;
import com.evolveum.midpoint.repo.sqale.qmodel.common.MContainerWithFullObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QOperationExecution;

/**
 * Querydsl "row bean" type related to {@link QOperationExecution}.
 */
public class MFocusIdentity extends MContainerWithFullObject {

    public UUID sourceResourceRefTargetOid; // target type and relation is implied/fixed
}
