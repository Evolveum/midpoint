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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.schema.exception.CommunicationException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;

/**
 * Task handler for "Import from resource" task.
 * 
 * The import task will search for all the accounts on a specific resource. It will
 * pretend that all the accounts were just created and notify other components (mode)
 * using the ResourceObjectChangeListener interface. This will efficiently result in
 * importing all the accounts. Depending on the sync policy, appropriate user objects
 * may be created, accounts may be linked to existing users, etc.
 * 
 * The handler will execute the import in background. It is using Task Manager
 * for that purpose, so the Task Manager instance needs to be injected. Most of the "import"
 * action is actually done in the callbacks from provisioning searchObjectsIterative() operation.
 * 
 * The import task may be executed on a different node (as usual for async tasks).
 * 
 * @see TaskHandler
 * @see ResourceObjectChangeListener
 * 
 * @author Radovan Semancik
 *
 */
public class ImportFromResourceTaskHandler implements TaskHandler {
	
	// TODO: correct URI
	public static final String HANDLER_URI = "http://foo.bar/ImportFromResourceTaskHandler";

	private ProvisioningService provisioning;
	private ResourceObjectChangeListener objectChangeListener;
	
	private Map<Task,ImportFromResourceResultHandler> handlers;
	
	public ImportFromResourceTaskHandler() {
		super();
		handlers = new HashMap<Task, ImportFromResourceResultHandler>();
	}
	
	/**
	 * Launch an import. Calling this method will start import in a new
	 * thread, possibly on a different node.
	 * 
	 * @param resource
	 * @param task
	 * @param manager
	 */
	public void launch(ResourceType resource, Task task, TaskManager manager) {
		
		// TODO: result (it is in the Task)
		
		// Set handler URI so we will be called back
		task.setHanderUri(HANDLER_URI);
		
		// Readable task name
		task.setName("Import from resource "+resource.getName());
		
		// Switch task to background. This will start new thread and call
		// the run(task) method.
		// Note: the thread may be actually started on a different node
		manager.switchToBackground(task);
	}

	/**
	 * The body of the task. This will start the import "loop".
	 */
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
			provisioning.searchObjectsIterative(createAccountShadowTypeQuery(resource), null, handler, parentResult);
		} catch (SchemaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ObjectNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CommunicationException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		handlers.remove(task);
		
		TaskRunResult runResult = new TaskRunResult();
		runResult.setProgress(handler.getProgress());
		runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
		return runResult;
	}
	
	private QueryType createAccountShadowTypeQuery(ResourceType resource) {
		
		Document doc = DOMUtil.getDocument();
        Element filter =
                QueryUtil.createAndFilter(doc,
                // TODO: The account type is hardcoded now, it should determined
                // from the schema later, or maybe we can make it entirely
                // generic (use ResourceObjectShadowType instead).
                QueryUtil.createTypeFilter(doc, QNameUtil.qNameToUri(SchemaConstants.I_ACCOUNT_SHADOW_TYPE)),
                QueryUtil.createEqualRefFilter(doc, null, SchemaConstants.I_RESOURCE_REF,resource.getOid()));
		
		QueryType query = new QueryType();
        query.setFilter(filter);

        return query;
	}
	
	private ImportFromResourceResultHandler getHandler(Task task) {
		return handlers.get(task);
	}

	@Override
	public long heartbeat(Task task) {
		// Delegate heartbeat to the result handler
		return getHandler(task).heartbeat();
	}

	@Override
	public void refreshStatus(Task task) {
		// Local task. No refresh needed. The Task instance has always fresh data.
	}

}
