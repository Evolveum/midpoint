/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    @Override
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
