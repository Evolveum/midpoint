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

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;

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
	 * Callback before the change is processed by the model. The callback is in the
	 * "primary" change phase. It is called before the policies and expressions are
	 * being executed. The callback may validate or even manipulate the changes before
	 * they are processed e.g. by approval process. The (manipulated) changes will be
	 * recomputed to secondary changes after this step. 
	 * 
	 * If the method returns FOREGROUND then the value of "changes" parameter will be
	 * used directly. Otherwise the return value from the task will be used.
	 * 
	 * @see HookOperationMode
	 * 
	 * @param changes primary changes
	 * @param task task in which context we execute
	 * @param result ????
	 * @return indication of hook operation mode (foreground or background)
	 */
	HookOperationMode preChangePrimary(Collection<ObjectDelta<? extends ObjectType>> changes, Task task, OperationResult result);

	/**
	 * Callback before the change is processed by the model. The callback is in the
	 * "secondary" change phase. It is called after the policies and expressions are
	 * being executed but before the change is applied to repository and resources.
	 * The callback may validate or even manipulate the changes before they are executed.
	 * The (manipulated) changes will NOT be recomputed again and will be directly applied.
	 * 
	 * If the method returns FOREGROUND then the value of "changes" parameter will be
	 * used directly. Otherwise the return value from the task will be used.
	 * 
	 * @see HookOperationMode
	 * 
	 * @param changes secondary changes
	 * @param task task in which context we execute
	 * @param result ????
	 * @return indication of hook operation mode (foreground or background)
	 */
	HookOperationMode preChangeSecondary(Collection<ObjectDelta<? extends ObjectType>> changes, Task task, OperationResult result);

	/**
	 * Callback after the change is executed by the model. The callback gets a view of the
	 * changes after they were executed - with filled-in OIDs, generated values, etc. 
	 * 
	 * The callback may be used to post-process the changes, e.g. to notify users about their
	 * new accounts.
	 * 
	 * If the method returns FOREGROUND then the value of "changes" parameter will be
	 * used directly (if needed). Otherwise the return value from the task will be used.
	 * 
	 * @see HookOperationMode
	 * 
	 * @param changes changes after the execution
	 * @param task task in which context we execute
	 * @param result ????
	 * @return indication of hook operation mode (foreground or background)
	 */
	HookOperationMode postChange(Collection<ObjectDelta<? extends ObjectType>> changes, Task task, OperationResult result);
}
