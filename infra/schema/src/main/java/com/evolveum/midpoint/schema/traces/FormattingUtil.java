/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowDiscriminatorType;

import org.apache.commons.lang3.BooleanUtils;

public class FormattingUtil {

    public static String getResourceName(ShadowDiscriminatorType discriminator) {
        if (discriminator == null || discriminator.getResourceRef() == null) {
            return null;
        }
        ObjectReferenceType resourceRef = discriminator.getResourceRef();
        if (resourceRef.getTargetName() != null) {
            return resourceRef.getTargetName().getOrig();
        } else {
            return resourceRef.getOid();
        }
    }

    public static String getDiscriminatorDescription(ShadowDiscriminatorType discriminator) {
        if (discriminator == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(getResourceName(discriminator))
                .append(", ").append(String.valueOf(discriminator.getKind()).toLowerCase())
                .append(", ").append(discriminator.getIntent());
        if (discriminator.getTag() != null) {
            sb.append(", ").append(discriminator.getTag());
        }
        if (BooleanUtils.isTrue(discriminator.isTombstone())) {
            sb.append(", tombstone");
        }
        return sb.toString();
    }
}
