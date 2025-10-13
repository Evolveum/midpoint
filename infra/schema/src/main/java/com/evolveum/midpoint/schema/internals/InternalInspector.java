/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.internals;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public interface InternalInspector {

    <O extends ObjectType> void inspectRepositoryRead(Class<O> type, String oid);

    <F extends AssignmentHolderType> void inspectRoleEvaluation(F target, boolean fullEvaluation);
}
