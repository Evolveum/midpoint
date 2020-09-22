/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.construction;

import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

/**
 * TEMPORARY
 */
public class OutboundConstructionBuilder<AH extends AssignmentHolderType>
        extends ResourceObjectConstructionBuilder<AH, EvaluatedOutboundConstructionImpl<AH>, OutboundConstructionBuilder<AH>> {

    LensProjectionContext projectionContext;

    public OutboundConstructionBuilder() {
    }

    public OutboundConstructionBuilder<AH> projectionContext(LensProjectionContext val) {
        projectionContext = val;
        return this;
    }

    public OutboundConstruction<AH> build() {
        return new OutboundConstruction<>(this);
    }
}
