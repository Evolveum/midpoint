/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.construction;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Builder for resource object constructions.
 */
public abstract class ResourceObjectConstructionBuilder<AH extends AssignmentHolderType, EC extends EvaluatedResourceObjectConstructionImpl<AH, ?>, RT extends ResourceObjectConstructionBuilder<AH, EC, RT>>
        extends AbstractConstructionBuilder<AH, ConstructionType, EC, RT> {

    ResolvedConstructionResource resolvedResource;

    public ResourceObjectConstructionBuilder() {
    }

    public RT resolvedResource(ResolvedConstructionResource val) {
        resolvedResource = val;
        return typedThis();
    }

    public abstract ResourceObjectConstruction<AH, EC> build();

    private RT typedThis() {
        //noinspection unchecked
        return (RT) this;
    }
}
