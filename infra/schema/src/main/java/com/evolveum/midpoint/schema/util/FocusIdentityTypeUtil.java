/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractFocusIdentitySourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusIdentityType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OwnFocusIdentitySourceType;

import org.jetbrains.annotations.NotNull;

@SuppressWarnings("WeakerAccess")
public class FocusIdentityTypeUtil {

    public static boolean isOwn(@NotNull FocusIdentityType identity) {
        return isOwn(identity.getSource());
    }

    private static boolean isOwn(AbstractFocusIdentitySourceType source) {
        return source == null || source instanceof OwnFocusIdentitySourceType;
    }

    static boolean isCompatible(@NotNull FocusIdentityType identity, @NotNull FocusIdentityType other) {
        return isCompatible(identity.getSource(), other.getSource());
    }

    private static boolean isCompatible(AbstractFocusIdentitySourceType source, AbstractFocusIdentitySourceType other) {
        boolean own = isOwn(source);
        boolean otherOwn = isOwn(other);
        if (own) {
            return otherOwn;
        } else if (otherOwn) {
            return false;
        } else {
            throw new UnsupportedOperationException("Cannot compare non-own sources yet");
        }
    }
}
