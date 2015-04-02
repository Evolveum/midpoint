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

package com.evolveum.midpoint.wf.impl.processors.primary.assignments;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import org.springframework.stereotype.Component;

/**
 * Treats adding assignment of an abstract role to another abstract role.
 *
 * @author mederly
 */
@Component
public class AddAbstractRoleAbstractRoleAssignmentAspect extends AddAbstractRoleAssignmentAspect<AbstractRoleType> {

    private static final Trace LOGGER = TraceManager.getTrace(AddAbstractRoleAbstractRoleAssignmentAspect.class);

    @Override
    public boolean isEnabledByDefault() {
        return false;
    }

    @Override
    protected boolean isFocusRelevant(ModelContext modelContext) {
        return primaryChangeAspectHelper.isRelatedToType(modelContext, AbstractRoleType.class);
    }

    @Override
    protected String getTargetDisplayName(AbstractRoleType target) {
        String nameOrOid = super.getTargetDisplayName(target);
        if (target instanceof RoleType) {
            return "role " + nameOrOid;
        } else if (target instanceof OrgType) {
            return "org " + nameOrOid;
        } else {
            return nameOrOid;       // should not occur
        }
    }
}