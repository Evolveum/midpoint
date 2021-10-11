/*
 * Copyright (c) 2015-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.Arrays;
import javax.xml.namespace.QName;

import org.apache.directory.api.util.GeneralizedTime;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.Entry;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Unix test with tolerant auxiliary object classes.
 * <p>
 * Also this is using different timestamp format in LDAP connector configuration.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestUnixTolerantAux extends TestUnix {

    protected static final File RESOURCE_OPENDJ_TOLERANT_AUX_FILE = new File(TEST_DIR, "resource-opendj-tolerant-aux.xml");
    protected static final String RESOURCE_OPENDJ_TOLERANT_AUX_OID = "10000000-0000-0000-0000-000000000003";
    private static final String URI_WHATEVER = "http://whatever/";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
    }

    @Override
    protected File getResourceFile() {
        return RESOURCE_OPENDJ_TOLERANT_AUX_FILE;
    }

    @Override
    protected String getResourceOid() {
        return RESOURCE_OPENDJ_TOLERANT_AUX_OID;
    }

    @Override
    protected void assertTest132User(PrismObject<UserType> userAfter) {
        super.assertTest132User(userAfter);
        assertUserAuxes(userAfter, OPENDJ_ACCOUNT_POSIX_AUXILIARY_OBJECTCLASS_NAME);
    }

    @Override
    protected void assertTest132Audit() {
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(3);
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
    }

    @Override
    protected void assertTest135Audit() {
        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertRecords(3);
        dummyAuditService.assertExecutionDeltas(2);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, ShadowType.class);
    }

    @Override
    protected void assertAccountTest136(PrismObject<ShadowType> shadow) throws Exception {
        assertPosixAccount(shadow, USER_LARGO_UID_NUMBER);
    }

    @Override
    protected void assertTest137User(PrismObject<UserType> userAfter) {
        super.assertTest137User(userAfter);
        assertUserAuxes(userAfter, OPENDJ_ACCOUNT_POSIX_AUXILIARY_OBJECTCLASS_NAME);
    }

    @Override
    protected void assertTest137Account(PrismObject<ShadowType> shadow) throws Exception {
        assertPosixAccount(shadow, USER_LARGO_UID_NUMBER);
    }

    @Test
    public void test140AssignUserLargoBasic() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_LARGO_USERNAME);

        // WHEN
        when();
        assignRole(userBefore.getOid(), ROLE_BASIC_OID);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = findUserByUsername(USER_LARGO_USERNAME);
        assertNotNull("No user after", userAfter);
        display("User after", userAfter);
        assertUser(userAfter, USER_LARGO_USERNAME, USER_LARGO_FIST_NAME, USER_LARGO_LAST_NAME);

        accountLargoOid = getSingleLinkOid(userAfter);

        PrismObject<ShadowType> shadow = getShadowModel(accountLargoOid);
        display("Shadow (model)", shadow);
        accountLargoDn = assertBasicAccount(shadow);
    }

    /**
     * Modify the account directly on resource: add aux object class, add the
     * attributes. Then reconcile the user. The recon should leave the aux object
     * class untouched.
     */
    @Test
    public void test142MeddleWithAccountAndReconcileUserLargo() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_LARGO_USERNAME);

        openDJController.executeLdifChange(
                "dn: " + accountLargoDn + "\n" +
                        "changetype: modify\n" +
                        "add: objectClass\n" +
                        "objectClass: " + OPENDJ_ACCOUNT_LABELED_URI_OBJECT_AUXILIARY_OBJECTCLASS_NAME.getLocalPart() + "\n" +
                        "-\n" +
                        "add: labeledURI\n" +
                        "labeledURI: " + URI_WHATEVER + "\n"
        );

        Entry entryBefore = openDJController.fetchEntry(accountLargoDn);
        display("Entry before", entryBefore);

        dummyAuditService.clear();

        // WHEN
        when();
        reconcileUser(userBefore.getOid(), task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = findUserByUsername(USER_LARGO_USERNAME);
        assertNotNull("No user after", userAfter);
        display("User after", userAfter);
        assertUserPosix(userAfter, USER_LARGO_USERNAME, USER_LARGO_FIST_NAME, USER_LARGO_LAST_NAME, USER_LARGO_UID_NUMBER);
        assertUserAuxes(userAfter, OPENDJ_ACCOUNT_LABELED_URI_OBJECT_AUXILIARY_OBJECTCLASS_NAME);

        String accountOid = getSingleLinkOid(userAfter);

        PrismObject<ShadowType> shadow = getShadowModel(accountOid);
        display("Shadow (model)", shadow);
        assertAccount(shadow, OPENDJ_ACCOUNT_LABELED_URI_OBJECT_AUXILIARY_OBJECTCLASS_NAME);
        assertLabeledUri(shadow, URI_WHATEVER);

        // TODO: check audit
    }

    @Test
    public void test144AssignUserLargoUnix() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_LARGO_USERNAME);

        // WHEN
        when();
        assignRole(userBefore.getOid(), ROLE_UNIX_OID);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = findUserByUsername(USER_LARGO_USERNAME);
        assertNotNull("No user after", userAfter);
        display("User after", userAfter);
        assertUserPosix(userAfter, USER_LARGO_USERNAME, USER_LARGO_FIST_NAME, USER_LARGO_LAST_NAME, USER_LARGO_UID_NUMBER);
        assertUserAuxes(userAfter, OPENDJ_ACCOUNT_POSIX_AUXILIARY_OBJECTCLASS_NAME, OPENDJ_ACCOUNT_LABELED_URI_OBJECT_AUXILIARY_OBJECTCLASS_NAME);

        String accountOid = getSingleLinkOid(userAfter);

        PrismObject<ShadowType> shadow = getShadowModel(accountOid);
        display("Shadow (model)", shadow);
        assertAccount(shadow, OPENDJ_ACCOUNT_POSIX_AUXILIARY_OBJECTCLASS_NAME, OPENDJ_ACCOUNT_LABELED_URI_OBJECT_AUXILIARY_OBJECTCLASS_NAME);
        assertLabeledUri(shadow, URI_WHATEVER);
    }

    @Test
    public void test146UnassignUserLargoUnix() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_LARGO_USERNAME);

        // WHEN
        when();
        unassignRole(userBefore.getOid(), ROLE_UNIX_OID);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = findUserByUsername(USER_LARGO_USERNAME);
        assertNotNull("No user after", userAfter);
        display("User after", userAfter);
        assertUserPosix(userAfter, USER_LARGO_USERNAME, USER_LARGO_FIST_NAME, USER_LARGO_LAST_NAME, USER_LARGO_UID_NUMBER);
        assertUserAuxes(userAfter, OPENDJ_ACCOUNT_LABELED_URI_OBJECT_AUXILIARY_OBJECTCLASS_NAME);

        String accountOid = getSingleLinkOid(userAfter);

        PrismObject<ShadowType> shadow = getShadowModel(accountOid);
        display("Shadow (model)", shadow);
        assertAccount(shadow, OPENDJ_ACCOUNT_LABELED_URI_OBJECT_AUXILIARY_OBJECTCLASS_NAME);
        assertLabeledUri(shadow, URI_WHATEVER);
    }

    @Test
    public void test149UnAssignUserLargoBasic() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_LARGO_USERNAME);

        // WHEN
        when();
        unassignRole(userBefore.getOid(), ROLE_BASIC_OID);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = findUserByUsername(USER_LARGO_USERNAME);
        assertNotNull("No user after", userAfter);
        display("User after", userAfter);
        assertUserPosix(userAfter, USER_LARGO_USERNAME, USER_LARGO_FIST_NAME, USER_LARGO_LAST_NAME, USER_LARGO_UID_NUMBER);
        assertLinks(userAfter, 0);

        assertNoObject(ShadowType.class, accountLargoOid, task, result);

        openDJController.assertNoEntry(accountLargoDn);
    }

    // The assignment was disabled in the repository. There was no change that went through
    // model. MidPoint won't remove the aux object class.
    @Override
    protected void assertAccountTest510(PrismObject<ShadowType> shadow) throws Exception {
        assertPosixAccount(shadow, null);
        assertGroupAssociation(shadow, groupMonkeyIslandOid);
    }

    @Override
    protected Long getTimestampAttribute(PrismObject<ShadowType> shadow) throws Exception {
        String attributeValue = ShadowUtil.getAttributeValue(shadow, OPENDJ_MODIFY_TIMESTAMP_ATTRIBUTE_QNAME);
        if (attributeValue == null) {
            return null;
        }
        if (!attributeValue.endsWith("Z")) {
            fail("Non-zulu timestamp: " + attributeValue);
        }
        GeneralizedTime gt = new GeneralizedTime(attributeValue);
        return gt.getCalendar().getTimeInMillis();
    }

    private void assertUserAuxes(PrismObject<UserType> userAfter, QName... expectedAuxClasses) {
        PrismAsserts.assertPropertyValue(userAfter, UserType.F_ORGANIZATIONAL_UNIT,
                Arrays.stream(expectedAuxClasses).map(x -> createPolyString(x.getLocalPart())).toArray(PolyString[]::new));
    }

    private void assertLabeledUri(PrismObject<ShadowType> shadow, String expecteduri) throws DirectoryException {
        //noinspection ConstantConditions
        String dn = (String) ShadowUtil.getSecondaryIdentifiers(shadow).iterator().next().getRealValue();

        Entry entry = openDJController.fetchEntry(dn);
        assertNotNull("No ou LDAP entry for " + dn, entry);
        display("Posix account entry", entry);
        OpenDJController.assertAttribute(entry, OPENDJ_LABELED_URI_ATTRIBUTE_NAME, expecteduri);
    }
}
