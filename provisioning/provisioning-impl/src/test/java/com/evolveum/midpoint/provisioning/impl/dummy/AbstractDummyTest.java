/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.provisioning.impl.dummy;

import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.icf.dummy.resource.DummyObject;
import com.evolveum.icf.dummy.resource.DummyPrivilege;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.impl.AbstractProvisioningIntegrationTest;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContextFactory;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

/**
 * @author semancik
 */
public abstract class AbstractDummyTest extends AbstractProvisioningIntegrationTest {

    public static final File TEST_DIR_DUMMY = new File("src/test/resources/dummy/");
    protected static final File TEST_DIR = TEST_DIR_DUMMY;

    public static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy.xml");
    public static final String RESOURCE_DUMMY_OID = "ef2bc95b-76e0-59e2-86d6-9999dddddddd";
    public static final String RESOURCE_DUMMY_NS = "http://midpoint.evolveum.com/xml/ns/public/resource/instance/ef2bc95b-76e0-59e2-86d6-9999dddddddd";
    public static final String RESOURCE_DUMMY_INTENT_GROUP = "group";

    protected static final String RESOURCE_DUMMY_NONEXISTENT_OID = "ef2bc95b-000-000-000-009900dddddd";

    protected static final File ACCOUNT_WILL_FILE = new File(TEST_DIR, "account-will.xml");
    protected static final String ACCOUNT_WILL_OID = "c0c010c0-d34d-b44f-f11d-33322212dddd";
    protected static final String ACCOUNT_WILL_USERNAME = "Will";
    protected static final String ACCOUNT_WILL_PASSWORD = "3lizab3th";
    protected static final String ACCOUNT_WILL_PASSWORD_NEW = "3lizab3th123";
    protected static final XMLGregorianCalendar ACCOUNT_WILL_ENABLE_TIMESTAMP = XmlTypeConverter.createXMLGregorianCalendar(2013, 5, 30, 12, 30, 42);

    protected static final File ACCOUNT_ELIZABETH_FILE = new File(TEST_DIR, "account-elizabeth.xml");
    protected static final String ACCOUNT_ELIZABETH_OID = "ca42f312-3bc3-11e7-a32d-73a68a0f363b";
    protected static final String ACCOUNT_ELIZABETH_USERNAME = "elizabeth";

    protected static final String ACCOUNT_DAEMON_USERNAME = "daemon";
    protected static final String ACCOUNT_DAEMON_OID = "c0c010c0-dddd-dddd-dddd-dddddddae604";
    protected static final File ACCOUNT_DAEMON_FILE = new File(TEST_DIR, "account-daemon.xml");

    protected static final String ACCOUNT_DAVIEJONES_USERNAME = "daviejones";

    protected static final File ACCOUNT_MORGAN_FILE = new File(TEST_DIR, "account-morgan.xml");
    protected static final String ACCOUNT_MORGAN_OID = "c0c010c0-d34d-b44f-f11d-444400008888";
    protected static final String ACCOUNT_MORGAN_NAME = "morgan";
    protected static final String ACCOUNT_MORGAN_PASSWORD = "sh1verM3T1mb3rs";

    protected static final File ACCOUNT_LECHUCK_FILE = new File(TEST_DIR, "account-lechuck.xml");
    protected static final String ACCOUNT_LECHUCK_OID = "c0c010c0-d34d-b44f-f11d-444400009aa9";
    protected static final String ACCOUNT_LECHUCK_NAME = "lechuck";

    protected static final File GROUP_PIRATES_FILE = new File(TEST_DIR, "group-pirates.xml");
    protected static final String GROUP_PIRATES_OID = "c0c010c0-d34d-b44f-f11d-3332eeee0000";
    protected static final String GROUP_PIRATES_NAME = "pirates";

    protected static final File PRIVILEGE_PILLAGE_FILE = new File(TEST_DIR, "privilege-pillage.xml");
    protected static final String PRIVILEGE_PILLAGE_OID = "c0c010c0-d34d-b44f-f11d-3332eeff0000";
    protected static final String PRIVILEGE_PILLAGE_NAME = "pillage";

