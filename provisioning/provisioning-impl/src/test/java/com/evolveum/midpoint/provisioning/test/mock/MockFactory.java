package com.evolveum.midpoint.provisioning.test.mock;

import org.mockito.Mockito;

import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.provisioning.api.SynchronizationProcessManager;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorInstanceIcfImpl;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.impl.TaskImpl;

public class MockFactory {

	
	public static Task createTaskMock() {
		return Mockito.mock(TaskImpl.class);
	}
	
	public static ConnectorInstance createConnectorInstance() {
		return Mockito.mock(ConnectorInstanceIcfImpl.class);
	}

}
