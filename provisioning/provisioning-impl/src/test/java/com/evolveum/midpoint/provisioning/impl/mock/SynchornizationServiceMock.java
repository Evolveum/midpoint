/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.provisioning.impl.mock;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.testng.AssertJUnit;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.api.ResourceOperationListener;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.ObjectChecker;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

@Service(value = "syncServiceMock")
public class SynchornizationServiceMock implements ResourceObjectChangeListener, ResourceOperationListener {

	private static final Trace LOGGER = TraceManager.getTrace(SynchornizationServiceMock.class);

	private int callCountNotifyChange = 0;
	private int callCountNotifyOperation = 0;
	private boolean wasSuccess = false;
	private boolean wasFailure = false;
	private boolean wasInProgress = false;
	private ResourceObjectShadowChangeDescription lastChange = null;
	private ResourceOperationDescription lastOperationDescription = null;
	private ObjectChecker changeChecker;
	private boolean supportActivation = true;

	@Autowired(required=true)
	ChangeNotificationDispatcher notificationManager;
	
	@Autowired(required=true)
	RepositoryService repositoryService;
	
	@PostConstruct
	public void register() {
		notificationManager.registerNotificationListener((ResourceObjectChangeListener)this);
		notificationManager.registerNotificationListener((ResourceOperationListener)this);
	}

	@PreDestroy
	public void unregister() {
		notificationManager.unregisterNotificationListener((ResourceObjectChangeListener)this);
		notificationManager.unregisterNotificationListener((ResourceOperationListener)this);
	}
	
	public ObjectChecker getChangeChecker() {
		return changeChecker;
	}

	public void setChangeChecker(ObjectChecker changeChecker) {
		this.changeChecker = changeChecker;
	}

	public boolean isSupportActivation() {
		return supportActivation;
	}

