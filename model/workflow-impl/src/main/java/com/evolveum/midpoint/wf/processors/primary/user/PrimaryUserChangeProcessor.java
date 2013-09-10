/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.processors.primary.user;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import org.springframework.stereotype.Component;

/**
 * Deals with approving primary changes related to a user. Typical such changes are adding of a role or an account.
 *
 * @author mederly
 */

@Component("primaryUserChangeProcessor")
public class PrimaryUserChangeProcessor extends PrimaryChangeProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(PrimaryUserChangeProcessor.class);

    @Override
    public HookOperationMode processModelInvocation(ModelContext context, Task taskFromModel, OperationResult result) throws SchemaException {

        if (context.getState() != ModelState.PRIMARY || context.getFocusContext() == null) {
            return null;
        }

        ObjectDelta<Objectable> change = context.getFocusContext().getPrimaryDelta();
        if (change == null) {
            return null;
        }

        // let's check whether we deal with a user

        if (!UserType.class.isAssignableFrom(change.getObjectTypeClass())) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("change object type class is not a UserType (it is " + change.getObjectTypeClass() + "), exiting " + this.getClass().getSimpleName());
            }
            return null;
        }

        // the rest is quite generic
        return super.processModelInvocation(context, taskFromModel, result);
    }


}
