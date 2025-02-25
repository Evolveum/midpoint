/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_GROUP_OBJECT_CLASS;

import static com.evolveum.midpoint.test.DummyResourceContoller.PIRATE_SCHEMA_NUMBER_OF_DEFINITIONS;

import static java.util.Objects.requireNonNull;
import static org.testng.AssertJUnit.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.*;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.test.*;

import com.evolveum.midpoint.test.asserter.RepoShadowAsserter;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.icf.dummy.connector.DummyConnector;
import com.evolveum.icf.dummy.resource.*;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.impl.AbstractProvisioningIntegrationTest;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContextFactory;
import com.evolveum.midpoint.provisioning.impl.ProvisioningTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.asserter.DummyAccountAsserter;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;

/**
 * @author semancik
 */
public abstract class AbstractDummyTest extends AbstractProvisioningIntegrationTest {

    public static final File TEST_DIR_DUMMY = new File("src/test/resources/dummy/");
    protected static final File TEST_DIR = TEST_DIR_DUMMY;

    public static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy.xml");
    public static final String RESOURCE_DUMMY_OID = "ef2bc95b-76e0-59e2-86d6-9999dddddddd";
    static final String RESOURCE_DUMMY_INTENT_GROUP = "group";

    static final String RESOURCE_DUMMY_NONEXISTENT_OID = "ef2bc95b-000-000-000-009900dddddd";

    protected static final File ACCOUNT_WILL_FILE = new File(TEST_DIR, "account-will.xml");
    protected static final String ACCOUNT_WILL_OID = "c0c010c0-d34d-b44f-f11d-33322212dddd";
    protected static final String ACCOUNT_WILL_USERNAME = "Will";
    static final String ACCOUNT_WILL_PASSWORD = "3lizab3th";
    static final String ACCOUNT_WILL_PASSWORD_123 = "3lizab3th123";
    static final String ACCOUNT_WILL_PASSWORD_321 = "3lizab3th321";
    static final XMLGregorianCalendar ACCOUNT_WILL_ENABLE_TIMESTAMP = XmlTypeConverter.createXMLGregorianCalendar(2013, 5, 30, 12, 30, 42);

    static final File ACCOUNT_ELIZABETH_FILE = new File(TEST_DIR, "account-elizabeth.xml");
    static final String ACCOUNT_ELIZABETH_OID = "ca42f312-3bc3-11e7-a32d-73a68a0f363b";
    static final String ACCOUNT_ELIZABETH_USERNAME = "elizabeth";
    static final String ACCOUNT_ELIZABETH_FULLNAME = "Elizabeth Swan";

    static final String ACCOUNT_DAEMON_USERNAME = "daemon";
    static final String ACCOUNT_DAEMON_OID = "c0c010c0-dddd-dddd-dddd-dddddddae604";
    private static final File ACCOUNT_DAEMON_FILE = new File(TEST_DIR, "account-daemon.xml");

    static final String ACCOUNT_RELIC_USERNAME = "relic";
    static final String ACCOUNT_RELIC_OID = "3689fda4-5c4c-11e9-b144-43a245ea74a9";
    static final File ACCOUNT_RELIC_FILE = new File(TEST_DIR, "account-relic.xml");

    static final String ACCOUNT_DAVIEJONES_USERNAME = "daviejones";

    protected static final File ACCOUNT_MORGAN_FILE = new File(TEST_DIR, "account-morgan.xml");
    protected static final String ACCOUNT_MORGAN_OID = "c0c010c0-d34d-b44f-f11d-444400008888";
    static final String ACCOUNT_MORGAN_NAME = "morgan";
    static final String ACCOUNT_CPTMORGAN_NAME = "cptmorgan";
    static final String ACCOUNT_MORGAN_FULLNAME = "Captain Morgan";
    static final String ACCOUNT_MORGAN_PASSWORD = "sh1verM3T1mb3rs";
    static final String ACCOUNT_MORGAN_PASSWORD_ENLIST_TIMESTAMP = "1663-05-30T14:15:16Z";
    static final String ACCOUNT_MORGAN_PASSWORD_ENLIST_TIMESTAMP_MODIFIED = "1666-07-08T09:10:11Z";

    static final File ACCOUNT_LECHUCK_FILE = new File(TEST_DIR, "account-lechuck.xml");
    static final String ACCOUNT_LECHUCK_OID = "c0c010c0-d34d-b44f-f11d-444400009aa9";
    static final String ACCOUNT_LECHUCK_NAME = "lechuck";

