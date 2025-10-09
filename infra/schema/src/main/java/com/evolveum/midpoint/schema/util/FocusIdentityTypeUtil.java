/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusIdentitySourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusIdentityType;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static com.evolveum.midpoint.prism.Referencable.getOid;

@SuppressWarnings("WeakerAccess")
public class FocusIdentityTypeUtil {

    public static boolean isOwn(@NotNull FocusIdentityType identity) {
        return isOwn(identity.getSource());
    }

    private static boolean isOwn(FocusIdentitySourceType source) {
        // This will be changed after (if?) some well-known "own" origin is created.
        return source == null
                || source.getOriginRef() == null && source.getResourceRef() == null;
    }

    static boolean matches(@NotNull FocusIdentityType identity, @NotNull FocusIdentityType other) {
        return matches(identity, other.getSource());
    }

    public static boolean matches(@NotNull FocusIdentityType identity, FocusIdentitySourceType other) {
        return matches(identity.getSource(), other);
    }

    /**
     * Returns true if these two sources match - i.e., denote the same "logical" entity.
     * This means that (e.g.) shadowRef is ignored.
     */
    public static boolean matches(FocusIdentitySourceType source, FocusIdentitySourceType other) {
        boolean own = isOwn(source);
        boolean otherOwn = isOwn(other);
        if (own) {
            return otherOwn;
        } else if (otherOwn) {
            return false;
        }

        // Ignoring originRef for the moment
        return Objects.equals(getResourceOid(source), getResourceOid(other))
                && source.getKind() == other.getKind()
                && Objects.equals(source.getIntent(), other.getIntent())
                && Objects.equals(source.getTag(), other.getTag());
    }

    private static String getResourceOid(FocusIdentitySourceType source) {
        return getOid(source.getResourceRef());
    }
}
