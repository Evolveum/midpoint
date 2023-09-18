/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDependencyDataBindingKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDependencyStrictnessType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDependencyType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static com.evolveum.midpoint.prism.Referencable.getOid;
import static com.evolveum.midpoint.util.MiscUtil.or0;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDependencyStrictnessType.STRICT;

public class ResourceObjectTypeDependencyTypeUtil {

    public static String describe(ResourceObjectTypeDependencyType dependency) {
        return describe(dependency, null);
    }

    public static String describe(ResourceObjectTypeDependencyType dependency, @Nullable String resourceName) {
        if (dependency == null) {
            return "null";
        } else {
            String resourceOid = getOid(dependency.getResourceRef());
            return String.format("%s/%s on %s; with order=%d",
                    dependency.getKind(),
                    dependency.getIntent() != null ? dependency.getIntent() : "(not specified)",
                    resourceName != null ?
                            resourceName + " (" + resourceOid + ")" :
                            resourceOid,
                    or0(dependency.getOrder()));
        }
    }

    public static @NotNull String getResourceOidRequired(@NotNull ResourceObjectTypeDependencyType dependency) {
        return MiscUtil.argNonNull(
                getOid(dependency.getResourceRef()),
                () -> "No resource OID in dependency [" + describe(dependency) + "]");
    }

    public static @NotNull ShadowKindType getKindRequired(@NotNull ResourceObjectTypeDependencyType dependency) {
        return MiscUtil.argNonNull(
                dependency.getKind(),
                () -> "No kind in dependency [" + describe(dependency) + "]");
    }

    public static ResourceObjectTypeDependencyStrictnessType getDependencyStrictness(
            ResourceObjectTypeDependencyType dependency) {
        return Objects.requireNonNullElse(dependency.getStrictness(), STRICT);
    }

    public static boolean isStrict(ResourceObjectTypeDependencyType dependency) {
        return getDependencyStrictness(dependency) == STRICT;
    }

    public static boolean isForceLoadDependentShadow(ResourceObjectTypeDependencyType dependency) {
        return Boolean.TRUE.equals(dependency.isForceLoad());
    }

    public static boolean isDataBindingPresent(ResourceObjectTypeDependencyType dependency) {
        return dependency.getDataBinding() == ResourceObjectTypeDependencyDataBindingKindType.SOME;
    }
}
