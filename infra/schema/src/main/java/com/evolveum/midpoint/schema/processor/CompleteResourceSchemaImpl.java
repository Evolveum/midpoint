/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;

import org.jetbrains.annotations.NotNull;

public class CompleteResourceSchemaImpl extends ResourceSchemaImpl implements CompleteResourceSchema {

    @NotNull private final BasicResourceInformation basicResourceInformation;

    CompleteResourceSchemaImpl(@NotNull BasicResourceInformation basicResourceInformation) {
        this.basicResourceInformation = basicResourceInformation;
    }

    private CompleteResourceSchemaImpl(@NotNull LayerType layer, @NotNull BasicResourceInformation basicResourceInformation) {
        super(layer);
        this.basicResourceInformation = basicResourceInformation;
    }

    @Override
    public @NotNull BasicResourceInformation getBasicResourceInformation() {
        return basicResourceInformation;
    }

    @Override
    @NotNull CompleteResourceSchemaImpl createEmptyClone(@NotNull LayerType layer) {
        return new CompleteResourceSchemaImpl(layer, basicResourceInformation);
    }

    @Override
    public String toString() {
        return super.toString() + " @" + basicResourceInformation;
    }
}
