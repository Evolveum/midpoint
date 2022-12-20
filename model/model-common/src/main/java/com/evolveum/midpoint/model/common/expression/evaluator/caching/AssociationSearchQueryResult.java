/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.expression.evaluator.caching;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.apache.commons.lang3.Validate;

import java.util.List;

public class AssociationSearchQueryResult extends QueryResult<PrismContainerValue<ShadowAssociationType>> {

    private final String resourceOid;
    private final ShadowKindType kind;

    AssociationSearchQueryResult(
            List<PrismContainerValue<ShadowAssociationType>> resultingList, List<PrismObject<ShadowType>> rawResultsList) {
        super(resultingList);

        Validate.isTrue(rawResultsList != null && !rawResultsList.isEmpty());
        ShadowType shadow = rawResultsList.get(0).asObjectable();

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
