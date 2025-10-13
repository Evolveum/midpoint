/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.selector.eval;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.selector.spec.OwnerClause;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Resolves the owner referenced to by {@link OwnerClause}.
 */
public interface ObjectResolver {

    /** TODO */
    PrismObject<? extends ObjectType> resolveReference(
            ObjectReferenceType ref, Object context, String referenceName);
}
