/*
 * Copyright (c) 2013 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.wf;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.processes.StartProcessInstructionForPrimaryStage;
import com.evolveum.midpoint.wf.processes.addrole.AddRoleAssignmentWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Deals with approving primary changes related to a user. Typical such changes are adding of a role or an account.
 *
 * @author mederly
 */

@Component
public class PrimaryUserChangeProcessor extends PrimaryChangeProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(PrimaryUserChangeProcessor.class);

    @Override
    public HookOperationMode startProcessesIfNeeded(ModelContext context, Task task, OperationResult result) throws SchemaException {

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
        return super.startProcessesIfNeeded(context, task, result);
    }


}
