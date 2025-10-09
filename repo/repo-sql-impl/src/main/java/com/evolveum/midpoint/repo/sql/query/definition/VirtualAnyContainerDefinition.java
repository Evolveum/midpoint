/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.query.definition;

import java.util.Objects;

import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;

import org.jetbrains.annotations.NotNull;

public class VirtualAnyContainerDefinition extends JpaAnyContainerDefinition {

    /**
     * ObjectType (for extension) or ShadowType (for attributes).
     */
    private final RObjectExtensionType ownerType;

    VirtualAnyContainerDefinition(@NotNull RObjectExtensionType ownerType) {
        super(RObject.class); // RObject is artificial - don't want to make jpaClass nullable just for this single situation
        Objects.requireNonNull(ownerType, "ownerType");
        this.ownerType = ownerType;
    }

    public RObjectExtensionType getOwnerType() {
        return ownerType;
    }

    @Override
    protected String getDebugDumpClassName() {
        return "VirtualAny";
    }

    @Override
    public String debugDump(int indent) {
        return super.debugDump(indent) + ", ownerType=" + ownerType;
    }
}
