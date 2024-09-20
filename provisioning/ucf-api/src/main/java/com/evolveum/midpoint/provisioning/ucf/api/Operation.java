/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PropertyModificationOperationType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Abstract operation for a connector. Subclasses of this class
 * represent specific operations such as attribute modification,
 * script execution and so on.
 *
 * This class is created primarily for type safety, but it may be
 * extended later on.
 *
 * @author Radovan Semancik
 */
public abstract class Operation implements DebugDumpable {

    /** Do the operations contain an identifier change? */
    public static boolean isRename(@NotNull Collection<Operation> operations, @NotNull ResourceObjectDefinition objDef) {
        return operations.stream()
                .anyMatch(operation -> operation.isRename(objDef));
    }

    public abstract @NotNull ItemDelta<?, ?> getItemDelta();

    /** Is this an identifier change? */
    public abstract boolean isRename(@NotNull ResourceObjectDefinition objDef);

    public abstract boolean isAttributeDelta();

    public abstract @Nullable ShadowSimpleAttributeDefinition<?> getAttributeDefinitionIfApplicable(
            @NotNull ResourceObjectDefinition objDef);

    /** Converts this Operation into respective xType bean */
    public abstract PropertyModificationOperationType asBean(PrismContext prismContext) throws SchemaException;
}
