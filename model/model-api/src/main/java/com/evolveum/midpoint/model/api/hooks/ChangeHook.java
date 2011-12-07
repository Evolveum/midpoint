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

import com.evolveum.midpoint.schema.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

/**
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
	 * If the method returns null, it is assumed that no return value is known yet
	 * and that the execution is switched to background.
	 * 
	 * @param changes primary changes
	 * @param task task in which context we execute
	 * @param result ????
	 * @return new set of changes or null
	 */
	Collection<ObjectDelta<?>> preChangePrimary(Collection<ObjectDelta<?>> changes, Task task, OperationResult result);
	
}
