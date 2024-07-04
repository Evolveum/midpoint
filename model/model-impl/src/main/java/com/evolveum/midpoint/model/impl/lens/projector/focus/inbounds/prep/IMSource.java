/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep;

import com.evolveum.midpoint.schema.processor.ResourceObjectInboundDefinition;
import com.evolveum.midpoint.schema.processor.ShadowAssociationDefinition;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttributeDefinition;
import com.evolveum.midpoint.util.DebugDumpable;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/** TEMPORARY */
interface IMSource extends DebugDumpable {

    @NotNull Collection<? extends ShadowSimpleAttributeDefinition<?>> getSimpleAttributeDefinitions();

    @NotNull Collection<? extends ShadowReferenceAttributeDefinition> getObjectReferenceAttributeDefinitions();

    @NotNull Collection<? extends ShadowAssociationDefinition> getAssociationDefinitions();

    @NotNull ResourceObjectInboundDefinition getInboundDefinition();

}
