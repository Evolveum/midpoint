/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.manual;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;

import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.asserter.ShadowAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * MID-4347
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestSemiManualGrouping extends AbstractGroupingManualResourceTest {

    @Override
    protected BackingStore createBackingStore() {
        return new CsvBackingStore();
    }

    @Override
    protected String getResourceOid() {
        return RESOURCE_SEMI_MANUAL_GROUPING_OID;
    }

    @Override
    protected File getResourceFile() {
        return RESOURCE_SEMI_MANUAL_GROUPING_FILE;
    }

    @Override
    protected String getRoleOneOid() {
        return ROLE_ONE_SEMI_MANUAL_GROUPING_OID;
    }

    @Override
    protected File getRoleOneFile() {
        return ROLE_ONE_SEMI_MANUAL_GROUPING_FILE;
    }

    @Override
    protected String getRoleTwoOid() {
        return ROLE_TWO_SEMI_MANUAL_GROUPING_OID;
    }

    @Override
    protected File getRoleTwoFile() {
        return ROLE_TWO_SEMI_MANUAL_GROUPING_FILE;
    }

    @Override
    protected String getPropagationTaskOid() {
        return TASK_PROPAGATION_MULTI_OID;
    }

    @Override
    protected File getPropagationTaskFile() {
        return TASK_PROPAGATION_MULTI_FILE;
    }

    @Override
    protected boolean hasMultivalueInterests() {
        return false;
    }

    @Override
    protected void assertResourceSchemaBeforeTest(Element resourceXsdSchemaElementBefore) {
        AssertJUnit.assertNull("Resource schema sneaked in before test connection", resourceXsdSchemaElementBefore);
    }

    @Override
    protected int getNumberOfAccountAttributeDefinitions() {
        return 5;
    }

    @Override
    protected void assertShadowPassword(PrismObject<ShadowType> shadow) {
        // CSV password is readable
        PrismProperty<PolyStringType> passValProp = shadow.findProperty(SchemaConstants.PATH_PASSWORD_VALUE);
        assertNotNull("No password value property in "+shadow+": "+passValProp, passValProp);
    }

    /**
     * Create phantom account in the backing store. MidPoint does not know anything about it.
     * At the same time, there is phantom user that has the account assigned. But it is not yet
     * provisioned. MidPoint won't figure out that there is already an account, as the propagation
     * is not executed immediately. The conflict will be discovered only later, when propagation
     * task is run.
     * MID-4614
     */
    @Test
    @Override
    public void test400PhantomAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        setupPhantom();

        // WHEN (mid1)
        when("mid1");
        recomputeUser(USER_PHANTOM_OID, task, result);

        // THEN (mid1)
        then("mid1");
        String caseOid1 = assertInProgress(result);
        displayValue("Case 1", caseOid1);
        // No case OID yet. The case would be created after propagation is run.
        assertNull("Unexpected case 1 OID", caseOid1);

        String shadowOid = assertUser(USER_PHANTOM_OID, "mid1")
            .singleLink()
                .getOid();

        assertModelShadowNoFetch(shadowOid)
            .attributes()
                .assertValue(ATTR_USERNAME_QNAME, USER_PHANTOM_USERNAME)
                .end()
            .pendingOperations()
                .singleOperation()
                    .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTION_PENDING);

        clockForward("PT3M");

        // WHEN (mid2)
        when("mid2");
        // Existing account is detected now. Hence partial error.
        runPropagation(OperationResultStatusType.PARTIAL_ERROR);

        // Synchronization service kicks in, reconciliation detects wrong full name.
        // It tries to fix it. But as we are in propagation mode, the data are not
        // fixed immediately. Instead there is a pending delta to fix the problem.

        // THEN (mid2)
        then("mid2");
        String caseOid2 = assertInProgress(result);
        displayValue("Case 2", caseOid2);
        // No case OID yet. The case will be created after propagation is run.
        assertNull("Unexpected case 2 OID", caseOid2);

        assertUser(USER_PHANTOM_OID, "mid2")
                .displayWithProjections()
                .links()
                    .assertLinks(1, 1)
                    .by()
                        .dead(true)
                    .find()
                        .resolveTarget()
                            .assertTombstone()
                            .pendingOperations()
                                .singleOperation()
                                    .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                                    // Fatal error. Add operation failed. It could not proceed as there was existing account already.
                                    .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                                    .assertHasCompletionTimestamp()
                                    .delta()
                                        .assertAdd()
                                        .end()
                                    .end()
                                .end()
                            .end()
                        .end()
                    .by()
                        .dead(false)
                    .find()
                        .resolveTarget()
                            .assertLive()
                            .pendingOperations()
                                .singleOperation()
                                    .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTION_PENDING)
                                    .delta()
                                        .assertModify()
                                        .assertHasModification(ItemPath.create(ShadowType.F_ATTRIBUTES, new QName(MidPointConstants.NS_RI, "fullname")))
                                        .end()
                                    .end()
                                .end()
                            .end()
                        .end()
                    .end()
                .end();

        clockForward("PT20M");

        // WHEN (final)
        when("final");
        runPropagation();

        // THEN
        then("final");

        String liveShadowOid = assertUser(USER_PHANTOM_OID, "final")
            .displayWithProjections()
            .links()
                .assertLinks(1, 1)
                .by()
                    .dead(true)
                .find()
                    .resolveTarget()
                        .assertTombstone()
                        .pendingOperations()
                            .singleOperation()
                                .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                                .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                                .assertHasCompletionTimestamp()
                                .delta()
                                    .assertAdd()
                                    .end()
                                .end()
                            .end()
                        .end()
                    .end()
                .by()
                    .dead(false)
                .find()
                    .getOid();

        ShadowAsserter<Void> shadowModelAfterAsserter = assertModelShadow(liveShadowOid)
            .assertLive()
            .attributes()
                .assertValue(ATTR_USERNAME_QNAME, USER_PHANTOM_USERNAME)
                .assertValue(ATTR_FULLNAME_QNAME, USER_PHANTOM_FULL_NAME_WRONG)
                .end()
            .pendingOperations()
                .singleOperation()
                    .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                    .assertResultStatus(OperationResultStatusType.IN_PROGRESS)
                    .delta()
                        .assertModify()
                        .end()
                    .end()
                .end();
        assertAttributeFromBackingStore(shadowModelAfterAsserter, ATTR_DESCRIPTION_QNAME, ACCOUNT_PHANTOM_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModelAfterAsserter);

        ShadowAsserter<Void> shadowModelAfterAsserterFuture = assertModelShadowFuture(liveShadowOid)
            .attributes()
                .assertValue(ATTR_USERNAME_QNAME, USER_PHANTOM_USERNAME)
                .assertValue(ATTR_FULLNAME_QNAME, USER_PHANTOM_FULL_NAME)
                .end();
        assertAttributeFromBackingStore(shadowModelAfterAsserterFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_PHANTOM_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModelAfterAsserterFuture);

        // TODO: assert the case

        assertSteadyResources();
    }

    @Override
    Collection<? extends QName> getCachedAttributes() throws SchemaException, ConfigurationException {
        return InternalsConfig.isShadowCachingOnByDefault() ?
                getAccountDefinition().getAttributeNames() :
                getAccountDefinition().getAllIdentifiersNames();
    }
}
