/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * Represents live sync change at the level of UCF.
 */
public class UcfLiveSyncChange extends UcfChange {

    /**
     * Sync token.
     */
    @NotNull private final PrismProperty<?> token;

    public UcfLiveSyncChange(int localSequenceNumber, @NotNull Object primaryIdentifierRealValue,
            @NotNull Collection<ResourceAttribute<?>> identifiers, ObjectClassComplexTypeDefinition objectClassDefinition,
            ObjectDelta<ShadowType> objectDelta, PrismObject<ShadowType> resourceObject,
            @NotNull PrismProperty<?> token, UcfErrorState errorState) {
        super(localSequenceNumber, primaryIdentifierRealValue, objectClassDefinition, identifiers, objectDelta, resourceObject,
                errorState);
        this.token = token;
    }

    public @NotNull PrismProperty<?> getToken() {
        return token;
    }

    @Override
    protected String toStringExtra() {
        return ", token=" + token;
    }

    @Override
    protected void debugDumpExtra(StringBuilder sb, int indent) {
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "token", token, indent + 1);
    }

    @Override
    protected void checkObjectClassDefinitionPresence() {
        if (errorState.isSuccess()) {
            stateCheck(isDelete() || objectClassDefinition != null, "No object class definition for non-delete LS change");
        }
    }
}