    protected static final File PRIVILEGE_BARGAIN_FILE = new File(TEST_DIR, "privilege-bargain.xml");
    protected static final String PRIVILEGE_BARGAIN_OID = "c0c010c0-d34d-b44f-f11d-3332eeff0001";
    protected static final String PRIVILEGE_BARGAIN_NAME = "bargain";

    protected static final String PRIVILEGE_NONSENSE_NAME = "NoNsEnSe";

    protected static final File ACCOUNT_SCRIPT_FILE = new File(TEST_DIR, "account-script.xml");
    protected static final String ACCOUNT_NEW_SCRIPT_OID = "c0c010c0-d34d-b44f-f11d-33322212abcd";
    protected static final File ENABLE_ACCOUNT_FILE = new File(TEST_DIR, "modify-will-enable.xml");
    protected static final File DISABLE_ACCOUNT_FILE = new File(TEST_DIR, "modify-will-disable.xml");
    protected static final File MODIFY_ACCOUNT_FILE = new File(TEST_DIR, "modify-will-fullname.xml");
    protected static final File SCRIPTS_FILE = new File(TEST_DIR, "scripts.xml");

    protected static final String NOT_PRESENT_OID = "deaddead-dead-dead-dead-deaddeaddead";

    protected static final String OBJECTCLAS_GROUP_LOCAL_NAME = "GroupObjectClass";
    protected static final String OBJECTCLAS_PRIVILEGE_LOCAL_NAME = "CustomprivilegeObjectClass";

    private static final Trace LOGGER = TraceManager.getTrace(AbstractDummyTest.class);

    protected static final QName ASSOCIATION_GROUP_NAME = new QName(RESOURCE_DUMMY_NS, "group");
    protected static final QName ASSOCIATION_PRIV_NAME = new QName(RESOURCE_DUMMY_NS, "priv");

    protected PrismObject<ResourceType> resource;
    protected ResourceType resourceType;
    protected static DummyResource dummyResource;
    protected static DummyResourceContoller dummyResourceCtl;

    protected String accountWillCurrentPassword = ACCOUNT_WILL_PASSWORD;

    @Autowired(required = true)
    protected ProvisioningContextFactory provisioningContextFactory;

    protected String daemonIcfUid;

    @Override
    protected PrismObject<ResourceType> getResource() {
        return resource;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        // We need to switch off the encryption checks. Some values cannot be encrypted as we do
        // not have a definition here
        InternalsConfig.encryptionChecks = false;
        provisioningService.postInit(initResult);
        resource = addResourceFromFile(getResourceDummyFilename(), IntegrationTestTools.DUMMY_CONNECTOR_TYPE, initResult);
        resourceType = resource.asObjectable();

        dummyResourceCtl = DummyResourceContoller.create(null);
        dummyResourceCtl.setResource(resource);
        dummyResourceCtl.extendSchemaPirate();
        dummyResource = dummyResourceCtl.getDummyResource();
        extraDummyResourceInit();

        DummyAccount dummyAccountDaemon = new DummyAccount(ACCOUNT_DAEMON_USERNAME);
        dummyAccountDaemon.setEnabled(true);
        dummyAccountDaemon.addAttributeValues("fullname", "Evil Daemon");
        dummyResource.addAccount(dummyAccountDaemon);
        daemonIcfUid = dummyAccountDaemon.getId();

        PrismObject<ShadowType> shadowDaemon = PrismTestUtil.parseObject(ACCOUNT_DAEMON_FILE);
        if (!isIcfNameUidSame()) {
            setIcfUid(shadowDaemon, dummyAccountDaemon.getId());
        }
        repositoryService.addObject(shadowDaemon, null, initResult);
    }

    protected void extraDummyResourceInit() throws Exception {
        // nothing to do here
    }

    protected void setIcfUid(PrismObject<ShadowType> shadow, String icfUid) {
        PrismProperty<String> icfUidAttr = shadow.findProperty(new ItemPath(ShadowType.F_ATTRIBUTES, SchemaConstants.ICFS_UID));
        icfUidAttr.setRealValue(icfUid);
    }

    protected String getIcfUid(ShadowType shadowType) {
        return getIcfUid(shadowType.asPrismObject());
    }

