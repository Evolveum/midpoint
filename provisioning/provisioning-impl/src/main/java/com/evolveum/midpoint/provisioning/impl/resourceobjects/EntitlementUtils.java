/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

class EntitlementUtils {

    private static final Trace LOGGER = TraceManager.getTrace(EntitlementUtils.class);

    /**
     * Creates a query that will select the entitlements for the subject.
     *
     * Entitlements point to subject using referencing ("association") attribute e.g. `ri:members`.
     * Subject is pointed to using referenced ("value") attribute, e.g. `ri:dn`.
     *
     * @param referencedAttrValue Value of the referenced ("value") attribute. E.g. uid=jack,ou=People,dc=example,dc=org.
     * @param referencingAttrDef Definition of the referencing ("association") attribute, e.g. "members"
     */
    static <TV,TA> ObjectQuery createEntitlementQuery(
            PrismPropertyValue<TV> referencedAttrValue, ResourceAttributeDefinition<TA> referencingAttrDef) {

        // The "referencedAttrValue" is what we look for in the entitlements (e.g. specific DN that should be their member).
        // We don't need the normalization, as the value is used for search on the resource.
        LOGGER.trace("Going to look for entitlements using value: {} def={}", referencedAttrValue, referencingAttrDef);
        ObjectQuery query = PrismContext.get().queryFor(ShadowType.class)
                .item(ItemPath.create(ShadowType.F_ATTRIBUTES, referencingAttrDef.getItemName()), referencingAttrDef)
                .eq(referencedAttrValue)
                .build();
        query.setAllowPartialResults(true);
        return query;
    }
}
