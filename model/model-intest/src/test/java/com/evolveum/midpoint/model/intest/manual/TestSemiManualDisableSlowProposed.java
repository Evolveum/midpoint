/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.manual;

import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.ucf.impl.builtin.ManualConnectorInstance;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectType;

/**
 * @author Radovan Semancik
 * <p>
 * THIS TEST IS DISABLED MID-4166 (see also {@link #skip()})
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestSemiManualDisableSlowProposed extends TestSemiManualDisable {

    @BeforeMethod
    public void skip() {
        throw new SkipException("Disabled for now");
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initManualConnector();

        // Recompute is not enough. It will not detect that account status is wrong and won't disable the account.
        setConflictResolutionAction(UserType.COMPLEX_TYPE, null, ConflictResolutionActionType.RECONCILE, initResult);
    }

    @Override
    protected String getResourceOid() {
        return RESOURCE_SEMI_MANUAL_DISABLE_SLOW_PROPOSED_OID;
    }

    @Override
    protected File getResourceFile() {
        return RESOURCE_SEMI_MANUAL_DISABLE_SLOW_PROPOSED_FILE;
    }

    @Override
    protected String getRoleOneOid() {
        return ROLE_ONE_SEMI_MANUAL_DISABLE_SLOW_PROPOSED_OID;
    }

    @Override
    protected File getRoleOneFile() {
        return ROLE_ONE_SEMI_MANUAL_DISABLE_SLOW_PROPOSED_FILE;
    }

    @Override
    protected String getRoleTwoOid() {
        return ROLE_TWO_SEMI_MANUAL_DISABLE_SLOW_PROPOSED_OID;
    }

    @Override
    protected File getRoleTwoFile() {
        return ROLE_TWO_SEMI_MANUAL_DISABLE_SLOW_PROPOSED_FILE;
    }

    @Override
    protected boolean nativeCapabilitiesEntered() {
        return false;
    }

    @Override
    protected int getConcurrentTestRandomStartDelayRangeAssign() {
        // Take it extra easy here. We do not have complete atomicity during shadow create.
        // And this resource is really slow.
        // MID-4166
        return 2000;
    }

    // Make the test fast ...
    @Override
    protected int getConcurrentTestRandomStartDelayRangeUnassign() {
        return 3;
    }

    // ... and intense ...
    @Override
    protected int getConcurrentTestNumberOfThreads() {
        return 10;
    }

    @Override
    protected boolean are9xxTestsEnabled() {
        return true;
    }

    // .. and make the resource slow.
    protected void initManualConnector() {
        ManualConnectorInstance.setRandomDelayRange(1000);
    }

    @Override
    protected void assertTest919ShadowRepo(PrismObject<ShadowType> shadowRepo, Task task, OperationResult result) throws Exception {
        ObjectDeltaType disablePendingDelta = null;
        for (PendingOperationType pendingOperation : shadowRepo.asObjectable().getPendingOperation()) {
            ObjectDeltaType delta = pendingOperation.getDelta();
            if (delta.getChangeType() == ChangeTypeType.ADD) {
                ObjectType objectToAdd = delta.getObjectToAdd();
                display("Pending ADD object", objectToAdd.asPrismObject());
            }
            if (isActivationStatusModifyDelta(delta, ActivationStatusType.DISABLED)) {
                if (disablePendingDelta != null) {
                    fail("More than one disable pending delta found:\n" + disablePendingDelta + "\n" + delta);
                }
                disablePendingDelta = delta;
            }
            if (isActivationStatusModifyDelta(delta, ActivationStatusType.ENABLED)) {
                fail("Unexpected enable pending delta found:\n" + delta);
            }
            if (delta.getChangeType() == ChangeTypeType.DELETE) {
                fail("Unexpected delete pending delta found:\n" + disablePendingDelta + "\n" + delta);
            }

        }
        assertNotNull("No disable pending delta", disablePendingDelta);
    }

    @Override
    protected void assertTest919ShadowFuture(PrismObject<ShadowType> shadowModelFuture, Task task,
            OperationResult result) {
        assertShadowNotDead(shadowModelFuture);
        assertAdministrativeStatusDisabled(shadowModelFuture);
    }
}
