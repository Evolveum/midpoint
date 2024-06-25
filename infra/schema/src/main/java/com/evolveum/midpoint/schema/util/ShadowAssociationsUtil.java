/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import static com.evolveum.midpoint.prism.Referencable.getOid;
import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttributeValue;
import com.evolveum.midpoint.util.EqualsChecker;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationValueType;

/** Covers working with the "new" shadow associations (introduced in 4.9). */
public class ShadowAssociationsUtil {

    // TODO move to ShadowAssociationValue
    private static final EqualsChecker<ShadowReferenceAttributeValue> SHADOW_REF_BASED_PCV_EQUALS_CHECKER =
            (o1, o2) -> {
                if (o1 == null || o2 == null) {
                    return o1 == null && o2 == null;
                }

                var oid1 = o1.getShadowOid();
                var oid2 = o2.getShadowOid();

                // Normally, we compare association values by OID. This works for pre-existing target shadows.
                if (oid1 != null && oid2 != null) {
                    return oid1.equals(oid2);
                }

                // However, (some of) these shadows can be newly created. So we have to compare the attributes.
                // (Comparing the whole shadows can be problematic, as there may be lots of generated data, not marked
                // as operational.)
                var s1 = o1.getShadowIfPresent();
                var s2 = o2.getShadowIfPresent();

                if (s1 == null || s2 == null) {
                    return s1 == null && s2 == null; // Actually we cannot do any better here.
                }

                return s1.equalsByContent(s2);
            };

    public static @NotNull EqualsChecker<ShadowReferenceAttributeValue> shadowRefBasedPcvEqualsChecker() {
        return SHADOW_REF_BASED_PCV_EQUALS_CHECKER;
    }

    public static String getShadowOid(@NotNull ShadowReferenceAttributeValue refValue) {
        return getOid(refValue.asReferencable());
    }
}
