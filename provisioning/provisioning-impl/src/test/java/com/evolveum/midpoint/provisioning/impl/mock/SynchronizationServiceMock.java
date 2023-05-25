/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.mock;

import static org.testng.AssertJUnit.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import com.evolveum.midpoint.task.api.TaskUtil;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.testng.AssertJUnit;

import com.evolveum.midpoint.provisioning.api.*;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

@Service(value = "syncServiceMock")
public class SynchronizationServiceMock
        implements ResourceObjectChangeListener, ResourceOperationListener {

    private static final Trace LOGGER = TraceManager.getTrace(SynchronizationServiceMock.class);

    private int callCountNotifyChange = 0;
    private int callCountNotifyOperation = 0;
    private boolean wasSuccess = false;
    private boolean wasFailure = false;
    private boolean wasInProgress = false;
    private ResourceObjectShadowChangeDescription lastChange = null;
    private boolean supportActivation = true;

    @Autowired private EventDispatcher notificationManager;
    @Autowired private RepositoryService repositoryService;

    @PostConstruct
    public void register() {
        notificationManager.registerListener((ResourceObjectChangeListener) this);
        notificationManager.registerListener((ResourceOperationListener) this);
    }

    @PreDestroy
    public void unregister() {
        notificationManager.unregisterListener((ResourceObjectChangeListener) this);
        notificationManager.unregisterListener((ResourceOperationListener) this);
    }

    public boolean isSupportActivation() {
        return supportActivation;
    }

    public void setSupportActivation(boolean supportActivation) {
        this.supportActivation = supportActivation;
    }

    @Override
    public void notifyChange(@NotNull ResourceObjectShadowChangeDescription change, Task task,
            OperationResult parentResult) {
        LOGGER.debug("Notify change mock called with {}", change);

        // Some basic sanity checks
        assertNotNull("No change", change);
        assertNotNull("No task", task);
        assertNotNull("No resource", change.getResource());
        assertNotNull("No parent result", parentResult);

        if (TaskUtil.isDryRun(task)
                || Boolean.TRUE.equals(change.getShadowedResourceObject().asObjectable().isProtectedObject())) {
            return;
        }

        ShadowType currentShadowType = change.getShadowedResourceObject().asObjectable();
        // not a useful check..the current shadow could be null
        assertNotNull("Current shadow does not have an OID", change.getShadowedResourceObject().getOid());
        assertNotNull("Current shadow does not have resourceRef", currentShadowType.getResourceRef());
        assertNotNull("Current shadow has null attributes", currentShadowType.getAttributes());
        assertFalse("Current shadow has empty attributes", ShadowUtil
                .getAttributesContainer(currentShadowType).isEmpty());

        if (!change.isDelete()) {
            // Check if the shadow is already present in repo
            try {
                repositoryService.getObject(currentShadowType.getClass(), currentShadowType.getOid(), null, new OperationResult("mockSyncService.notifyChange"));
            } catch (Exception e) {
                AssertJUnit.fail("Got exception while trying to read current shadow " + currentShadowType +
                        ": " + e.getCause() + ": " + e.getMessage());
            }
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

        if (change.getShadowedResourceObject().asObjectable().getKind() == ShadowKindType.ACCOUNT) {
            ShadowType account = change.getShadowedResourceObject().asObjectable();
            if (ShadowUtil.isExists(account)) {
                if (supportActivation) {
                    assertNotNull("Current shadow does not have activation", account.getActivation());
                    assertNotNull("Current shadow activation status is null", account.getActivation()
                            .getAdministrativeStatus());
                } else {
                    assertNull("Activation sneaked into current shadow", account.getActivation());
                }
            }
        }

        // remember ...
        callCountNotifyChange++;
        lastChange = change;
    }

    @Override
    public void notifySuccess(@NotNull ResourceOperationDescription opDescription,
            Task task, OperationResult parentResult) {
        notifyOp("success", opDescription, task, parentResult, false);
        wasSuccess = true;
    }

    @Override
    public void notifyFailure(@NotNull ResourceOperationDescription opDescription,
            Task task, OperationResult parentResult) {
        notifyOp("failure", opDescription, task, parentResult, true);
        wasFailure = true;
    }

    @Override
    public void notifyInProgress(@NotNull ResourceOperationDescription opDescription,
            Task task, OperationResult parentResult) {
        notifyOp("in-progress", opDescription, task, parentResult, false);
        wasInProgress = true;
    }

    private void notifyOp(String notificationDesc, ResourceOperationDescription opDescription,
            Task task, OperationResult parentResult, boolean failure) {
        LOGGER.debug("Notify " + notificationDesc + " mock called with:\n{}", opDescription.debugDump());

        // Some basic sanity checks
        assertNotNull("No op description", opDescription);
        assertNotNull("No task", task);
        assertNotNull("No resource", opDescription.getResource());
        assertNotNull("No parent result", parentResult);

        assertNotNull("Current shadow not present", opDescription.getCurrentShadow());
        if (!failure) {
            assertNotNull("Delta not present", opDescription.getObjectDelta());
        }
        if (opDescription.getCurrentShadow() != null) {
            ShadowType currentShadowType = opDescription.getCurrentShadow().asObjectable();
            // not a useful check..the current shadow could be null
            if (!failure) {
                assertNotNull("Current shadow does not have an OID", opDescription.getCurrentShadow().getOid());
                assertNotNull("Current shadow has null attributes", currentShadowType.getAttributes());
                assertFalse("Current shadow has empty attributes", ShadowUtil
                        .getAttributesContainer(currentShadowType).isEmpty());
            }
            assertNotNull("Current shadow does not have resourceRef", currentShadowType.getResourceRef());

            // Check if the shadow is already present in repo (if it is not a delete case)
            if (!opDescription.getObjectDelta().isDelete() && !failure) {
                try {
                    repositoryService.getObject(currentShadowType.getClass(), currentShadowType.getOid(), null, new OperationResult("mockSyncService." + notificationDesc));
                } catch (Exception e) {
                    AssertJUnit.fail("Got exception while trying to read current shadow " + currentShadowType +
                            ": " + e.getCause() + ": " + e.getMessage());
                }
            }
            // Check resource
            String resourceOid = ShadowUtil.getResourceOid(currentShadowType);
            assertFalse("No resource OID in current shadow " + currentShadowType, StringUtils.isBlank(resourceOid));
            try {
                repositoryService.getObject(ResourceType.class, resourceOid, null, new OperationResult("mockSyncService." + notificationDesc));
            } catch (Exception e) {
                AssertJUnit.fail("Got exception while trying to read resource " + resourceOid + " as specified in current shadow " + currentShadowType +
                        ": " + e.getCause() + ": " + e.getMessage());
            }
        }
        if (opDescription.getObjectDelta() != null && !failure) {
            assertNotNull("Delta has null OID", opDescription.getObjectDelta().getOid());
        }

        // remember ...
        callCountNotifyOperation++;
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

    public int getCallCount() {
        return callCountNotifyChange;
    }

    public void setCallCount(int callCount) {
        this.callCountNotifyChange = callCount;
    }

    public SynchronizationServiceMock assertNotifyChange() {
        assert wasCalledNotifyChange() : "Expected that notifyChange will be called but it was not";
        return this;
    }

    public SynchronizationServiceMock assertNoNotifyChange() {
        assert !wasCalledNotifyChange() : "Expected that no notifyChange will be called but it was";
        return this;
    }

    public ResourceObjectShadowChangeDescriptionAsserter lastNotifyChange() {
        return new ResourceObjectShadowChangeDescriptionAsserter(lastChange);
    }

    public SynchronizationServiceMock assertSingleNotifySuccessOnly() {
        assert wasSuccess : "Expected that notifySuccess will be called but it was not";
        assert !wasFailure : "Expected that notifyFailure will NOT be called but it was";
        assert !wasInProgress : "Expected that notifyInProgress will NOT be called but it was";
        assert callCountNotifyOperation == 1 : "Expected only a single notification call but there was " + callCountNotifyOperation + " calls";
        return this;
    }

    public SynchronizationServiceMock assertNotifySuccessOnly() {
        assert wasSuccess : "Expected that notifySuccess will be called but it was not";
        assert !wasFailure : "Expected that notifyFailure will NOT be called but it was";
        assert !wasInProgress : "Expected that notifyInProgress will NOT be called but it was";
        return this;
    }

    public SynchronizationServiceMock assertSingleNotifyFailureOnly() {
        assert wasFailure : "Expected that notifyFailure will be called but it was not";
        assert !wasSuccess : "Expected that notifySuccess will NOT be called but it was";
        assert !wasInProgress : "Expected that notifyInProgress will NOT be called but it was";
        assert callCountNotifyOperation == 1 : "Expected only a single notification call but there was " + callCountNotifyOperation + " calls";
        return this;
    }

    public SynchronizationServiceMock assertNotifyFailure() {
        assert wasFailure : "Expected that notifyFailure will be called but it was not";
        return this;
    }

    public SynchronizationServiceMock assertNotifyOperations(int expected) {
        assert callCountNotifyOperation == expected : "Expected " + expected + " notify operations, but was " + callCountNotifyOperation;
        return this;
    }

    public SynchronizationServiceMock assertNotifyChangeCalls(int expected) {
        assert callCountNotifyChange == expected : "Expected " + expected + " notify change calls, but was " + callCountNotifyOperation;
        return this;
    }

    public SynchronizationServiceMock assertSingleNotifyInProgressOnly() {
        assert wasInProgress : "Expected that notifyInProgress will be called but it was not";
        assert !wasSuccess : "Expected that notifySuccess will NOT be called but it was";
        assert !wasFailure : "Expected that notifyFailure will NOT be called but it was";
        assert callCountNotifyOperation == 1 : "Expected only a single notification call but there was " + callCountNotifyOperation + " calls";
        return this;
    }

    public SynchronizationServiceMock assertNoNotifications() {
        assert !wasInProgress : "Expected that notifyInProgress will NOT be called but it was";
        assert !wasSuccess : "Expected that notifySuccess will NOT be called but it was";
        assert !wasFailure : "Expected that notifyFailure will NOT be called but it was";
        assert callCountNotifyOperation == 0 : "Expected no notification call but there was " + callCountNotifyOperation + " calls";
        return this;
    }

    @Override
    public String getName() {
        return "synchronization service mock";
    }

    public void waitForNotifyChange(long timeout) throws InterruptedException {
        long stop = System.currentTimeMillis() + timeout;
        while (System.currentTimeMillis() < stop) {
            if (wasCalledNotifyChange()) {
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("notifyChange has not arrived within " + timeout + " ms");
    }
}
