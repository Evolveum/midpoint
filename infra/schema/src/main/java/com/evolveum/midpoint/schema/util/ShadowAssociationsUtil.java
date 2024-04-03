/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.util.EqualsChecker;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationValueType;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.prism.Referencable.getOid;
import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

/** Covers working with the "new" shadow associations (introduced in 4.9). */
public class ShadowAssociationsUtil {

    private static final EqualsChecker<PrismContainerValue<ShadowAssociationValueType>> SHADOW_REF_BASED_PCV_EQUALS_CHECKER =
            (o1, o2) -> {
                if (o1 == null || o2 == null) {
                    return o1 == null && o2 == null;
                }
                // We do not want to compare references in details. Comparing OIDs suffices.
                // Otherwise we get into problems, as one of the references might be e.g. without identifiers.
                // Later, we should extend this by checking e.g. the parameters.
                return getShadowOidRequired(o1)
                        .equals(getShadowOidRequired(o2));
            };

    public static @NotNull EqualsChecker<PrismContainerValue<ShadowAssociationValueType>> shadowRefBasedPcvEqualsChecker() {
        return SHADOW_REF_BASED_PCV_EQUALS_CHECKER;
    }

    public static @NotNull String getShadowOidRequired(@NotNull PrismContainerValue<ShadowAssociationValueType> pcv) {
        return stateNonNull(getShadowOid(pcv), "No shadow OID in association: %s", pcv);
    }

    private static String getShadowOid(@NotNull PrismContainerValue<ShadowAssociationValueType> pcv) {
        return getOid(
                pcv.asContainerable().getShadowRef());
    }
}
