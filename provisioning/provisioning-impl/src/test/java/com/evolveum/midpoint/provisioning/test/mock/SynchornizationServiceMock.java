package com.evolveum.midpoint.provisioning.test.mock;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.testng.AssertJUnit;

import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowFailureDescription;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.ObjectChecker;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;

@Service(value = "syncServiceMock")
public class SynchornizationServiceMock implements ResourceObjectChangeListener {

	private static final Trace LOGGER = TraceManager.getTrace(SynchornizationServiceMock.class);

	private int callCount = 0;
	private ResourceObjectShadowChangeDescription lastChange = null;
	private ResourceObjectShadowFailureDescription lastFailure = null;
	private ObjectChecker changeChecker;

	@Autowired(required=true)
	ChangeNotificationDispatcher notificationManager;
	@Autowired(required=true)
	RepositoryService repositoryService;
	
	@PostConstruct
	public void registerForResourceObjectChangeNotifications() {
		notificationManager.registerNotificationListener(this);
	}

	@PreDestroy
	public void unregisterForResourceObjectChangeNotifications() {
		notificationManager.unregisterNotificationListener(this);
	}
	
	public ObjectChecker getChangeChecker() {
		return changeChecker;
	}

	public void setChangeChecker(ObjectChecker changeChecker) {
		this.changeChecker = changeChecker;
	}

