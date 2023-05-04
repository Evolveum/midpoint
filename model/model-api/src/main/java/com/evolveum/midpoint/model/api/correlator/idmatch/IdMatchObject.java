/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator.idmatch;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IdMatchAttributesType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Collection;

/**
 * Object to be matched, resolved or updated.
 */
public class IdMatchObject implements DebugDumpable, Serializable {

    @NotNull private final String sorIdentifierValue;

    @NotNull private final IdMatchAttributesType attributes;

    private IdMatchObject(@NotNull String sorIdentifierValue, @NotNull IdMatchAttributesType attributes) {
        this.sorIdentifierValue = sorIdentifierValue;
        this.attributes = attributes;
    }

    public static @NotNull IdMatchObject create(@NotNull String sorIdentifierValue, @NotNull IdMatchAttributesType attributes) {
        return new IdMatchObject(sorIdentifierValue, attributes);
    }

    @VisibleForTesting
    public static @NotNull IdMatchObject create(@NotNull String sorIdentifier, @NotNull ShadowAttributesType attributes)
            throws SchemaException {
        return new IdMatchObject(sorIdentifier, copyAttributes(attributes));
    }

    private static IdMatchAttributesType copyAttributes(ShadowAttributesType attributes) throws SchemaException {
        IdMatchAttributesType target = new IdMatchAttributesType();
        for (Item<?, ?> attribute : ((PrismContainerValue<?>) attributes.asPrismContainerValue()).getItems()) {
            //noinspection unchecked
            target.asPrismContainerValue().add(attribute.clone());
        }
        return target;
    }

    public @NotNull String getSorIdentifierValue() {
        return sorIdentifierValue;
    }

    public @NotNull IdMatchAttributesType getAttributes() {
        return attributes;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id='" + sorIdentifierValue + '\'' +
                ", attributes: " + attributes.asPrismContainerValue().size() +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "sorIdentifierValue", sorIdentifierValue, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "attributes", attributes, indent + 1);
        return sb.toString();
    }

    public Collection<? extends PrismProperty<?>> getProperties() {
        //noinspection unchecked
        return attributes.asPrismContainerValue().getItems();
    }
}
