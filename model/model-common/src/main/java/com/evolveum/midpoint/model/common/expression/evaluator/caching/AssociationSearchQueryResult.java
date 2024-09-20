/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.expression.evaluator.caching;

import java.util.Collection;

import org.apache.commons.lang3.Validate;

import com.evolveum.midpoint.model.common.expression.evaluator.AbstractSearchExpressionEvaluator.ObjectFound;
import com.evolveum.midpoint.schema.processor.ShadowAssociationValue;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

public class AssociationSearchQueryResult extends QueryResult<ShadowAssociationValue> {

    private final String resourceOid;
    private final ShadowKindType kind;

    AssociationSearchQueryResult(
            Collection<? extends ObjectFound<ShadowType, ShadowAssociationValue>> objectsFound) {
        super(objectsFound);

        // TODO this is quite suspicious ... but it's here for ages, let us keep it for now

        Validate.isTrue(objectsFound != null && !objectsFound.isEmpty());
        ShadowType shadow = objectsFound.iterator().next().sourceObject().asObjectable();

        resourceOid = ShadowUtil.getResourceOid(shadow);
        kind = shadow.getKind();
    }

    public String getResourceOid() {
        return resourceOid;
    }

    public ShadowKindType getKind() {
        return kind;
    }
}
