/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Identification of a resource object: one or more identifiers, plus object class(es) information.
 * Everything else is explicitly forbidden in {@link #checkConsistence()} method.
 */
public class UcfResourceObjectIdentification extends UcfResourceObjectFragment {

    private UcfResourceObjectIdentification(@NotNull ShadowType bean, @NotNull UcfErrorState errorState) {
        super(bean, errorState);
    }

    public static UcfResourceObjectIdentification of(@NotNull ShadowType bean) {
        return new UcfResourceObjectIdentification(bean, UcfErrorState.success());
    }

    @Override
    public void checkConsistence() {
        super.checkConsistence();
        bean.asPrismContainerValue().checkNothingExceptFor(
                ShadowType.F_ATTRIBUTES,
                ShadowType.F_OBJECT_CLASS,
                ShadowType.F_AUXILIARY_OBJECT_CLASS);
    }

    @Override
    public @NotNull AbstractShadow withNewContent(@NotNull ShadowType newBean) {
        return new UcfResourceObjectIdentification(newBean, errorState);
    }

    @Override
    public UcfResourceObjectIdentification clone() {
        return new UcfResourceObjectIdentification(bean.clone(), errorState);
    }
}
