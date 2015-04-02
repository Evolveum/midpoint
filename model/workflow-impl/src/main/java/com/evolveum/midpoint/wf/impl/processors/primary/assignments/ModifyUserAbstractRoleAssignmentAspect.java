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
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Treats modifying abstract role assignment to a user.
 *
 * @author mederly
 */
@Component
public class ModifyUserAbstractRoleAssignmentAspect extends ModifyAbstractRoleAssignmentAspect<UserType> {

    private static final Trace LOGGER = TraceManager.getTrace(ModifyUserAbstractRoleAssignmentAspect.class);

    @Override
    public boolean isEnabledByDefault() {
        return true;
    }

    @Override
    protected boolean isFocusRelevant(ModelContext modelContext) {
        return primaryChangeAspectHelper.isRelatedToType(modelContext, UserType.class);
    }
}