    @Override
    protected String getIcfUid(PrismObject<ShadowType> shadow) {
        PrismProperty<String> icfUidAttr = shadow.findProperty(new ItemPath(ShadowType.F_ATTRIBUTES, SchemaConstants.ICFS_UID));
        return icfUidAttr.getRealValue();
    }

    protected String getIcfName(PrismObject<ShadowType> shadow) {
        PrismProperty<String> icfUidAttr = shadow.findProperty(new ItemPath(ShadowType.F_ATTRIBUTES, SchemaConstants.ICFS_NAME));
        return icfUidAttr.getRealValue();
    }

    protected File getResourceDummyFilename() {
        return RESOURCE_DUMMY_FILE;
    }

    protected File getAccountWillFile() {
        return ACCOUNT_WILL_FILE;
    }

    protected String transformNameFromResource(String origName) {
        return origName;
    }

    protected String transformNameToResource(String origName) {
        return origName;
    }

    protected boolean supportsActivation() {
        return true;
    }

    protected <T extends ShadowType> void checkConsistency(Collection<PrismObject<T>> shadows) throws SchemaException {
        for (PrismObject<T> shadow : shadows) {
            checkConsistency(shadow);
        }
    }

    protected void checkConsistency(PrismObject<? extends ShadowType> object) throws SchemaException {

        OperationResult result = new OperationResult(TestDummyNegative.class.getName()
            + ".checkConsistency");

        PrismPropertyDefinition itemDef = ShadowUtil.getAttributesContainer(object).getDefinition().findAttributeDefinition(SchemaConstants.ICFS_NAME);

        LOGGER.info("item definition: {}", itemDef.debugDump());
        //TODO: matching rule
        ObjectQuery query = QueryBuilder.queryFor(ShadowType.class, prismContext)
            .itemWithDef(itemDef, ShadowType.F_ATTRIBUTES, itemDef.getName()).eq(getWillRepoIcfName())
            .build();

        System.out.println("Looking for shadows of \"" + getWillRepoIcfName() + "\" with filter "
            + query.debugDump());
        display("Looking for shadows of \"" + getWillRepoIcfName() + "\" with filter "
            + query.debugDump());

        List<PrismObject<ShadowType>> objects = repositoryService.searchObjects(ShadowType.class, query,
            null, result);


        assertEquals("Wrong number of repo shadows for ICF NAME \"" + getWillRepoIcfName() + "\"", 1, objects.size());

    }

    protected <T> void assertAttribute(PrismObject<ShadowType> shadow, String attrName, T... expectedValues) {
        assertAttribute(resource, shadow.asObjectable(), attrName, expectedValues);
    }

    protected <T> void assertAttribute(PrismObject<ShadowType> shadow, QName attrName, T... expectedValues) {
        assertAttribute(resource, shadow.asObjectable(), attrName, expectedValues);
    }

    protected <T> void assertAttribute(PrismObject<ShadowType> shadow, MatchingRule<T> matchingRule, QName attrName, T... expectedValues) throws SchemaException {
        assertAttribute(resource, shadow.asObjectable(), matchingRule, attrName, expectedValues);
    }

    protected <T> void assertNoAttribute(PrismObject<ShadowType> shadow, String attrName) {
        assertNoAttribute(resource, shadow.asObjectable(), attrName);
    }

    protected void assertSchemaSanity(ResourceSchema resourceSchema, ResourceType resourceType) throws Exception {
        dummyResourceCtl.assertDummyResourceSchemaSanityExtended(resourceSchema, resourceType, true);
    }

    protected DummyAccount getDummyAccount(String icfName, String icfUid) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException {
//		if (isNameUnique()) {
        if (isIcfNameUidSame()) {
            return dummyResource.getAccountByUsername(icfName);
        } else {
            return dummyResource.getAccountById(icfUid);
        }
    }

    protected DummyAccount getDummyAccountAssert(String icfName, String icfUid) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException {
//		if (isNameUnique()) {
        if (isIcfNameUidSame()) {
            return dummyResource.getAccountByUsername(icfName);
        } else {
            DummyAccount account = dummyResource.getAccountById(icfUid);
            assertNotNull("No dummy account with ICF UID " + icfUid + " (expected name " + icfName + ")", account);
            assertEquals("Unexpected name in " + account, icfName, account.getName());
            return account;
        }
    }

