/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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

    public static boolean referencesOidEqual(ObjectReferenceType ref1, ObjectReferenceType ref2) {
        if (ref1 == null || ref2 == null) {
            return false;
        }
        if (ref1.getOid() == null || ref2.getOid() == null) {
            return false;
        }
        return ref1.getOid().equals(ref2.getOid());
    }
}
