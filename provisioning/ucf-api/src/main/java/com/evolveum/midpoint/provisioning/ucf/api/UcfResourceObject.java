/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a resource object obtained by UCF connector and passed to clients.
 *
 * TODO finish
 */
public class UcfResourceObject {

    @NotNull private final ShadowType convertedResourceObject;

    public UcfResourceObject(@NotNull ShadowType convertedResourceObject) {
        this.convertedResourceObject = convertedResourceObject;
    }

    public @NotNull ShadowType getConvertedResourceObject() {
        return convertedResourceObject;
    }
}