    protected void assertNoDummyAccount(String icfName, String icfUid) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException {
        DummyAccount account;
        if (isIcfNameUidSame()) {
            account = dummyResource.getAccountByUsername(icfName);
        } else {
            account = dummyResource.getAccountById(icfUid);
        }
        assertNull("Unexpected dummy account with ICF UID " + icfUid + " (name " + icfName + ")", account);
    }

    protected DummyGroup getDummyGroup(String icfName, String icfUid) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException {
//		if (isNameUnique()) {
        if (isIcfNameUidSame()) {
            return dummyResource.getGroupByName(icfName);
        } else {
            return dummyResource.getGroupById(icfUid);
        }
    }

    protected DummyGroup getDummyGroupAssert(String icfName, String icfUid) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException {
//		if (isNameUnique()) {
        if (isIcfNameUidSame()) {
            return dummyResource.getGroupByName(icfName);
        } else {
            DummyGroup group = dummyResource.getGroupById(icfUid);
            assertNotNull("No dummy group with ICF UID " + icfUid + " (expected name " + icfName + ")", group);
            assertEquals("Unexpected name in " + group, icfName, group.getName());
            return group;
        }
    }

    protected DummyPrivilege getDummyPrivilege(String icfName, String icfUid) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException {
//		if (isNameUnique()) {
        if (isIcfNameUidSame()) {
            return dummyResource.getPrivilegeByName(icfName);
        } else {
            return dummyResource.getPrivilegeById(icfUid);
        }
    }

    protected DummyPrivilege getDummyPrivilegeAssert(String icfName, String icfUid) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException {
//		if (isNameUnique()) {
        if (isIcfNameUidSame()) {
            return dummyResource.getPrivilegeByName(icfName);
        } else {
            DummyPrivilege priv = dummyResource.getPrivilegeById(icfUid);
            assertNotNull("No dummy privilege with ICF UID " + icfUid + " (expected name " + icfName + ")", priv);
            assertEquals("Unexpected name in " + priv, icfName, priv.getName());
            return priv;
        }
    }

    protected <T> void assertDummyAccountAttributeValues(String accountName, String accountUid, String attributeName, T... expectedValues) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException {
        DummyAccount dummyAccount = getDummyAccountAssert(accountName, accountUid);
        assertNotNull("No account '" + accountName + "'", dummyAccount);
        assertDummyAttributeValues(dummyAccount, attributeName, expectedValues);
    }

    protected <T> void assertDummyAttributeValues(DummyObject object, String attributeName, T... expectedValues) {
        Set<T> attributeValues = (Set<T>) object.getAttributeValues(attributeName, expectedValues[0].getClass());
        assertNotNull("No attribute " + attributeName + " in " + object.getShortTypeName() + " " + object, attributeValues);
        TestUtil.assertSetEquals("Wrong values of attribute " + attributeName + " in " + object.getShortTypeName() + " " + object, attributeValues, expectedValues);
    }

    protected void assertNoDummyAttribute(DummyObject object, String attributeName) {
        Set<Object> attributeValues = object.getAttributeValues(attributeName, Object.class);
        assertNotNull("Unexpected attribute " + attributeName + " in " + object.getShortTypeName() + " " + object + ": " + attributeValues, attributeValues);
    }

    protected String getWillRepoIcfName() {
        return ACCOUNT_WILL_USERNAME;
    }

    protected boolean isIcfNameUidSame() {
        return true;
    }

    protected boolean isNameUnique() {
        return true;
    }

    protected void assertMember(DummyGroup group, String accountId) {
        IntegrationTestTools.assertGroupMember(group, accountId);
    }

    protected void assertNoMember(DummyGroup group, String accountId) {
        IntegrationTestTools.assertNoGroupMember(group, accountId);
    }

    protected void assertEntitlementGroup(PrismObject<ShadowType> account, String entitlementOid) {
        assertAssociation(account, ASSOCIATION_GROUP_NAME, entitlementOid);
    }

    protected void assertEntitlementPriv(PrismObject<ShadowType> account, String entitlementOid) {
        assertAssociation(account, ASSOCIATION_PRIV_NAME, entitlementOid);
    }

}