    static final File ACCOUNT_WALLY_FILE = new File(TEST_DIR, "account-wally.xml");

    static final File GROUP_PIRATES_FILE = new File(TEST_DIR, "group-pirates.xml");
    static final String GROUP_PIRATES_OID = "c0c010c0-d34d-b44f-f11d-3332eeee0000";
    static final String GROUP_PIRATES_NAME = "pirates";

    static final File PRIVILEGE_PILLAGE_FILE = new File(TEST_DIR, "privilege-pillage.xml");
    static final String PRIVILEGE_PILLAGE_OID = "c0c010c0-d34d-b44f-f11d-3332eeff0000";
    static final String PRIVILEGE_PILLAGE_NAME = "pillage";

    static final File PRIVILEGE_BARGAIN_FILE = new File(TEST_DIR, "privilege-bargain.xml");
    static final String PRIVILEGE_BARGAIN_OID = "c0c010c0-d34d-b44f-f11d-3332eeff0001";
    static final String PRIVILEGE_BARGAIN_NAME = "bargain";

    static final String PRIVILEGE_NONSENSE_NAME = "NoNsEnSe";

    static final File ACCOUNT_SCRIPT_FILE = new File(TEST_DIR, "account-script.xml");
    static final String ACCOUNT_NEW_SCRIPT_OID = "c0c010c0-d34d-b44f-f11d-33322212abcd";
    static final File MODIFY_WILL_FULLNAME_FILE = new File(TEST_DIR, "modify-will-fullname.xml");
    static final File SCRIPTS_FILE = new File(TEST_DIR, "scripts.xml");

    static final String NOT_PRESENT_OID = "deaddead-dead-dead-dead-deaddeaddead";

    static final String OBJECTCLASS_GROUP_LOCAL_NAME = "GroupObjectClass";
    static final String OBJECTCLASS_PRIVILEGE_LOCAL_NAME = "CustomprivilegeObjectClass";

    protected static final QName ASSOCIATION_GROUP_NAME = new QName(MidPointConstants.NS_RI, "group");
    static final QName ASSOCIATION_PRIV_NAME = new QName(MidPointConstants.NS_RI, "priv");

    protected PrismObject<ResourceType> resource;
    protected ResourceType resourceBean;
    /** True if the resource was successfully tested and {@link #resource} and {@link #resourceBean} contain complete schema. */
    boolean resourceInitialized;
    static boolean resourceShutDown;
    protected static DummyResource dummyResource;
    protected static DummyResourceContoller dummyResourceCtl;

    String accountWillCurrentPassword = ACCOUNT_WILL_PASSWORD;

    private static final TestObject<ArchetypeType> ARCHETYPE_OBJECT_MARK = TestObject.classPath(
            "initial-objects/archetype", "701-archetype-object-mark.xml", SystemObjectsType.ARCHETYPE_OBJECT_MARK.value());

    private static final TestObject<ArchetypeType> ARCHETYPE_SHADOW_POLICY_MARK = TestObject.classPath(
            "initial-objects/archetype", "705-archetype-shadow-policy-mark.xml",
            SystemObjectsType.ARCHETYPE_SHADOW_POLICY_MARK.value());

    private static final TestObject<MarkType> MARK_PROTECTED_SHADOW = TestObject.classPath(
            "initial-objects/mark", "800-mark-protected.xml",
            SystemObjectsType.MARK_PROTECTED.value());

    static final TestObject<MarkType> MARK_INVALID_DATA = TestObject.classPath(
            "initial-objects/mark", "804-mark-invalid-data.xml",
            SystemObjectsType.MARK_INVALID_DATA.value());

    @Autowired
    protected ProvisioningContextFactory provisioningContextFactory;

    String daemonIcfUid;

    @Override
    protected PrismObject<ResourceType> getResource() {
        return resource;
    }

    protected boolean areReferencesSupportedNatively() {
        return false;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        resource = addResourceFromFile(getResourceDummyFile(), getDummyConnectorType(), initResult);
        resourceBean = resource.asObjectable();

        dummyResourceCtl = DummyResourceContoller.create(null);
        dummyResourceCtl.setResource(resource);
        dummyResourceCtl.extendSchemaPirate();
        dummyResource = dummyResourceCtl.getDummyResource();
        extraDummyResourceInit();

        if (areMarksSupported()) {
            repoAdd(ARCHETYPE_OBJECT_MARK, initResult);
            repoAdd(ARCHETYPE_SHADOW_POLICY_MARK, initResult);
            repoAdd(MARK_PROTECTED_SHADOW, initResult);
            repoAdd(MARK_INVALID_DATA, initResult);
        }
    }

