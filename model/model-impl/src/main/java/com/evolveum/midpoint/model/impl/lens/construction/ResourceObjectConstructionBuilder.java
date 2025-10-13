/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens.construction;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Builder for resource object constructions.
 */
public abstract class ResourceObjectConstructionBuilder<
        AH extends AssignmentHolderType,
        EC extends EvaluatedResourceObjectConstructionImpl<AH, ?>,
        RT extends ResourceObjectConstructionBuilder<AH, EC, RT>>
        extends AbstractConstructionBuilder<AH, ConstructionType, EC, RT> {

    ResolvedConstructionResource resolvedResource;

    ResourceObjectConstructionBuilder() {
    }
}
