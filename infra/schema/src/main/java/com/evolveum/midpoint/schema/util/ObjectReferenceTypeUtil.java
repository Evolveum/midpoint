/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class ObjectReferenceTypeUtil {

    public static String getTargetNameOrOid(ObjectReferenceType ref) {
        if (ref == null) {
            return null;
        }
        PolyStringType targetName = ref.getTargetName();
        if (targetName != null) {
            String orig = targetName.getOrig();
            if (orig != null) {
                return orig;
            }
        }
        return ref.getOid();
    }
}
