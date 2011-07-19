package com.evolveum.midpoint.provisioning.test.mock;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeNotificationManager;
import com.evolveum.midpoint.provisioning.impl.ProvisioningServiceImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;


@Service(value = "syncServiceMock")
public class SynchornizationServiceMock implements ResourceObjectChangeListener{
	
	private static final Trace LOGGER = TraceManager.getTrace(SynchornizationServiceMock.class);

	private boolean called = false;
	
	@Autowired
	ResourceObjectChangeNotificationManager notificationManager;
	
	@PostConstruct
	public void registerForResourceObjectChangeNotifications(){
		notificationManager.registerNotificationListener(this);
	}
	
	@PreDestroy
	public void unregisterForResourceObjectChangeNotifications(){
		notificationManager.unregisterNotificationListener(this);
	}
	
	@Override
	public void notifyChange(ResourceObjectShadowChangeDescriptionType change, OperationResult parentResult) {
		LOGGER.debug("Notify change in model called");
		called= true;
	}

	public boolean isCalled() {
		return called;
	}

	public void setCalled(boolean called) {
		this.called = called;
	}
	
	

}
