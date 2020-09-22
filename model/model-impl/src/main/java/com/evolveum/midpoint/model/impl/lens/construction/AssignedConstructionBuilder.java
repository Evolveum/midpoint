/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.construction;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

/**
 * TEMPORARY
 */
public class AssignedConstructionBuilder<AH extends AssignmentHolderType, EC extends EvaluatedResourceObjectConstructionImpl<AH>>
        extends ResourceObjectConstructionBuilder<AH, EC, AssignedConstructionBuilder<AH, EC>> {

    public AssignedConstructionBuilder() {
    }

    public ResourceObjectConstruction<AH, EC> build() {
        return new ResourceObjectConstruction<>(this);
    }
}