    /**
     * Not called during the initialization, because the resource is not yet configured.
     * We need the definition to create the repo shadow.
     */
    void addAccountDaemon(OperationResult result) throws Exception {
        DummyAccount dummyAccountDaemon = new DummyAccount(ACCOUNT_DAEMON_USERNAME);
        dummyAccountDaemon.setEnabled(true);
        dummyAccountDaemon.addAttributeValues("fullname", "Evil Daemon");
        dummyResource.addAccount(dummyAccountDaemon);
        daemonIcfUid = dummyAccountDaemon.getId();

        PrismObject<ShadowType> shadowDaemon = PrismTestUtil.parseObject(ACCOUNT_DAEMON_FILE);
        if (!isIcfNameUidSame()) {
            setIcfUid(shadowDaemon, dummyAccountDaemon.getId());
        }
        convertAttributesToRepoFormat(shadowDaemon);
        repositoryService.addObject(shadowDaemon, null, result);
    }

    protected String getDummyConnectorType() {
        return IntegrationTestTools.DUMMY_CONNECTOR_TYPE;
    }

    protected Class<?> getDummyConnectorClass() {
        return DummyConnector.class;
    }

    protected void extraDummyResourceInit() throws Exception {
        // nothing to do here
    }

    private void setIcfUid(PrismObject<ShadowType> shadow, String icfUid) {
        PrismProperty<String> icfUidAttr = shadow.findProperty(SchemaConstants.ICFS_UID_PATH);
        icfUidAttr.setRealValue(icfUid);
    }

    void convertAttributesToRepoFormat(PrismObject<ShadowType> shadow) throws SchemaException, ConfigurationException {
        PrismContainer<ShadowAttributesType> origAttrContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
        var attrContainerClone = origAttrContainer.clone();
        var objectDef = Resource.of(resourceBean)
                .getCompleteSchemaRequired()
                .findDefinitionForObjectClassRequired(shadow.asObjectable().getObjectClass());
        attrContainerClone.applyDefinition(objectDef.toShadowAttributesContainerDefinition());

        PrismContainerValue<ShadowAttributesType> origAttrContainerValue = origAttrContainer.getValue();
        origAttrContainerValue.clear();
        for (Item<?, ?> clonedItem : attrContainerClone.getValue().getItems()) {
            origAttrContainerValue.add(
                    objectDef.findSimpleAttributeDefinitionRequired(clonedItem.getElementName())
                            .toNormalizationAware()
                            .adoptRealValuesAndInstantiate(clonedItem.getRealValues()));
        }
    }

    String getIcfUid(RawRepoShadow shadow) {
        return getIcfUid(shadow.getPrismObject());
    }

    String getIcfUid(AbstractShadow shadow) {
        return getIcfUid(shadow.getPrismObject());
    }

    @Override
    protected String getIcfUid(PrismObject<ShadowType> shadow) {
        Object value = shadow.findProperty(SchemaConstants.ICFS_UID_PATH).getRealValue();
        return value instanceof PolyString polyString ? polyString.getOrig() : (String) value;
    }

    protected File getResourceDummyFile() {
        return RESOURCE_DUMMY_FILE;
    }

    protected File getAccountWillFile() {
        return ACCOUNT_WILL_FILE;
    }

    protected String getWillNameOnResource() {
        return transformNameToResource(ACCOUNT_WILL_USERNAME);
    }

    protected String transformNameToResource(String origName) {
        return origName;
    }

    protected boolean supportsActivation() {
        return true;
    }

    protected boolean supportsMemberOf() {
        return false;
    }

    protected void checkUniqueness(Collection<? extends AbstractShadow> shadows) throws SchemaException {
        for (AbstractShadow shadow : shadows) {
            checkUniqueness(shadow);
        }
    }

    protected void checkUniqueness(AbstractShadow object) throws SchemaException {
        checkUniqueness(object, false);
    }

