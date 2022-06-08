/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static com.evolveum.midpoint.prism.Referencable.getOid;

public class ConstructionTypeUtil {

    public static String getResourceOid(@Nullable ConstructionType construction) {
        return construction != null ? getOid(construction.getResourceRef()) : null;
    }

    public static @NotNull ShadowKindType getKind(@NotNull ConstructionType construction) {
        // The default for kind is ACCOUNT. But we cannot do the same for intent!
        return Objects.requireNonNullElse(construction.getKind(), ShadowKindType.ACCOUNT);
    }
}
