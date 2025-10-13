/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens.construction;

import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

@Experimental
public class PlainResourceObjectConstructionBuilder<AH extends AssignmentHolderType>
        extends ResourceObjectConstructionBuilder<
            AH,
            EvaluatedPlainResourceObjectConstructionImpl<AH>,
            PlainResourceObjectConstructionBuilder<AH>> {

    LensProjectionContext projectionContext;

    public PlainResourceObjectConstructionBuilder() {
    }

    public PlainResourceObjectConstructionBuilder<AH> projectionContext(LensProjectionContext val) {
        projectionContext = val;
        return this;
    }

    public PlainResourceObjectConstruction<AH> build() {
        return new PlainResourceObjectConstruction<>(this);
    }
}
