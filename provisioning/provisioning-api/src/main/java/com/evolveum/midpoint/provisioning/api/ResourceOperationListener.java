/**
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
package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

/**
 * @author semancik
 *
 */
public interface ResourceOperationListener extends ProvisioningListener {
	
	/**
	 * Submits notification about a failure to apply a change on resource.
	 * 
	 * This describes the change that should have been executed on the resource but that 
	 * never happened because a failure was detected. The upper layers are
	 * notified to take handle that failure (e.g. notify the administrator).
	 * 
	 * This should be called for operations that were done asynchronously and failed
	 * to execute. It should NOT be called for synchronous operations. Direct return
	 * value should be used instead.
	 * 
	 * The call should return without a major delay. It means that the
	 * implementation can do calls to repository, but it should not
	 * (synchronously) initiate a long-running process or provisioning request.
	 */
	public void notifyFailure(ResourceOperationFailureDescription failureDescription, Task task, OperationResult parentResult);

}
