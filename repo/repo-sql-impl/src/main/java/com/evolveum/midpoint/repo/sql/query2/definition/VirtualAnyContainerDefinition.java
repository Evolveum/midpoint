/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query2.definition;

import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;
import org.apache.commons.lang.Validate;

/**
 * @author mederly
 */
public class VirtualAnyContainerDefinition extends JpaAnyContainerDefinition {

    private RObjectExtensionType ownerType;            // ObjectType (for extension) or ShadowType (for attributes)

    public VirtualAnyContainerDefinition(RObjectExtensionType ownerType) {
        super(RObject.class);       // RObject is artificial - don't want to make jpaClass nullable just for this single situation
        Validate.notNull(ownerType, "ownerType");
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
