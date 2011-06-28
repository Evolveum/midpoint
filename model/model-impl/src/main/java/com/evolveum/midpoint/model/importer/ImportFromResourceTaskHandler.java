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
 * "Portions Copyrighted 2011 [name of copyright owner]"
 * 
 */
package com.evolveum.midpoint.model.importer;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.NotImplementedException;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.provisioning.resource_object_change_listener_1.ResourceObjectChangeListenerPortType;

/**
 * @author Radovan Semancik
 *
 */
public class ImportFromResourceTaskHandler implements TaskHandler {

	private ProvisioningService provisioning;
	private ResourceObjectChangeListener objectChangeListener;
	
	private Map<Task,ImportFromResourceResultHandler> handlers;
	
	public ImportFromResourceTaskHandler() {
		super();
		handlers = new HashMap<Task, ImportFromResourceResultHandler>();
	}

	@Override
	public TaskRunResult run(Task task) {
		
		OperationResult parentResult = task.getResult();
		
		ObjectType object = task.getObject();
		// TODO: Error handling
		ResourceType resource = (ResourceType)object;
		
		ImportFromResourceResultHandler handler = new ImportFromResourceResultHandler(resource,task,objectChangeListener);
		
		// TODO: error checking - already running
		handlers.put(task,handler);
		
		try {
			provisioning.searchObjectsIterative(createAccountShadowTypeQuery(), null, handler, parentResult);
		} catch (SchemaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		handlers.remove(task);
		
		TaskRunResult runResult = new TaskRunResult();
		runResult.setProgress(handler.getProgress());
		runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
		return runResult;
	}
	
	private QueryType createAccountShadowTypeQuery() {
		// TODO
		throw new NotImplementedException();
	}
	
	private ImportFromResourceResultHandler getHandler(Task task) {
		return handlers.get(task);
	}

	@Override
	public long heartbeat(Task task) {
		return getHandler(task).heartbeat();
	}

	@Override
	public void refreshStatus(Task task) {
		// Local task. No refresh needed. The Task instance has always fresh data.
	}

}
