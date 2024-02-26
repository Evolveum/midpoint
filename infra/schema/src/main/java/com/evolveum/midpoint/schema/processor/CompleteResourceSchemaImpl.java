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

    /** TODO */
    private final boolean caseIgnoreAttributeNames;

    CompleteResourceSchemaImpl(
            @NotNull BasicResourceInformation basicResourceInformation,
            boolean caseIgnoreAttributeNames) {
        this.basicResourceInformation = basicResourceInformation;
        this.caseIgnoreAttributeNames = caseIgnoreAttributeNames;
    }

    private CompleteResourceSchemaImpl(
            @NotNull LayerType layer,
            @NotNull BasicResourceInformation basicResourceInformation,
            boolean caseIgnoreAttributeNames) {
        super(layer);
        this.basicResourceInformation = basicResourceInformation;
        this.caseIgnoreAttributeNames = caseIgnoreAttributeNames;
    }

    @Override
    public @NotNull BasicResourceInformation getBasicResourceInformation() {
        return basicResourceInformation;
    }

    @Override
    public boolean isCaseIgnoreAttributeNames() {
        return caseIgnoreAttributeNames;
    }

    @Override
    @NotNull CompleteResourceSchemaImpl createEmptyClone(@NotNull LayerType layer) {
        return new CompleteResourceSchemaImpl(layer, basicResourceInformation, caseIgnoreAttributeNames);
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append(super.toString());
        sb.append(" @").append(basicResourceInformation);
        if (caseIgnoreAttributeNames) {
            sb.append(" (case-ignore attribute names)");
        }
        return sb.toString();
    }
}