    protected void checkUniqueness(AbstractShadow object, boolean liveOnly) throws SchemaException {
        OperationResult result = createOperationResult("checkUniqueness");
        var nameAttr = object.getSimpleAttributeRequired(SchemaConstants.ICFS_NAME);
        var q = prismContext.queryFor(ShadowType.class)
                .item(ShadowType.F_RESOURCE_REF).ref(object.getResourceOidRequired())
                .and().item(ShadowType.F_OBJECT_CLASS).eq(object.getObjectClass())
                .and().filter(nameAttr.normalizationAwareEqFilter());
        if (liveOnly) {
            q = q.and().block()
                    .item(ShadowType.F_DEAD).isNull()
                    .or().item(ShadowType.F_DEAD).eq(false)
                    .endBlock();
        }
        var query = q.build();
        displayDumpable("Filter for checking the uniqueness", query);
        var objects = repositoryService.searchObjects(ShadowType.class, query, null, result);
        assertEquals("Wrong number of repo shadows for ICF NAME \"" + nameAttr + "\"", 1, objects.size());
    }

    @SafeVarargs
    protected final <T> void assertAttribute(PrismObject<ShadowType> shadow, String attrName, T... expectedValues) {
        assertAttribute(shadow.asObjectable(), attrName, expectedValues);
    }

    protected void assertNoAttribute(AbstractShadow shadow, String attrName) {
        assertNoAttribute(resource, shadow.getBean(), attrName);
    }

    protected void assertNoAttribute(PrismObject<ShadowType> shadow, String attrName) {
        assertNoAttribute(resource, shadow.asObjectable(), attrName);
    }

    protected void assertBareSchemaSanity(BareResourceSchema resourceSchema, ResourceType resourceType) throws Exception {
        dummyResourceCtl.assertDummyResourceSchemaSanityExtended(
                resourceSchema, resourceType, !supportsMemberOf(),
                PIRATE_SCHEMA_NUMBER_OF_DEFINITIONS + (supportsMemberOf() ? 1 : 0));
    }