	@Override
	public void notifyChange(ResourceObjectShadowChangeDescription change, Task task,
			OperationResult parentResult) {
		LOGGER.debug("Notify change mock called with {}", change);

		// Some basic sanity checks
		assertNotNull("No change", change);
		assertNotNull("No task", task);
		assertNotNull("No resource", change.getResource());
		assertNotNull("No parent result", parentResult);

		assertTrue("Either current shadow or delta must be present", change.getCurrentShadow() != null
				|| change.getObjectDelta() != null);
		if (change.getCurrentShadow() != null) {
			ResourceObjectShadowType currentShadowType = change.getCurrentShadow().asObjectable();
			if (currentShadowType != null) {
				// not a useful check..the current shadow could be null
				assertNotNull("Current shadow does not have an OID", change.getCurrentShadow().getOid());
				assertNotNull("Current shadow does not have resourceRef", currentShadowType.getResourceRef());
				assertNotNull("Current shadow has null attributes", currentShadowType.getAttributes());
				assertFalse("Current shadow has empty attributes", ResourceObjectShadowUtil
						.getAttributesContainer(currentShadowType).isEmpty());

				// Check if the shadow is already present in repo
				try {
					repositoryService.getObject(currentShadowType.getClass(), currentShadowType.getOid(), new OperationResult("mockSyncService.notifyChange"));
				} catch (Exception e) {
					AssertJUnit.fail("Got exception while trying to read current shadow "+currentShadowType+
							": "+e.getCause()+": "+e.getMessage());
				}			
				// Check resource
				String resourceOid = ResourceObjectShadowUtil.getResourceOid(currentShadowType);
				assertFalse("No resource OID in current shadow "+currentShadowType, StringUtils.isBlank(resourceOid));
				try {
					repositoryService.getObject(ResourceType.class, resourceOid, new OperationResult("mockSyncService.notifyChange"));
				} catch (Exception e) {
					AssertJUnit.fail("Got exception while trying to read resource "+resourceOid+" as specified in current shadow "+currentShadowType+
							": "+e.getCause()+": "+e.getMessage());
				}

				if (change.getCurrentShadow().asObjectable() instanceof AccountShadowType) {
					AccountShadowType account = (AccountShadowType) change.getCurrentShadow().asObjectable();
					assertNotNull("Current shadow does not have activation", account.getActivation());
					assertNotNull("Current shadow activation/enabled is null", account.getActivation()
							.isEnabled());
				} else {
					// We don't support other types now
					AssertJUnit.fail("Unexpected type of shadow " + change.getCurrentShadow().getClass());
				}
			}
		}
		if (change.getOldShadow() != null) {
			assertNotNull("Old shadow does not have an OID", change.getOldShadow().getOid());
			assertNotNull("Old shadow does not have an resourceRef", change.getOldShadow().asObjectable()
					.getResourceRef());
		}
		if (change.getObjectDelta() != null) {
			assertNotNull("Delta has null OID", change.getObjectDelta().getOid());
		}
		
		if (changeChecker != null) {
			changeChecker.check(change);
		}

		// remember ...
		callCount++;
		lastChange = change;
	}
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener#notifyFailure(com.evolveum.midpoint.provisioning.api.ResourceObjectShadowFailureDescription, com.evolveum.midpoint.task.api.Task, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public void notifyFailure(ResourceObjectShadowFailureDescription failureDescription,
			Task task, OperationResult parentResult) {
		LOGGER.debug("Notify failure mock called with {}", failureDescription);

		// Some basic sanity checks
		assertNotNull("No failure", failureDescription);
		assertNotNull("No task", task);
		assertNotNull("No result", failureDescription.getResult());
		assertNotNull("No resource", failureDescription.getResource());
		assertNotNull("No parent result", parentResult);

		assertNotNull("Current shadow not present", failureDescription.getCurrentShadow());
		assertNotNull("Delta not present", failureDescription.getObjectDelta());
		if (failureDescription.getCurrentShadow() != null) {
			ResourceObjectShadowType currentShadowType = failureDescription.getCurrentShadow().asObjectable();
			if (currentShadowType != null) {
				// not a useful check..the current shadow could be null
				assertNotNull("Current shadow does not have an OID", failureDescription.getCurrentShadow().getOid());
				assertNotNull("Current shadow does not have resourceRef", currentShadowType.getResourceRef());
				assertNotNull("Current shadow has null attributes", currentShadowType.getAttributes());
				assertFalse("Current shadow has empty attributes", ResourceObjectShadowUtil
						.getAttributesContainer(currentShadowType).isEmpty());

				// Check if the shadow is already present in repo
				try {
					repositoryService.getObject(currentShadowType.getClass(), currentShadowType.getOid(), new OperationResult("mockSyncService.notifyChange"));
				} catch (Exception e) {
					AssertJUnit.fail("Got exception while trying to read current shadow "+currentShadowType+
							": "+e.getCause()+": "+e.getMessage());
				}			
				// Check resource
				String resourceOid = ResourceObjectShadowUtil.getResourceOid(currentShadowType);
				assertFalse("No resource OID in current shadow "+currentShadowType, StringUtils.isBlank(resourceOid));
				try {
					repositoryService.getObject(ResourceType.class, resourceOid, new OperationResult("mockSyncService.notifyChange"));
				} catch (Exception e) {
					AssertJUnit.fail("Got exception while trying to read resource "+resourceOid+" as specified in current shadow "+currentShadowType+
							": "+e.getCause()+": "+e.getMessage());
				}

				if (failureDescription.getCurrentShadow().asObjectable() instanceof AccountShadowType) {
					AccountShadowType account = (AccountShadowType) failureDescription.getCurrentShadow().asObjectable();
					assertNotNull("Current shadow does not have activation", account.getActivation());
					assertNotNull("Current shadow activation/enabled is null", account.getActivation()
							.isEnabled());
				} else {
					// We don't support other types now
					AssertJUnit.fail("Unexpected type of shadow " + failureDescription.getCurrentShadow().getClass());
				}
			}
		}
		if (failureDescription.getObjectDelta() != null) {
			assertNotNull("Delta has null OID", failureDescription.getObjectDelta().getOid());
		}
		
		if (changeChecker != null) {
			changeChecker.check(failureDescription);
		}

		// remember ...
		callCount++;
		lastFailure = failureDescription;
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

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener#getName()
	 */
	@Override
	public String getName() {
		return "synchronization service mock";
	}

}
