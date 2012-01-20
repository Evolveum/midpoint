package com.evolveum.midpoint.provisioning.test.mock;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ws.rs.DELETE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.testng.AssertJUnit;

import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.processor.ChangeType;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;


@Service(value = "syncServiceMock")
public class SynchornizationServiceMock implements ResourceObjectChangeListener{
	
	private static final Trace LOGGER = TraceManager.getTrace(SynchornizationServiceMock.class);

	private int callCount = 0;
	private ResourceObjectShadowChangeDescription lastChange = null;
	
	@Autowired
	ChangeNotificationDispatcher notificationManager;
	
	@PostConstruct
	public void registerForResourceObjectChangeNotifications(){
		notificationManager.registerNotificationListener(this);
	}
	
	@PreDestroy
	public void unregisterForResourceObjectChangeNotifications(){
		notificationManager.unregisterNotificationListener(this);
	}
	
	@Override
	public void notifyChange(ResourceObjectShadowChangeDescription change, OperationResult parentResult) {
		LOGGER.debug("Notify change mock called with {}", change);
		
		// Some basic sanity checks
		
		assertTrue("Either current shadow or delta must be present",change.getCurrentShadow() != null || change.getObjectDelta() != null);
		if (change.getCurrentShadow() != null) {
			assertNotNull("Current shadow does not have an OID", change.getCurrentShadow().getOid());
			assertNotNull("Current shadow does not have resourceRef", change.getCurrentShadow().getResourceRef());
			assertNotNull("Current shadow has null attributes", change.getCurrentShadow().getAttributes());
			assertNotNull("Current shadow has empty attributes", change.getCurrentShadow().getAttributes().getAny().isEmpty());
			
			if (change.getCurrentShadow() instanceof AccountShadowType) {
				AccountShadowType account = (AccountShadowType)change.getCurrentShadow();
				assertNotNull("Current shadow does not have activation", account.getActivation());
				assertNotNull("Current shadow activation/enabled is null", account.getActivation().isEnabled());
			} else {
				// We don't support other types now
				AssertJUnit.fail("Unexpected type of shadow "+change.getCurrentShadow().getClass());
			}
		}
		if (change.getOldShadow() != null) {
			assertNotNull("Old shadow does not have an OID", change.getOldShadow().getOid());
			assertNotNull("Old shadow does not have an resourceRef", change.getOldShadow().getResourceRef());
		}
		if (change.getObjectDelta() != null) {
			assertNotNull("Delta has null OID", change.getObjectDelta().getOid());
		}
		
		// remember ...
		callCount++;
		lastChange = change;
	}

	public boolean wasCalled() {
		return (callCount > 0);
	}

	public void reset() {
		callCount = 0;
		lastChange = null;
	}

	public ResourceObjectShadowChangeDescription getLastChange() {
		return lastChange;
	}

	public void setLastChange(ResourceObjectShadowChangeDescription lastChange) {
		this.lastChange = lastChange;
	}

	public int getCallCount() {
		return callCount;
	}

	public void setCallCount(int callCount) {
		this.callCount = callCount;
	}

}