    protected DummyAccount getDummyAccount(String icfName, String icfUid) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        if (isIcfNameUidSame()) {
            return dummyResource.getAccountByName(icfName);
        } else {
            return dummyResource.getAccountById(icfUid);
        }
    }

    DummyAccount getDummyAccountAssert(String icfName, String icfUid) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        if (isIcfNameUidSame()) {
            return dummyResource.getAccountByName(icfName);
        } else {
            DummyAccount account = dummyResource.getAccountById(icfUid);
            assertNotNull("No dummy account with ICF UID " + icfUid + " (expected name " + icfName + ")", account);
            assertEquals("Unexpected name in " + account, icfName, account.getName());
            return account;
        }
    }

    protected DummyAccountAsserter<Void> assertDummyAccount(String icfName, String icfUid)
            throws ConnectException, FileNotFoundException, SchemaViolationException,
            ConflictException, InterruptedException {
        if (isIcfNameUidSame()) {
            return dummyResourceCtl.assertAccountByUsername(icfName);
        } else {
            return dummyResourceCtl.assertAccountById(icfUid);
        }
    }

    protected void assertNoDummyAccount(String icfName, String icfUid) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        DummyAccount account;
        if (isIcfNameUidSame()) {
            account = dummyResource.getAccountByName(icfName);
        } else {
            account = dummyResource.getAccountById(icfUid);
        }
        assertNull("Unexpected dummy account with ICF UID " + icfUid + " (name " + icfName + ")", account);
    }

    @SuppressWarnings({ "SameParameterValue", "WeakerAccess" })
    protected DummyGroup getDummyGroup(String icfName, String icfUid) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        if (isIcfNameUidSame()) {
            return dummyResource.getGroupByName(icfName);
        } else {
            return dummyResource.getGroupById(icfUid);
        }
    }

    DummyGroup getDummyGroupAssert(String icfName, String icfUid) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        if (isIcfNameUidSame()) {
            return dummyResource.getGroupByName(icfName);
        } else {
            DummyGroup group = dummyResource.getGroupById(icfUid);
            assertNotNull("No dummy group with ICF UID " + icfUid + " (expected name " + icfName + ")", group);
            assertEquals("Unexpected name in " + group, icfName, group.getName());
            return group;
        }
    }

    @SuppressWarnings({ "SameParameterValue", "WeakerAccess" })
    protected DummyPrivilege getDummyPrivilege(String icfName, String icfUid) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        if (isIcfNameUidSame()) {
            return dummyResource.getPrivilegeByName(icfName);
        } else {
            return dummyResource.getPrivilegeById(icfUid);
        }
    }

    DummyPrivilege getDummyPrivilegeAssert(String icfName, String icfUid) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        if (isIcfNameUidSame()) {
            return dummyResource.getPrivilegeByName(icfName);
        } else {
            DummyPrivilege priv = dummyResource.getPrivilegeById(icfUid);
            assertNotNull("No dummy privilege with ICF UID " + icfUid + " (expected name " + icfName + ")", priv);
            assertEquals("Unexpected name in " + priv, icfName, priv.getName());
            return priv;
        }
    }

    @SuppressWarnings("WeakerAccess")
    @SafeVarargs
    protected final <T> void assertDummyAccountAttributeValues(
            String accountName, String accountUid, String attributeName, T... expectedValues)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        DummyAccount dummyAccount = getDummyAccountAssert(accountName, accountUid);
        assertNotNull("No account '" + accountName + "'", dummyAccount);
        assertDummyAttributeValues(dummyAccount, attributeName, expectedValues);
    }

    @SuppressWarnings("WeakerAccess")
    @SafeVarargs
    protected final <T> void assertDummyAttributeValues(
            DummyObject object, String attributeName, T... expectedValues) {
        //noinspection unchecked
        Set<T> attributeValues = (Set<T>) object.getAttributeValues(attributeName, expectedValues[0].getClass());
        assertNotNull("No attribute " + attributeName + " in " + object.getShortTypeName() + " " + object, attributeValues);
        TestUtil.assertSetEquals("Wrong values of attribute " + attributeName + " in " + object.getShortTypeName() + " " + object, attributeValues, expectedValues);
    }

    void assertNoDummyAttribute(DummyObject object, String attributeName) {
        Set<Object> attributeValues = object.getAttributeValues(attributeName, Object.class);
        assertNotNull("Unexpected attribute " + attributeName + " in " + object.getShortTypeName() + " " + object + ": " + attributeValues, attributeValues);
    }

    String getWillRepoIcfName() {
        return ACCOUNT_WILL_USERNAME;
    }

    String getWillRepoIcfNameNorm() {
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

    /**
     * Asserts privileges.
     *
     * The default implementation uses old-school simulation attribute.
     * (Overridden by the use of links for native references.)
     *
     * Treats {@link #PRIVILEGE_NONSENSE_NAME} privilege specially.
     */
    protected void assertPrivileges(DummyAccount dummyAccount, String... expected) {
        assertNotNull("Account is gone!", dummyAccount);
        Set<String> accountPrivileges = dummyAccount.getAttributeValues(DummyAccount.ATTR_PRIVILEGES_NAME, String.class);
        PrismAsserts.assertSets(
                "account privileges",
                accountPrivileges,
                Arrays.stream(expected)
                        .map(origName ->
                                PRIVILEGE_NONSENSE_NAME.equals(origName) ?
                                        origName : // nonsense does not exist on resource, so it's not transformed
                                        transformNameToResource(origName))
                        .toList());
    }

    void assertGroupAssociation(AbstractShadow account, String entitlementOid) {
        assertAssociation(account, ASSOCIATION_GROUP_NAME, entitlementOid);
    }

    void assertPrivAssociation(AbstractShadow account, String entitlementOid) {
        assertAssociation(account, ASSOCIATION_PRIV_NAME, entitlementOid);
    }

    protected void checkRepoAccountShadow(RawRepoShadow repoShadow) {
        ProvisioningTestUtil.checkRepoAccountShadow(repoShadow);
    }

    void assertDummyConnectorInstances(int expectedConnectorInstances)
            throws NumberFormatException, SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        List<ConnectorOperationalStatus> stats = provisioningService.getConnectorOperationalStatus(RESOURCE_DUMMY_OID, task, result);
        display("Resource connector stats", stats);
        assertSuccess(result);

        assertEquals("unexpected number of stats", 1, stats.size());
        ConnectorOperationalStatus stat = stats.get(0);

        assertEquals("Unexpected number of Dummy connector instances", expectedConnectorInstances,
                stat.getPoolStatusNumIdle() + stat.getPoolStatusNumActive());
    }

    /**
     * Deletes all accounts on given dummy resource: from the resource and from the repository.
     * The accounts can be broken, so we do the deletion on the very low level (repo + dummy resource).
     */
    void cleanupAccounts(DummyTestResource resource, OperationResult result) throws SchemaException,
            ObjectNotFoundException, InterruptedException, FileNotFoundException, ConnectException, SchemaViolationException,
            ConflictException, ObjectDoesNotExistException {
        SearchResultList<PrismObject<ShadowType>> shadows = repositoryService.searchObjects(ShadowType.class, getAllAccountsQuery(resource), null, result);
        for (PrismObject<ShadowType> shadow : shadows) {
            repositoryService.deleteObject(ShadowType.class, shadow.getOid(), result);
        }
        DummyResource dummyResource = resource.controller.getDummyResource();
        for (DummyAccount dummyAccount : new ArrayList<>(dummyResource.listAccounts())) {
            dummyResource.deleteAccountById(dummyAccount.getId());
        }
    }

    @SuppressWarnings({ "SameParameterValue", "WeakerAccess" })
    @NotNull
    protected ObjectQuery getAllAccountsQuery(DummyTestResource resource) {
        return ObjectQueryUtil.createResourceAndObjectClassQuery(resource.oid, RI_ACCOUNT_OBJECT_CLASS);
    }

    /** Useful for standalone running of supported tests. */
    void initializeResourceIfNeeded() throws CommonException {
        if (!resourceInitialized) {
            var task = getTestTask();
            var result = task.getResult();
            provisioningService.testResource(RESOURCE_DUMMY_OID, task, result);
            resource = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
            resourceBean = resource.asObjectable();
            resourceInitialized = true;
        }
    }

    @NotNull ResourceObjectDefinition getAccountDefaultDefinition() throws SchemaException, ConfigurationException {
        return MiscUtil.stateNonNull(
                Resource.of(resourceBean)
                        .getCompleteSchemaRequired()
                        .getObjectTypeDefinition(ShadowKindType.ACCOUNT, SchemaConstants.INTENT_DEFAULT),
                "No account/default definition in %s", resourceBean);
    }

    @NotNull ResourceObjectDefinition getGroupDefaultDefinition() throws SchemaException, ConfigurationException {
        return MiscUtil.stateNonNull(
                Resource.of(resourceBean)
                        .getCompleteSchemaRequired()
                        .findDefinitionForObjectClass(RI_GROUP_OBJECT_CLASS),
                "No group definition in %s", resourceBean);
    }

    protected @NotNull Collection<? extends QName> getCachedAccountAttributes() throws SchemaException, ConfigurationException {
        return InternalsConfig.isShadowCachingOnByDefault() ?
                getAccountDefaultDefinition().getAttributeNames() :
                getAccountDefaultDefinition().getAllIdentifiersNames();
    }

    protected ShadowSimpleAttributeDefinition<?> getAccountAttrDef(String name) throws SchemaException, ConfigurationException {
        return requireNonNull(
                getAccountObjectClassDefinition().findSimpleAttributeDefinition(name));
    }

    protected ShadowSimpleAttributeDefinition<?> getAccountAttrDef(QName name) throws SchemaException, ConfigurationException {
        return requireNonNull(
                getAccountObjectClassDefinition().findSimpleAttributeDefinition(name));
    }

    @NotNull ResourceObjectClassDefinition getAccountObjectClassDefinition() throws SchemaException, ConfigurationException {
        ResourceSchema resourceSchema =
                requireNonNull(
                        ResourceSchemaFactory.getCompleteSchemaRequired(resource));
        return requireNonNull(
                resourceSchema.findObjectClassDefinition(RI_ACCOUNT_OBJECT_CLASS));
    }

    /** TODO reconcile with {@link #assertRepoShadow(String)} */
    RepoShadowAsserter<Void> assertRepoShadowNew(@NotNull String oid)
            throws SchemaException, ConfigurationException, ObjectNotFoundException {
        return assertRepoShadow(oid, getCachedAccountAttributes());
    }

    /** TODO reconcile with {@link #assertRepoShadow(String)} */
    RepoShadowAsserter<Void> assertRepoShadowNew(@NotNull RawRepoShadow rawRepoShadow)
            throws SchemaException, ConfigurationException {
        return RepoShadowAsserter.forRepoShadow(rawRepoShadow, getCachedAccountAttributes());
    }

    /** TODO reconcile with {@link #assertRepoShadow(String)} */
    RepoShadowAsserter<Void> assertRepoShadowNew(@NotNull PrismObject<ShadowType> rawRepoShadow)
            throws SchemaException, ConfigurationException {
        return RepoShadowAsserter.forRepoShadow(rawRepoShadow, getCachedAccountAttributes());
    }

    void initAndReloadDummyResource(Task task, OperationResult result) throws CommonException {
        assertSuccess(
                provisioningService.testResource(RESOURCE_DUMMY_OID, task, result));

        resource = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
        resourceBean = resource.asObjectable();
    }
}
