/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

/**
 * @author mederly
 *
 */
public interface EvaluatedConstruction extends DebugDumpable {

    PrismObject<ResourceType> getResource();

    ShadowKindType getKind();

    String getIntent();

    boolean isDirectlyAssigned();

    AssignmentPath getAssignmentPath();

    boolean isWeak();
}
