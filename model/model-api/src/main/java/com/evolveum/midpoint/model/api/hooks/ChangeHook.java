/**
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.api.hooks;

import java.util.Collection;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;

/**
 * TODO
 * 
 * This applies to all changes, therefore it will "hook" into addObject, modifyObject
 * and also deleteObject.
 * 
 * @author semancik
 *
 */
public interface ChangeHook {

    /**
     * Generic method to be implemented by the hook. It is invoked by the Model Clockwork at these occasions:
     *  - after PRIMARY state has been entered,
     *  - after SECONDARY state has been entered, and
     *  - after each of secondary-state waves has been executed (i.e. with the state of SECONDARY for all except
     *    the last one, will have state set to FINAL).
     *
     *  TODO: what about EXECUTION and POSTEXECUTION states?
     *
     *  @return
     *   - FOREGROUND, if the processing of model operation should continue on the foreground
     *   - BACKGROUND, if the hook switched further processing into background (and, therefore,
     *     current execution of model operation should end immediately, in the hope it will eventually
     *     be resumed later)
     *   - ERROR, if the hook encountered an error which prevents model operation from continuing
     *     (this case is currently not defined very well)
     */
    HookOperationMode invoke(ModelContext context, Task task, OperationResult result);

	/**
	 * Callback after the change is executed by the model. The callback gets a view of the
	 * changes after they were executed - with filled-in OIDs, generated values, etc. 
	 * 
	 * The callback may be used to post-process the changes, e.g. to notify users about their
	 * new accounts.
	 *
	 * @param changes changes after the execution
	 * @param task task in which context we execute
	 * @param result ????
	 */
//    @Deprecated
//	void postChange(Collection<ObjectDelta<? extends ObjectType>> changes, Task task, OperationResult result);
}