	public void setSupportActivation(boolean supportActivation) {
		this.supportActivation = supportActivation;
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
		
		if (change.isUnrelatedChange() || isDryRun(task) || (change.getCurrentShadow() != null && change.getCurrentShadow().asObjectable().isProtectedObject() == Boolean.TRUE)){
			return;
		}
		
		if (change.getCurrentShadow() != null) {
			ShadowType currentShadowType = change.getCurrentShadow().asObjectable();
			if (currentShadowType != null) {
				// not a useful check..the current shadow could be null
				assertNotNull("Current shadow does not have an OID", change.getCurrentShadow().getOid());
				assertNotNull("Current shadow does not have resourceRef", currentShadowType.getResourceRef());
				assertNotNull("Current shadow has null attributes", currentShadowType.getAttributes());
				assertFalse("Current shadow has empty attributes", ShadowUtil
						.getAttributesContainer(currentShadowType).isEmpty());

				// Check if the shadow is already present in repo
				try {
					repositoryService.getObject(currentShadowType.getClass(), currentShadowType.getOid(), null, new OperationResult("mockSyncService.notifyChange"));
				} catch (Exception e) {
					AssertJUnit.fail("Got exception while trying to read current shadow "+currentShadowType+
							": "+e.getCause()+": "+e.getMessage());
				}			
				// Check resource
				String resourceOid = ShadowUtil.getResourceOid(currentShadowType);
				assertFalse("No resource OID in current shadow "+currentShadowType, StringUtils.isBlank(resourceOid));
				try {
					repositoryService.getObject(ResourceType.class, resourceOid, null, new OperationResult("mockSyncService.notifyChange"));
				} catch (Exception e) {
					AssertJUnit.fail("Got exception while trying to read resource "+resourceOid+" as specified in current shadow "+currentShadowType+
							": "+e.getCause()+": "+e.getMessage());
				}

				if (change.getCurrentShadow().asObjectable().getKind() == ShadowKindType.ACCOUNT) {
					ShadowType account = change.getCurrentShadow().asObjectable();
					if (supportActivation) {
						assertNotNull("Current shadow does not have activation", account.getActivation());
						assertNotNull("Current shadow activation status is null", account.getActivation()
								.getAdministrativeStatus());
					} else {
						assertNull("Activation sneaked into current shadow", account.getActivation());
					}
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
		callCountNotifyChange++;
		lastChange = change;
	}
	
	 private static boolean isDryRun(Task task){
	    	
	    	Validate.notNull(task, "Task must not be null.");
	    	
	    	if (task.getExtension() == null){
	    		return false;
	    	}
			
	    	PrismProperty<Boolean> item = task.getExtensionProperty(SchemaConstants.MODEL_EXTENSION_DRY_RUN);
			if (item == null || item.isEmpty()){
				return false;
			}
			
			if (item.getValues().size() > 1){
				return false;
//				throw new SchemaException("Unexpected number of values for option 'dry run'.");
			}
					
			Boolean dryRun = item.getValues().iterator().next().getValue();
			
			if (dryRun == null){
				return false;
			}
	    	
			return dryRun.booleanValue(); 
	    }
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener#notifyFailure(com.evolveum.midpoint.provisioning.api.ResourceObjectShadowFailureDescription, com.evolveum.midpoint.task.api.Task, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public void notifySuccess(ResourceOperationDescription opDescription,
			Task task, OperationResult parentResult) {
		notifyOp("success", opDescription, task, parentResult, false);
		wasSuccess = true;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener#notifyFailure(com.evolveum.midpoint.provisioning.api.ResourceObjectShadowFailureDescription, com.evolveum.midpoint.task.api.Task, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public void notifyFailure(ResourceOperationDescription opDescription,
			Task task, OperationResult parentResult) {
		notifyOp("failure", opDescription, task, parentResult, true);
		wasFailure = true;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener#notifyFailure(com.evolveum.midpoint.provisioning.api.ResourceObjectShadowFailureDescription, com.evolveum.midpoint.task.api.Task, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public void notifyInProgress(ResourceOperationDescription opDescription,
			Task task, OperationResult parentResult) {
		notifyOp("in-progress", opDescription, task, parentResult, false);
		wasInProgress = true;
	}

		
	private void notifyOp(String notificationDesc, ResourceOperationDescription opDescription,
			Task task, OperationResult parentResult, boolean failure) {
		LOGGER.debug("Notify "+notificationDesc+" mock called with:\n{}", opDescription.debugDump());

		// Some basic sanity checks
		assertNotNull("No op description", opDescription);
		assertNotNull("No task", task);
		assertNotNull("No result", opDescription.getResult());
		assertNotNull("No resource", opDescription.getResource());
		assertNotNull("No parent result", parentResult);

		assertNotNull("Current shadow not present", opDescription.getCurrentShadow());
		assertNotNull("Delta not present", opDescription.getObjectDelta());
		if (opDescription.getCurrentShadow() != null) {
			ShadowType currentShadowType = opDescription.getCurrentShadow().asObjectable();
			if (currentShadowType != null) {
				// not a useful check..the current shadow could be null
				if (!failure){
				assertNotNull("Current shadow does not have an OID", opDescription.getCurrentShadow().getOid());
				assertNotNull("Current shadow has null attributes", currentShadowType.getAttributes());
				assertFalse("Current shadow has empty attributes", ShadowUtil
						.getAttributesContainer(currentShadowType).isEmpty());
				}
				assertNotNull("Current shadow does not have resourceRef", currentShadowType.getResourceRef());
				
				// Check if the shadow is already present in repo (if it is not a delete case)
				if (!opDescription.getObjectDelta().isDelete() && !failure){
				try {
					repositoryService.getObject(currentShadowType.getClass(), currentShadowType.getOid(), null, new OperationResult("mockSyncService."+notificationDesc));
				} catch (Exception e) {
					AssertJUnit.fail("Got exception while trying to read current shadow "+currentShadowType+
							": "+e.getCause()+": "+e.getMessage());
				}			
				}
				// Check resource
				String resourceOid = ShadowUtil.getResourceOid(currentShadowType);
				assertFalse("No resource OID in current shadow "+currentShadowType, StringUtils.isBlank(resourceOid));
				try {
					repositoryService.getObject(ResourceType.class, resourceOid, null, new OperationResult("mockSyncService."+notificationDesc));
				} catch (Exception e) {
					AssertJUnit.fail("Got exception while trying to read resource "+resourceOid+" as specified in current shadow "+currentShadowType+
							": "+e.getCause()+": "+e.getMessage());
				}

				// FIXME: enable this check later..but for example, opendj
				// resource does not have native capability and if the reosurce
				// does not have sprecified simulated capability, this will
				// produce an error
//				if (opDescription.getCurrentShadow().asObjectable() instanceof AccountShadowType) {
//					AccountShadowType account = (AccountShadowType) opDescription.getCurrentShadow().asObjectable();
//					assertNotNull("Current shadow does not have activation", account.getActivation());
//					assertNotNull("Current shadow activation/enabled is null", account.getActivation()
//							.isEnabled());
//				} else {
//					// We don't support other types now
//					AssertJUnit.fail("Unexpected type of shadow " + opDescription.getCurrentShadow().getClass());
//				}
			}
		}
		if (opDescription.getObjectDelta() != null && !failure) {
			assertNotNull("Delta has null OID", opDescription.getObjectDelta().getOid());
		}
		
		if (changeChecker != null) {
			changeChecker.check(opDescription);
		}

		// remember ...
		callCountNotifyOperation++;
		lastOperationDescription = opDescription;
	}

	public boolean wasCalledNotifyChange() {
		return (callCountNotifyChange > 0);
	}

	public void reset() {
		callCountNotifyChange = 0;
		callCountNotifyOperation = 0;
		lastChange = null;
		wasSuccess = false;
		wasFailure = false;
		wasInProgress = false;
	}

	public ResourceObjectShadowChangeDescription getLastChange() {
		return lastChange;
	}

	public void setLastChange(ResourceObjectShadowChangeDescription lastChange) {
		this.lastChange = lastChange;
	}

	public int getCallCount() {
		return callCountNotifyChange;
	}

	public void setCallCount(int callCount) {
		this.callCountNotifyChange = callCount;
	}

	public void assertNotifyChange() {
		assert wasCalledNotifyChange() : "Expected that notifyChange will be called but it was not";
	}

	public void assertNoNotifyChange() {
		assert !wasCalledNotifyChange() : "Expected that no notifyChange will be called but it was";
	}
	
	public void assertNotifySuccessOnly() {
		assert wasSuccess : "Expected that notifySuccess will be called but it was not";		
		assert !wasFailure : "Expected that notifyFailure will NOT be called but it was";
		assert !wasInProgress : "Expected that notifyInProgress will NOT be called but it was";
		assert callCountNotifyOperation == 1 : "Expected only a single notification call but there was "+callCountNotifyOperation+" calls";
	}

	public void assertNotifyFailureOnly() {
		assert wasFailure : "Expected that notifyFailure will be called but it was not";				
		assert !wasSuccess : "Expected that notifySuccess will NOT be called but it was";
		assert !wasInProgress : "Expected that notifyInProgress will NOT be called but it was";
		assert callCountNotifyOperation == 1 : "Expected only a single notification call but there was "+callCountNotifyOperation+" calls";
	}
	
	public void assertNotifyInProgressOnly() {
		assert wasInProgress : "Expected that notifyInProgress will be called but it was not";				
		assert !wasSuccess : "Expected that notifySuccess will NOT be called but it was";
		assert !wasFailure : "Expected that notifyFailure will NOT be called but it was";
		assert callCountNotifyOperation == 1 : "Expected only a single notification call but there was "+callCountNotifyOperation+" calls";
	}
	
	public void assertNoNotifcations() {
		assert !wasInProgress : "Expected that notifyInProgress will NOT be called but it was";				
		assert !wasSuccess : "Expected that notifySuccess will NOT be called but it was";
		assert !wasFailure : "Expected that notifyFailure will NOT be called but it was";
		assert callCountNotifyOperation == 0 : "Expected no notification call but there was "+callCountNotifyOperation+" calls";
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener#getName()
	 */
	@Override
	public String getName() {
		return "synchronization service mock";
	}

}
