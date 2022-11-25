/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertNotNull;

import static com.evolveum.midpoint.model.api.util.ReferenceResolver.Source.REPOSITORY;
import static com.evolveum.midpoint.schema.constants.MidPointConstants.NS_RI;
import static com.evolveum.midpoint.test.util.MidPointTestConstants.QNAME_UID;

import java.io.File;
import java.util.List;
import javax.xml.namespace.QName;

import org.opends.server.types.DirectoryException;
import org.opends.server.types.Entry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.icf.dummy.resource.DummyPrivilege;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.model.impl.sync.tasks.recon.DebugReconciliationResultListener;
import com.evolveum.midpoint.model.impl.sync.tasks.recon.ReconciliationActivityHandler;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests MID-2422, among others.
 *
 * Tests 2xx are related to MID-7966.
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestUniversity extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "university");

    public static final File OBJECT_TEMPLATE_ORG_FILE = new File(TEST_DIR, "object-template-org.xml");
    public static final String OBJECT_TEMPLATE_ORG_OID = "10000000-0000-0000-0000-000000000231";

    protected static final File RESOURCE_DUMMY_HR_FILE = new File(TEST_DIR, "resource-dummy-hr.xml");
    protected static final String RESOURCE_DUMMY_HR_ID = "HR";
    protected static final String RESOURCE_DUMMY_HR_OID = "10000000-0000-0000-0000-000000000001";

    protected static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj.xml");
    protected static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";

    private static final String DUMMY_PRIVILEGE_ATTRIBUTE_HR_ORGPATH = "orgpath";

    public static final File ORG_TOP_FILE = new File(TEST_DIR, "org-top.xml");
    public static final String ORG_TOP_OID = "00000000-8888-6666-0000-100000000001";

    public static final File ROLE_META_ORG_FILE = new File(TEST_DIR, "role-meta-org.xml");
    public static final String ROLE_META_ORG_OID = "10000000-0000-0000-0000-000000006601";

    protected static final File TASK_LIVE_SYNC_DUMMY_HR_FILE = new File(TEST_DIR, "task-dummy-hr-livesync.xml");
    protected static final String TASK_LIVE_SYNC_DUMMY_HR_OID = "10000000-0000-0000-5555-555500000001";

    @Autowired private ReconciliationActivityHandler reconciliationActivityHandler;

    protected static DummyResource dummyResourceHr;
    protected static DummyResourceContoller dummyResourceCtlHr;
    protected ResourceType resourceDummyHrType;
    protected PrismObject<ResourceType> resourceDummyHr;

    protected ResourceType resourceOpenDjType;
    protected PrismObject<ResourceType> resourceOpenDj;

    @Override
    protected String getTopOrgOid() {
        return ORG_TOP_OID;
    }

    @Override
    protected void startResources() throws Exception {
        openDJController.startCleanServerRI();
    }

    @AfterClass
    public static void stopResources() {
        openDJController.stop();
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        DebugReconciliationResultListener reconciliationTaskResultListener = new DebugReconciliationResultListener();
        reconciliationActivityHandler.setReconciliationResultListener(reconciliationTaskResultListener);

        // Resources
        dummyResourceCtlHr = DummyResourceContoller.create(RESOURCE_DUMMY_HR_ID, resourceDummyHr);
        DummyObjectClass privilegeObjectClass = dummyResourceCtlHr.getDummyResource().getPrivilegeObjectClass();
        dummyResourceCtlHr.addAttrDef(privilegeObjectClass, DUMMY_PRIVILEGE_ATTRIBUTE_HR_ORGPATH, String.class, false, false);
        dummyResourceHr = dummyResourceCtlHr.getDummyResource();
        resourceDummyHr = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_HR_FILE, RESOURCE_DUMMY_HR_OID, initTask, initResult);
        resourceDummyHrType = resourceDummyHr.asObjectable();
        dummyResourceCtlHr.setResource(resourceDummyHr);
        dummyResourceHr.setSyncStyle(DummySyncStyle.SMART);

        resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, RESOURCE_OPENDJ_FILE, RESOURCE_OPENDJ_OID, initTask, initResult);
        resourceOpenDjType = resourceOpenDj.asObjectable();
        openDJController.setResource(resourceOpenDj);

        // Object Templates
        importObjectFromFile(OBJECT_TEMPLATE_ORG_FILE, initResult);
        setDefaultObjectTemplate(OrgType.COMPLEX_TYPE, OBJECT_TEMPLATE_ORG_OID);

        // Org
        importObjectFromFile(ORG_TOP_FILE, initResult);

        // Role
        importObjectFromFile(ROLE_META_ORG_FILE, initResult);

        // Tasks
        importObjectFromFile(TASK_LIVE_SYNC_DUMMY_HR_FILE, initResult);
    }

    @Test
    public void test000Sanity() throws Exception {
        Task task = getTestTask();

        OperationResult testResultHr = modelService.testResource(RESOURCE_DUMMY_HR_OID, task, task.getResult());
        TestUtil.assertSuccess(testResultHr);

        OperationResult testResultOpenDj = modelService.testResource(RESOURCE_OPENDJ_OID, task, task.getResult());
        TestUtil.assertSuccess(testResultOpenDj);

        waitForTaskStart(TASK_LIVE_SYNC_DUMMY_HR_OID, false);

        dumpOrgTree();
    }

    @Test
    public void test100AddComeniusUniversity() throws Exception {
        DummyPrivilege comenius = new DummyPrivilege("UK");

        // WHEN
        dummyResourceHr.addPrivilege(comenius);
        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_HR_OID, true);

        // THEN
        PrismObject<OrgType> org = getAndAssertFunctionalOrg("UK");
        assertNotNull("Comenius University was not found", org);
        display("Org", org);

        dumpOrgTree();

        assertHasOrg(org, ORG_TOP_OID);
        assertAssignedOrg(org, ORG_TOP_OID);
        assertSubOrgs(org, 0);
        assertSubOrgs(ORG_TOP_OID, 1);
    }

    @Test
    public void test110AddComeniusStructure() throws Exception {
        DummyPrivilege srcFmfi = new DummyPrivilege("FMFI");
        srcFmfi.addAttributeValue(DUMMY_PRIVILEGE_ATTRIBUTE_HR_ORGPATH, "UK");

        DummyPrivilege srcVc = new DummyPrivilege("VC");
        srcVc.addAttributeValue(DUMMY_PRIVILEGE_ATTRIBUTE_HR_ORGPATH, "UK:FMFI");

        DummyPrivilege srcPrif = new DummyPrivilege("PRIF");
        srcPrif.addAttributeValue(DUMMY_PRIVILEGE_ATTRIBUTE_HR_ORGPATH, "UK");

        // WHEN
        dummyResourceHr.addPrivilege(srcFmfi);
        dummyResourceHr.addPrivilege(srcVc);
        dummyResourceHr.addPrivilege(srcPrif);
        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_HR_OID, true);

        // THEN
        dumpOrgTree();

        PrismObject<OrgType> uk = getAndAssertFunctionalOrg("UK");
        assertNotNull("UK was not found", uk);
        display("Org UK", uk);

        assertHasOrg(uk, ORG_TOP_OID);
        assertAssignedOrg(uk, ORG_TOP_OID);
        assertSubOrgs(uk, 2);
        assertSubOrgs(ORG_TOP_OID, 1);
        assertGroupMembers(uk, "cn=DL-FMFI,ou=FMFI,ou=UK,dc=example,dc=com", "cn=DL-PRIF,ou=PRIF,ou=UK,dc=example,dc=com");

        PrismObject<OrgType> fmfi = getAndAssertFunctionalOrg("FMFI");
        assertNotNull("FMFI was not found", fmfi);
        display("Org FMFI", fmfi);

        assertHasOrg(fmfi, uk.getOid());
        assertAssignedOrg(fmfi, uk.getOid());
        assertSubOrgs(fmfi, 1);
        assertGroupMembers(fmfi, "cn=DL-VC,ou=VC,ou=FMFI,ou=UK,dc=example,dc=com");

        PrismObject<OrgType> prif = getAndAssertFunctionalOrg("PRIF");
        assertNotNull("PRIF was not found", prif);
        display("Org PRIF", prif);

        assertHasOrg(prif, uk.getOid());
        assertAssignedOrg(prif, uk.getOid());
        assertSubOrgs(prif, 0);
        assertNoGroupMembers(prif);

        PrismObject<OrgType> vc = getAndAssertFunctionalOrg("VC");
        assertNotNull("VC was not found", vc);
        display("Org VC", vc);

        assertHasOrg(vc, fmfi.getOid());
        assertAssignedOrg(vc, fmfi.getOid());
        assertSubOrgs(vc, 0);
        assertNoGroupMembers(vc);
    }

    /** MID-7966 */
    @Test
    public void test200FetchJohn() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a group 'group200'");
        openDJController.addEntry(
                "dn: cn=group200,dc=example,dc=com\n" +
                "objectClass: groupOfUniqueNames\n" +
                "cn: group200");

        and("a user 'john' in that group");
        openDJController.addEntry(
                "dn: uid=john,dc=example,dc=com\n" +
                        "objectClass: inetOrgPerson\n" +
                        "uid: john\n" +
                        "cn: john\n" +
                        "sn: Smith\n" +
                        "departmentNumber: group200");

        when("'john' is fetched");
        List<PrismObject<ShadowType>> accounts = modelService.searchObjects(
                ShadowType.class,
                accountDefaultObjectsQuery(resourceOpenDjType, QNAME_UID, "john"),
                null, task, result);

        then("account was found");
        assertSuccess(result);
        assertThat(accounts).as("accounts").hasSize(1);
        PrismObject<ShadowType> account = accounts.get(0);
        displayDumpable("account of john", account);

        and("it has the correct association");
        List<ShadowAssociationType> associations = account.asObjectable().getAssociation();
        assertThat(associations).as("associations").hasSize(1);
        ShadowAssociationType association = associations.get(0);
        assertThat(association.getName()).as("association name").isEqualTo(new QName(NS_RI, "department"));
        List<PrismObject<? extends ObjectType>> targets =
                referenceResolver.resolve(association.getShadowRef(), null, REPOSITORY, null, task, result);
        assertThat(targets).as("association targets").hasSize(1);
        PrismObject<? extends ObjectType> target = targets.get(0);
        assertThat(target.asObjectable().getName().getOrig())
                .as("association target name")
                .isEqualTo("cn=group200,dc=example,dc=com");
    }

    private void assertGroupMembers(PrismObject<OrgType> org, String... members) throws Exception {
        String groupOid = getLinkRefOid(org, RESOURCE_OPENDJ_OID, ShadowKindType.ENTITLEMENT, "org-group");
        PrismObject<ShadowType> groupShadow = getShadowModel(groupOid);
        assertAttribute(groupShadow.asObjectable(), new QName(NS_RI, "uniqueMember"), members);
    }

    private void assertNoGroupMembers(PrismObject<OrgType> org) throws Exception {
        String groupOid = getLinkRefOid(org, RESOURCE_OPENDJ_OID, ShadowKindType.ENTITLEMENT, "org-group");
        PrismObject<ShadowType> groupShadow = getShadowModel(groupOid);
        assertNoAttribute(groupShadow.asObjectable(), new QName(NS_RI, "uniqueMember"));
    }

    @Test
    public void test120MoveComputingCentre() throws Exception {
        DummyPrivilege srcVc = dummyResourceHr.getPrivilegeByName("VC");

        // WHEN
        srcVc.replaceAttributeValue(DUMMY_PRIVILEGE_ATTRIBUTE_HR_ORGPATH, "UK:PRIF");
        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_HR_OID, true, 999999999);

        // THEN
        dumpOrgTree();

        PrismObject<OrgType> uk = getAndAssertFunctionalOrg("UK");
        assertNotNull("UK was not found", uk);
        display("Org UK", uk);

        assertHasOrg(uk, ORG_TOP_OID);
        assertAssignedOrg(uk, ORG_TOP_OID);
        assertSubOrgs(uk, 2);
        assertSubOrgs(ORG_TOP_OID, 1);
        assertGroupMembers(uk, "cn=DL-FMFI,ou=FMFI,ou=UK,dc=example,dc=com", "cn=DL-PRIF,ou=PRIF,ou=UK,dc=example,dc=com");

        PrismObject<OrgType> fmfi = getAndAssertFunctionalOrg("FMFI");
        assertNotNull("FMFI was not found", fmfi);
        display("Org FMFI", fmfi);

        assertHasOrg(fmfi, uk.getOid());
        assertAssignedOrg(fmfi, uk.getOid());
        assertSubOrgs(fmfi, 0);
        assertNoGroupMembers(fmfi);

        PrismObject<OrgType> prif = getAndAssertFunctionalOrg("PRIF");
        assertNotNull("PRIF was not found", prif);
        display("Org PRIF", prif);

        assertHasOrg(prif, uk.getOid());
        assertAssignedOrg(prif, uk.getOid());
        assertSubOrgs(prif, 1);
        assertGroupMembers(prif, "cn=DL-VC,ou=VC,ou=PRIF,ou=UK,dc=example,dc=com");

        PrismObject<OrgType> vc = getAndAssertFunctionalOrg("VC");
        assertNotNull("VC was not found", vc);
        display("Org VC", vc);

        assertHasOrg(vc, prif.getOid());
        assertAssignedOrg(vc, prif.getOid());
        assertSubOrgs(vc, 0);
        assertNoGroupMembers(vc);
    }

    private PrismObject<OrgType> getAndAssertFunctionalOrg(String orgName) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, DirectoryException, ExpressionEvaluationException {
        PrismObject<OrgType> org = getOrg(orgName);
        PrismAsserts.assertPropertyValue(org, OrgType.F_SUBTYPE, "functional");
        assertAssignedRole(org, ROLE_META_ORG_OID);

        String ouOid = getLinkRefOid(org, RESOURCE_OPENDJ_OID, ShadowKindType.GENERIC, "org-ou");
        PrismObject<ShadowType> ouShadow = getShadowModel(ouOid);
        display("Org " + orgName + " OU shadow", ouShadow);
        // TODO assert shadow content

        String groupOid = getLinkRefOid(org, RESOURCE_OPENDJ_OID, ShadowKindType.ENTITLEMENT, "org-group");
        PrismObject<ShadowType> groupShadow = getShadowModel(groupOid);
        display("Org " + orgName + " group shadow", groupShadow);
        // TODO assert shadow content

        Entry ouEntry = openDJController.searchSingle("ou=" + orgName);
        assertNotNull("No ou LDAP entry for " + orgName, ouEntry);
        display("OU entry", ouEntry);
        OpenDJController.assertObjectClass(ouEntry, "organizationalUnit");

        Entry groupEntry = openDJController.searchSingle("cn=DL-" + orgName);
        assertNotNull("No group LDAP entry for " + orgName, groupEntry);
        display("OU GROUP entry", groupEntry);
        OpenDJController.assertObjectClass(groupEntry, "groupOfUniqueNames");

        return org;
    }

}
