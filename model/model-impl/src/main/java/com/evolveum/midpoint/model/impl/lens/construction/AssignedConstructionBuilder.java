/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens.construction;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

@Experimental
public class AssignedConstructionBuilder<AH extends AssignmentHolderType>
        extends ResourceObjectConstructionBuilder<AH, EvaluatedAssignedResourceObjectConstructionImpl<AH>, AssignedConstructionBuilder<AH>> {

    public AssignedConstructionBuilder() {
    }

    public AssignedResourceObjectConstruction<AH> build() {
        return new AssignedResourceObjectConstruction<>(this);
    }
}
