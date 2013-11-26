package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

public interface ResourceEventListener extends ProvisioningListener{

	
	public void notifyEvent(ResourceEventDescription eventDescription, Task task, OperationResult parentResult);
}
