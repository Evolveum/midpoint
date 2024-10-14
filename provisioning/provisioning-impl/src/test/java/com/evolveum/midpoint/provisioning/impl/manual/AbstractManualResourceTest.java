/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.manual;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.*;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.impl.AbstractProvisioningIntegrationTest;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.PointInTimeType;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.ShadowAsserter;
import com.evolveum.midpoint.test.asserter.prism.PrismObjectAsserter;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public abstract class AbstractManualResourceTest extends AbstractProvisioningIntegrationTest {

    protected static final File TEST_DIR = new File("src/test/resources/manual/");

    protected static final File RESOURCE_MANUAL_FILE = new File(TEST_DIR, "resource-manual.xml");
    protected static final String RESOURCE_MANUAL_OID = "8a8e19de-1a14-11e7-965f-6f995b457a8b";

    protected static final File RESOURCE_SEMI_MANUAL_FILE = new File(TEST_DIR, "resource-semi-manual.xml");

    public static final QName RESOURCE_ACCOUNT_OBJECTCLASS = RI_ACCOUNT_OBJECT_CLASS;

    protected static final String MANUAL_CONNECTOR_TYPE = "ManualConnector";

    protected static final String NS_MANUAL_CONF = "http://midpoint.evolveum.com/xml/ns/public/connector/builtin-1/bundle/com.evolveum.midpoint.provisioning.ucf.impl.builtin/ManualConnector";
    protected static final ItemName CONF_PROPERTY_DEFAULT_ASSIGNEE_QNAME = new ItemName(NS_MANUAL_CONF, "defaultAssignee");

    protected static final File ACCOUNT_WILL_FILE = new File(TEST_DIR, "account-will.xml");
    protected static final String ACCOUNT_WILL_OID = "c1add81e-1df7-11e7-bbb7-5731391ba751";
    protected static final String ACCOUNT_WILL_USERNAME = "will";
    protected static final String ACCOUNT_WILL_FULLNAME = "Will Turner";
    protected static final String ACCOUNT_WILL_FULLNAME_PIRATE = "Pirate Will Turner";
    protected static final String ACCOUNT_WILL_DESCRIPTION_MANUAL = "manual";
    protected static final String ACCOUNT_WILL_PASSWORD_OLD = "3lizab3th";
    protected static final String ACCOUNT_WILL_PASSWORD_NEW = "ELIZAbeth";

    protected static final File ACCOUNT_FIASCO_FILE = new File(TEST_DIR, "account-fiasco.xml");
    protected static final String ACCOUNT_FIASCO_OID = "414dd45c-017f-11eb-acac-1760a547c740";

    protected static final File ACCOUNT_BLACK_FILE = new File(TEST_DIR, "account-black.xml");
    protected static final String ACCOUNT_BLACK_OID = "c9de3f3d-125f-452f-b321-b90cc7457246";

    protected static final String ATTR_USERNAME = "username";
    protected static final QName ATTR_USERNAME_QNAME = new QName(MidPointConstants.NS_RI, ATTR_USERNAME);

    protected static final String ATTR_FULLNAME = "fullname";
    protected static final QName ATTR_FULLNAME_QNAME = new QName(MidPointConstants.NS_RI, ATTR_FULLNAME);

    protected static final String ATTR_DESCRIPTION = "description";
    protected static final QName ATTR_DESCRIPTION_QNAME = new QName(MidPointConstants.NS_RI, ATTR_DESCRIPTION);

    protected static final File ORG_OPERATORS_FILE = new File(TEST_DIR, "organization-operators.xml");

    protected static final File ROLE_OPERATORS_FILE = new File(TEST_DIR, "role-operators.xml");

    protected static final File USER_OPERATOR_ORG_MEMBER_FILE = new File(TEST_DIR, "user-operator-org-member.xml");
    protected static final String USER_OPERATOR_ORG_MEMBER_OID = "6f706572-6174-6f72-2d6f-72672d6d656d";

    protected static final File USER_OPERATOR_ORG_MANAGER_FILE = new File(TEST_DIR, "user-operator-org-manager.xml");

    protected static final File USER_OPERATOR_ROLE_MEMBER_FILE = new File(TEST_DIR, "user-operator-role-member.xml");
    protected static final String USER_OPERATOR_ROLE_MEMBER_OID = "6f706572-6174-6f72-2d72-6f6c652d6d65";

    protected static final File USER_OPERATOR_ROLE_APPROVER_FILE = new File(TEST_DIR, "user-operator-role-approver.xml");

    protected static final File USER_OPERATOR_FILE = new File(TEST_DIR, "user-operator.xml");
    protected static final String USER_OPERATOR_OID = "6f706572-6174-6f72-2d75-736572000000";

    protected PrismObject<ResourceType> resource;
    protected ResourceType resourceType;

    protected XMLGregorianCalendar accountWillReqestTimestampStart;
    protected XMLGregorianCalendar accountWillReqestTimestampEnd;

    protected XMLGregorianCalendar accountWillCompletionTimestampStart;
    protected XMLGregorianCalendar accountWillCompletionTimestampEnd;

    protected XMLGregorianCalendar accountWillSecondReqestTimestampStart;
    protected XMLGregorianCalendar accountWillSecondReqestTimestampEnd;

    protected XMLGregorianCalendar accountWillSecondCompletionTimestampStart;
    protected XMLGregorianCalendar accountWillSecondCompletionTimestampEnd;

    protected String willLastCaseOid;
    protected String willSecondLastCaseOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        addOperatorsObjects(initResult);
    }

    private void addOperatorsObjects(OperationResult initResult) throws Exception {
        display("Adding operators organization", repoAddObjectFromFile(ORG_OPERATORS_FILE, initResult));
        display("Adding operators organization member", repoAddObjectFromFile(USER_OPERATOR_ORG_MEMBER_FILE, initResult));
        display("Adding operators organization manager", repoAddObjectFromFile(USER_OPERATOR_ORG_MANAGER_FILE, initResult));

        display("Adding operators role", repoAddObjectFromFile(ROLE_OPERATORS_FILE, initResult));
        display("Adding operators role member", repoAddObjectFromFile(USER_OPERATOR_ROLE_MEMBER_FILE, initResult));
        display("Adding operators role approver", repoAddObjectFromFile(USER_OPERATOR_ROLE_APPROVER_FILE, initResult));

        display("Adding operator user", repoAddObjectFromFile(USER_OPERATOR_FILE, initResult));
    }

    protected abstract File getResourceFile();

    protected String getResourceOid() {
        return RESOURCE_MANUAL_OID;
    }

    protected boolean supportsBackingStore() {
        return false;
    }

    @Test
    public void test000Sanity() throws Exception {
        assertNotNull("Resource is null", resource);
        assertNotNull("ResourceType is null", resourceType);

        OperationResult result = createOperationResult();

        ResourceType repoResource = repositoryService.getObject(ResourceType.class, getResourceOid(),
                null, result).asObjectable();
        assertNotNull("No connector ref", repoResource.getConnectorRef());
        String connectorOid = repoResource.getConnectorRef().getOid();
        assertNotNull("No connector ref OID", connectorOid);
        ConnectorType repoConnector = repositoryService
                .getObject(ConnectorType.class, connectorOid, null, result).asObjectable();
        assertNotNull(repoConnector);
        display("Manual Connector", repoConnector);

        // Check connector schema
        IntegrationTestTools.assertConnectorSchemaSanity(repoConnector);
    }

    @Test
    public void test003Connection() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Check that there is a schema, but no capabilities before test (pre-condition)
        ResourceType resourceBefore = repositoryService.getObject(ResourceType.class, getResourceOid(),
                null, result).asObjectable();

        Element resourceXsdSchemaElementBefore = ResourceTypeUtil.getResourceXsdSchemaElement(resourceBefore);
        assertResourceSchemaBeforeTest(resourceXsdSchemaElementBefore);

        CapabilitiesType capabilities = resourceBefore.getCapabilities();
        if (capabilities != null) {
            AssertJUnit.assertNull("Native capabilities present before test connection. Bad test setup?", capabilities.getNative());
        }

        // WHEN
        OperationResult testResult = provisioningService.testResource(getResourceOid(), task, result);

        // THEN
        display("Test result", testResult);
        TestUtil.assertSuccess("Test resource failed (result)", testResult);

        PrismObject<ResourceType> resourceRepoAfter = repositoryService.getObject(ResourceType.class, getResourceOid(), null, result);
        ResourceType resourceTypeRepoAfter = resourceRepoAfter.asObjectable();
        display("Resource after test", resourceTypeRepoAfter);

        XmlSchemaType xmlSchemaTypeAfter = resourceTypeRepoAfter.getSchema();
        assertNotNull("No schema after test connection", xmlSchemaTypeAfter);
        Element resourceXsdSchemaElementAfter = ResourceTypeUtil.getResourceXsdSchemaElement(resourceTypeRepoAfter);
        assertNotNull("No schema after test connection", resourceXsdSchemaElementAfter);

        String resourceXml = prismContext.xmlSerializer().serialize(resourceRepoAfter);
        displayValue("Resource XML", resourceXml);

        CachingMetadataType cachingMetadata = xmlSchemaTypeAfter.getCachingMetadata();
        assertNotNull("No caching metadata", cachingMetadata);
        assertNotNull("No retrievalTimestamp", cachingMetadata.getRetrievalTimestamp());
        assertNotNull("No serialNumber", cachingMetadata.getSerialNumber());

        Element xsdElement = ObjectTypeUtil.findXsdElement(xmlSchemaTypeAfter);
        ResourceSchema parsedSchema = ResourceSchemaFactory.parseNativeSchemaAsBare(xsdElement);
        assertNotNull("No schema after parsing", parsedSchema);

        // schema will be checked in next test
    }

    protected abstract void assertResourceSchemaBeforeTest(Element resourceXsdSchemaElementBefore);

    @Test
    public void test004Configuration() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = createOperationResult();

        // WHEN
        resource = provisioningService.getObject(ResourceType.class, getResourceOid(), null, task, result);
        resourceType = resource.asObjectable();

        PrismContainer<Containerable> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
        assertNotNull("No configuration container", configurationContainer);
        PrismContainerDefinition<?> confContDef = configurationContainer.getDefinition();
        assertNotNull("No configuration container definition", confContDef);
        PrismProperty<String> propDefaultAssignee = configurationContainer.findProperty(CONF_PROPERTY_DEFAULT_ASSIGNEE_QNAME);
        assertNotNull("No defaultAssignee conf prop", propDefaultAssignee);

//        assertNotNull("No configuration properties container", configurationPropertiesContainer);
//        PrismContainerDefinition confPropDef = configurationPropertiesContainer.getDefinition();
//        assertNotNull("No configuration properties container definition", confPropDef);

    }

    @Test
    public void test005ParsedSchema() throws Exception {
        // GIVEN

        // THEN
        // The returned type should have the schema pre-parsed
        assertTrue(ResourceSchemaFactory.hasParsedSchema(resourceType));

        // Also test if the utility method returns the same thing
        BareResourceSchema resourceSchema = ResourceSchemaFactory.getBareSchema(resourceType);

        displayDumpable("Parsed resource schema", resourceSchema);

        assertNativeSchemaCached(resourceSchema, ResourceSchemaFactory.getBareSchema(resourceType));

        ResourceObjectClassDefinition accountDef = resourceSchema.findObjectClassDefinition(RESOURCE_ACCOUNT_OBJECTCLASS);
        assertNotNull("Account definition is missing", accountDef);
        assertNotNull("Null identifiers in account", accountDef.getPrimaryIdentifiers());
        assertFalse("Empty identifiers in account", accountDef.getPrimaryIdentifiers().isEmpty());
        assertNotNull("No naming attribute in account", accountDef.getNamingAttribute());

        assertEquals("Unexpected number of definitions", getNumberOfAccountAttributeDefinitions(), accountDef.getDefinitions().size());

        ShadowSimpleAttributeDefinition<?> usernameDef = accountDef.findSimpleAttributeDefinition(ATTR_USERNAME);
        assertNotNull("No definition for username", usernameDef);
        assertEquals(1, usernameDef.getMaxOccurs());
        assertEquals(1, usernameDef.getMinOccurs());
        assertTrue("No username create", usernameDef.canAdd());
        assertTrue("No username update", usernameDef.canModify());
        assertTrue("No username read", usernameDef.canRead());

        ShadowSimpleAttributeDefinition<?> fullnameDef = accountDef.findSimpleAttributeDefinition(ATTR_FULLNAME);
        assertNotNull("No definition for fullname", fullnameDef);
        assertEquals(1, fullnameDef.getMaxOccurs());
        assertEquals(0, fullnameDef.getMinOccurs());
        assertTrue("No fullname create", fullnameDef.canAdd());
        assertTrue("No fullname update", fullnameDef.canModify());
        assertTrue("No fullname read", fullnameDef.canRead());
    }

    protected int getNumberOfAccountAttributeDefinitions() {
        return 4;
    }

    @Test
    public void test006Capabilities() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = createOperationResult();

        // WHEN
        ResourceType resource = provisioningService.getObject(ResourceType.class, getResourceOid(), null, task, result).asObjectable();

        // THEN
        display("Resource from provisioning", resource);
        displayValue("Resource from provisioning (XML)", PrismTestUtil.serializeToXml(resource));

        CapabilityCollectionType nativeCapabilities = resource.getCapabilities().getNative();
        assertFalse("Empty capabilities returned", CapabilityUtil.isEmpty(nativeCapabilities));

        CreateCapabilityType capCreate = CapabilityUtil.getCapability(nativeCapabilities, CreateCapabilityType.class);
        assertNotNull("Missing create capability", capCreate);
        assertManual(capCreate);

        ActivationCapabilityType capAct = CapabilityUtil.getCapability(nativeCapabilities, ActivationCapabilityType.class);
        assertNotNull("Missing activation capability", capAct);

        ReadCapabilityType capRead = CapabilityUtil.getCapability(nativeCapabilities, ReadCapabilityType.class);
        assertNotNull("Missing read capability", capRead);
        assertEquals("Wrong caching-only setting in read capability", Boolean.TRUE, capRead.isCachingOnly());

        dumpResourceCapabilities(resource);
    }

    @Test
    public void test100AddAccountWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        PrismObject<ShadowType> account = parseObject(ACCOUNT_WILL_FILE);
        account.checkConsistence();

        display("Adding shadow", account);

        accountWillReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        String addedObjectOid = provisioningService.addObject(account, null, null, task, result);

        // THEN
        then();
        display("result", result);
        willLastCaseOid = assertInProgress(result);

        accountWillReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();
        assertEquals(ACCOUNT_WILL_OID, addedObjectOid);
        account.checkConsistence();

        ShadowAsserter<Void> shadowRepoAsserter = assertRepoShadow(ACCOUNT_WILL_OID)
                .assertConception()
                .pendingOperations()
                .singleOperation()
                .assertId()
                .assertType(PendingOperationTypeType.MANUAL)
                .assertRequestTimestamp(accountWillReqestTimestampStart, accountWillReqestTimestampEnd)
                .end()
                .end()
                .attributes()
                .assertValue(ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME)
                .end()
                //.assertNoPasswordIf(isNoPasswordExpected());
                .assertNoPassword(); // change when resolving MID-10050
        assertAttributeFromCache(shadowRepoAsserter, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
        assertShadowActivationAdministrativeStatusFromCache(shadowRepoAsserter, ActivationStatusType.ENABLED);

        syncServiceMock.assertNoNotifyChange();
        syncServiceMock.assertSingleNotifyInProgressOnly();

        ShadowAsserter<Void> shadowProvisioningAsserter = assertShadowProvisioning(ACCOUNT_WILL_OID)
                .assertConception()
                .assertName(ACCOUNT_WILL_USERNAME)
                .assertKind(ShadowKindType.ACCOUNT)
                .pendingOperations()
                .singleOperation()
                .assertId()
                .assertType(PendingOperationTypeType.MANUAL)
                .assertRequestTimestamp(accountWillReqestTimestampStart, accountWillReqestTimestampEnd)
                .end()
                .end()
                .attributes()
                .assertValue(ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME)
                .end()
                //.assertNoPasswordIf(isNoPasswordExpected());
                .assertNoPassword(); // change when resolving MID-10050
        assertAttributeFromCache(shadowProvisioningAsserter, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
        assertShadowActivationAdministrativeStatusFromCache(shadowProvisioningAsserter, ActivationStatusType.ENABLED);

        assertNotNull("No async reference in result", willLastCaseOid);

        CaseType aCase = assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
        ObjectReferenceType targetRef = aCase.getTargetRef();
        assertThat(targetRef).as("case targetRef").isNotNull();
    }

    private boolean isNoPasswordExpected() {
        //return !InternalsConfig.isShadowCachingOnByDefault() && supportsBackingStore();
        return true; // change when resolving MID-10050
    }

    @Test
    public void test102GetAccountWillFuture() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        Collection<SelectorOptions<GetOperationOptions>> options =
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE));

        // WHEN
        when();
        PrismObject<ShadowType> shadowProvisioning =
                provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, options, task, result);

        // THEN
        then();
        assertSuccess(result);

        display("Provisioning shadow", shadowProvisioning);
        ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
        assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
        assertNoAttribute(shadowProvisioning, ATTR_DESCRIPTION_QNAME);
        assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
        assertShadowExists(shadowProvisioning, true);
        // TODO
//        assertShadowPassword(shadowProvisioning);
    }

    /**
     * Case haven't changed. There should be no change in the shadow.
     */
    @Test
    public void test104RefreshAccountWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        PrismObject<ShadowType> shadowBefore = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
        display("Shadow before", shadowBefore);

        // WHEN
        when();
        provisioningService.refreshShadow(shadowBefore, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<ShadowType> shadowRepo = getShadowRepoLegacy(ACCOUNT_WILL_OID);
        display("Repo shadow", shadowRepo);
        assertSinglePendingOperation(shadowRepo, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);
        assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
        assertNoAttribute(shadowRepo, ATTR_DESCRIPTION_QNAME);
        assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
        if (isNoPasswordExpected()) {
            assertNoShadowPassword(shadowRepo);
        }
        assertShadowExists(shadowRepo, false);

        syncServiceMock.assertNoNotifyChange();
        syncServiceMock.assertNoNotifications();

        PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID, null, task, result);

        display("Provisioning shadow", shadowProvisioning);
        ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
        assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttributeFromCache(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
        assertNoAttribute(shadowProvisioning, ATTR_DESCRIPTION_QNAME);
        assertShadowActivationAdministrativeStatusFromCache(shadowProvisioning, ActivationStatusType.ENABLED);
        if (isNoPasswordExpected()) {
            assertNoShadowPassword(shadowProvisioning);
        }

        PendingOperationType pendingOperation = assertSinglePendingOperation(shadowProvisioning, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);

        assertCaseState(pendingOperation.getAsynchronousOperationReference(), SchemaConstants.CASE_STATE_OPEN);
    }

    protected void backingStoreAddWill() throws IOException {
        // nothing to do here
    }

    @Test
    public void test106AddToBackingStoreAndGetAccountWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        backingStoreAddWill();

        syncServiceMock.reset();

        // WHEN
        when();
        PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        display("Provisioning shadow", shadowProvisioning);
        ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
        assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
        assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
        assertShadowExists(shadowProvisioning, supportsBackingStore());
        assertShadowPassword(shadowProvisioning);

        PrismObject<ShadowType> shadowRepo = getShadowRepoLegacy(ACCOUNT_WILL_OID);
        display("Repo shadow", shadowRepo);
        assertSinglePendingOperation(shadowRepo, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);
        assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
        assertNoAttribute(shadowRepo, ATTR_DESCRIPTION_QNAME);
        assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
        if (isNoPasswordExpected()) {
            assertNoShadowPassword(shadowRepo);
        }
        assertShadowExists(shadowRepo, supportsBackingStore());
    }

    @Test
    public void test108GetAccountWillFuture() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        syncServiceMock.reset();

        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE));

        // WHEN
        when();
        PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, options, task, result);

        // THEN
        then();
        assertSuccess(result);

        display("Provisioning shadow", shadowProvisioning);
        assertNotNull("no OID", shadowProvisioning.getOid());
        ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
        assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
        assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
        assertShadowExists(shadowProvisioning, true);
        // TODO
//        assertShadowPassword(shadowProvisioning);

        PrismObject<ShadowType> shadowRepo = getShadowRepoLegacy(ACCOUNT_WILL_OID);
        display("Repo shadow", shadowRepo);
        assertSinglePendingOperation(shadowRepo, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);
        assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
        assertNoAttribute(shadowRepo, ATTR_DESCRIPTION_QNAME);
        assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
        if (isNoPasswordExpected()) {
            assertNoShadowPassword(shadowRepo);
        }
        assertShadowExists(shadowRepo, supportsBackingStore());
    }

    @Test
    public void test109GetAccountWillFutureNoFetch() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        GetOperationOptions options = GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE);
        options.setNoFetch(true);

        // WHEN
        when();
        PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID,
                SelectorOptions.createCollection(options),
                task, result);

        // THEN
        then();
        assertSuccess(result);

        display("Provisioning shadow (future,noFetch)", shadowProvisioningFuture);
        assertNotNull("no OID", shadowProvisioningFuture.getOid());
        ShadowType shadowTypeProvisioning = shadowProvisioningFuture.asObjectable();
        assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        // Both manual and semimanual resource can see fullname at this point.
        // As there is no object in the CSV (no resourceShadow) the pending ADD delta will be used.
        assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
        assertNoAttribute(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME);
        assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
        assertShadowExists(shadowProvisioningFuture, true);
        // TODO
//        assertShadowPassword(shadowProvisioningFuture);
    }

    /**
     * Case is closed. The operation is complete.
     */
    @Test
    public void test110CloseCaseAndRefreshAccountWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        PrismObject<ShadowType> shadowBefore = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
        display("Shadow before", shadowBefore);

        closeCase(willLastCaseOid);

        accountWillCompletionTimestampStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        provisioningService.refreshShadow(shadowBefore, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        accountWillCompletionTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        PrismObject<ShadowType> shadowRepo = getShadowRepoLegacy(ACCOUNT_WILL_OID);
        display("Repo shadow", shadowRepo);
        assertSinglePendingOperation(shadowRepo,
                accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
                OperationResultStatusType.SUCCESS,
                accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
        assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
        assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);

        syncServiceMock.assertNoNotifyChange();
        syncServiceMock.assertSingleNotifySuccessOnly();

        PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID, null, task, result);

        display("Provisioning shadow", shadowProvisioning);
        ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
        assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
        assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
        assertShadowPassword(shadowProvisioning);

        assertSinglePendingOperation(shadowProvisioning,
                accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
                OperationResultStatusType.SUCCESS,
                accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
    }

    /**
     * ff 5min, everything should be the same (grace not expired yet)
     */
    @Test
    public void test120RefreshAccountWillAfter5min() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        clock.overrideDuration("PT5M");

        PrismObject<ShadowType> shadowBefore = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
        display("Shadow before", shadowBefore);

        // WHEN
        when();
        provisioningService.refreshShadow(shadowBefore, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<ShadowType> shadowRepo = getShadowRepoLegacy(ACCOUNT_WILL_OID);
        ShadowAsserter.forShadow(shadowRepo, "repo")
                .display()
                .pendingOperations()
                .singleOperation()
                .assertRequestTimestamp(accountWillReqestTimestampStart, accountWillReqestTimestampEnd)
                .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                .assertResultStatus(OperationResultStatusType.SUCCESS)
                .assertCompletionTimestamp(accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

        PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID, null, task, result);
        ShadowAsserter.forShadow(shadowProvisioning, "provisioning")
                .display()
                .pendingOperations()
                .singleOperation()
                .assertRequestTimestamp(accountWillReqestTimestampStart, accountWillReqestTimestampEnd)
                .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                .assertResultStatus(OperationResultStatusType.SUCCESS)
                .assertCompletionTimestamp(accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);

        syncServiceMock.assertNoNotifyChange();
        syncServiceMock.assertNoNotifications();
    }

    /**
     * ff 11min, grace should expire. The operation should no longer be considered
     * for future shadow computation. But it should still remain in the shadow.
     */
    @Test
    public void test130RefreshAccountWillAfter16min() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        clock.overrideDuration("PT11M");

        PrismObject<ShadowType> shadowBefore = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
        display("Shadow before", shadowBefore);

        // WHEN
        when();
        provisioningService.refreshShadow(shadowBefore, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<ShadowType> shadowRepo = getShadowRepoLegacy(ACCOUNT_WILL_OID);
        ShadowAsserter.forShadow(shadowRepo, "repo")
                .display()
                .pendingOperations()
                .singleOperation()
                .assertRequestTimestamp(accountWillReqestTimestampStart, accountWillReqestTimestampEnd)
                .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                .assertResultStatus(OperationResultStatusType.SUCCESS)
                .assertCompletionTimestamp(accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

        PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID, null, task, result);
        ShadowAsserter.forShadow(shadowProvisioning, "provisioning")
                .display()
                .pendingOperations()
                .singleOperation()
                .assertRequestTimestamp(accountWillReqestTimestampStart, accountWillReqestTimestampEnd)
                .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                .assertResultStatus(OperationResultStatusType.SUCCESS)
                .assertCompletionTimestamp(accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);

        syncServiceMock.assertNoNotifyChange();
        syncServiceMock.assertNoNotifications();
    }

    /**
     * ff 6min, pending operation should expire. The operation should no longer
     * be in the shadow.
     */
    @Test
    public void test132RefreshAccountWillAfter27min() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        clock.overrideDuration("PT6M");

        PrismObject<ShadowType> shadowBefore = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
        display("Shadow before", shadowBefore);

        // WHEN
        when();
        provisioningService.refreshShadow(shadowBefore, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<ShadowType> shadowRepo = getShadowRepoLegacy(ACCOUNT_WILL_OID);
        ShadowAsserter.forShadow(shadowRepo, "repo")
                .display()
                .pendingOperations()
                .assertNone();

        PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
        ShadowAsserter.forShadow(shadowProvisioning, "provisioning")
                .display()
                .pendingOperations()
                .assertNone();

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);

        syncServiceMock.assertNoNotifyChange();
        syncServiceMock.assertNoNotifications();
    }

    @Test
    public void test200ModifyAccountWillFullname() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                ACCOUNT_WILL_OID, ItemPath.create(ShadowType.F_ATTRIBUTES, ATTR_FULLNAME_QNAME),
                ACCOUNT_WILL_FULLNAME_PIRATE);
        displayDumpable("ObjectDelta", delta);

        accountWillReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        provisioningService.modifyObject(
                ShadowType.class, delta.getOid(), delta.getModifications(),
                null, null, task, result);

        // THEN
        then();
        display("result", result);
        willLastCaseOid = assertInProgress(result);

        accountWillReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        PrismObject<ShadowType> shadowRepo = getShadowRepoLegacy(ACCOUNT_WILL_OID);
        ShadowAsserter.forShadow(shadowRepo, "repo")
                .display()
                .assertBasicRepoProperties()
                .attributes()
                .assertValue(ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME)
                .end()
                .pendingOperations()
                .singleOperation()
                .assertType(PendingOperationTypeType.MANUAL)
                .assertRequestTimestamp(accountWillReqestTimestampStart, accountWillReqestTimestampEnd)
                .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .assertResultStatus(OperationResultStatusType.IN_PROGRESS)
                .assertId();

        // Still old data in the repo. The operation is not completed yet.
        assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
        assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);

        syncServiceMock.assertNoNotifyChange();
        syncServiceMock.assertSingleNotifyInProgressOnly();

        PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID, null, task, result);

        display("Provisioning shadow", shadowProvisioning);
        ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
        assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
        assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
        assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowProvisioning);

        assertSinglePendingOperation(shadowProvisioning, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);

        PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID,
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
                task, result);
        display("Provisioning shadow (future)", shadowProvisioningFuture);
        assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
        assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
        assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
        assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowProvisioningFuture);

        assertNotNull("No async reference in result", willLastCaseOid);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
    }

    @Test
    public void test202RefreshAccountWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        PrismObject<ShadowType> shadowBefore = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
        display("Shadow before", shadowBefore);

        // WHEN
        when();
        provisioningService.refreshShadow(shadowBefore, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<ShadowType> shadowRepo = getShadowRepoLegacy(ACCOUNT_WILL_OID);
        ShadowAsserter.forShadow(shadowRepo, "repo")
                .display()
                .assertBasicRepoProperties()
                .attributes()
                .assertValue(ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME)
                .end()
                .pendingOperations()
                .singleOperation()
                .assertRequestTimestamp(accountWillReqestTimestampStart, accountWillReqestTimestampEnd)
                .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .assertResultStatus(OperationResultStatusType.IN_PROGRESS)
                .assertId();

        // Still old data in the repo. The operation is not completed yet.
        assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
        assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);

        syncServiceMock.assertNoNotifyChange();
        syncServiceMock.assertNoNotifications();

        PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
        ShadowAsserter.forShadow(shadowProvisioning, "provisioning")
                .display()
                .assertBasicRepoProperties()
                .assertKind(ShadowKindType.ACCOUNT)
                .assertAdministrativeStatus(ActivationStatusType.ENABLED)
                .attributes()
                .assertValue(ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME)
                .assertValue(ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME)
                .end()
                .pendingOperations()
                .singleOperation()
                .assertRequestTimestamp(accountWillReqestTimestampStart, accountWillReqestTimestampEnd)
                .assertExecutionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .assertResultStatus(OperationResultStatusType.IN_PROGRESS)
                .assertId();

        assertShadowPassword(shadowProvisioning);

        PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID,
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
                task, result);
        ShadowAsserter.forShadow(shadowProvisioningFuture, "future")
                .display()
                .assertName(ACCOUNT_WILL_USERNAME)
                .assertKind(ShadowKindType.ACCOUNT)
                .assertAdministrativeStatus(ActivationStatusType.ENABLED)
                .attributes()
                .assertValue(ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME)
                .assertValue(ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
        assertShadowPassword(shadowProvisioningFuture);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);

    }

    /**
     * Case is closed. The operation is complete.
     */
    @Test
    public void test204CloseCaseAndRefreshAccountWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        PrismObject<ShadowType> shadowBefore = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
        display("Shadow before", shadowBefore);

        closeCase(willLastCaseOid);

        accountWillCompletionTimestampStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        provisioningService.refreshShadow(shadowBefore, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        accountWillCompletionTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        PrismObject<ShadowType> shadowRepo = getShadowRepoLegacy(ACCOUNT_WILL_OID);
        ShadowAsserter.forShadow(shadowRepo, "repo")
                .display()
                .assertBasicRepoProperties()
                .assertName(ACCOUNT_WILL_USERNAME)
                .attributes()
                .assertValue(ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME)
                .end()
                .pendingOperations()
                .singleOperation()
                .assertRequestTimestamp(accountWillReqestTimestampStart, accountWillReqestTimestampEnd)
                .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                .assertResultStatus(OperationResultStatusType.SUCCESS)
                .assertCompletionTimestamp(accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd)
                .assertId();
        assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);

        PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID, null, task, result);
        ShadowAsserter.forShadow(shadowProvisioning, "provisioning")
                .display()
                .assertName(ACCOUNT_WILL_USERNAME)
                .assertKind(ShadowKindType.ACCOUNT)
                .assertAdministrativeStatus(ActivationStatusType.ENABLED)
                .attributes()
                .assertValue(ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME)
                .end()
                .pendingOperations()
                .singleOperation()
                .assertRequestTimestamp(accountWillReqestTimestampStart, accountWillReqestTimestampEnd)
                .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                .assertResultStatus(OperationResultStatusType.SUCCESS)
                .assertCompletionTimestamp(accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd)
                .assertId();
        if (supportsBackingStore()) {
            assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
        } else {
            assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
        }
        assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowProvisioning);

        PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID,
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
                task, result);
        ShadowAsserter.forShadow(shadowProvisioningFuture, "future")
                .display()
                .assertName(ACCOUNT_WILL_USERNAME)
                .assertKind(ShadowKindType.ACCOUNT)
                .assertAdministrativeStatus(ActivationStatusType.ENABLED)
                .attributes()
                .assertValue(ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME)
                .assertValue(ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
        assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowProvisioningFuture);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);

        // In this case check notifications at the end. There were some reads that
        // internally triggered refresh. Maku sure no extra notifications were sent.
        syncServiceMock.assertNoNotifyChange();
        syncServiceMock.assertSingleNotifySuccessOnly();
    }

    /**
     * ff 5min, everything should be the same (grace not expired yet)
     */
    @Test
    public void test210RefreshAccountWillAfter5min() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        clock.overrideDuration("PT5M");

        PrismObject<ShadowType> shadowBefore = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
        display("Shadow before", shadowBefore);

        // WHEN
        when();
        provisioningService.refreshShadow(shadowBefore, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<ShadowType> shadowRepo = getShadowRepoLegacy(ACCOUNT_WILL_OID);
        display("Repo shadow", shadowRepo);
        assertSinglePendingOperation(shadowRepo,
                accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
                OperationResultStatusType.SUCCESS,
                accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

        syncServiceMock.assertNoNotifyChange();
        syncServiceMock.assertNoNotifications();

        PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID, null, task, result);
        assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
        assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        if (supportsBackingStore()) {
            assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME);
        } else {
            assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
        }
        assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowProvisioning);

        assertSinglePendingOperation(shadowProvisioning,
                accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
                OperationResultStatusType.SUCCESS,
                accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

        PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID,
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
                task, result);
        display("Provisioning shadow (future)", shadowProvisioningFuture);
        assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
        assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
        assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
        assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowProvisioningFuture);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
    }

    protected void backingStoreUpdateWill(String newFullName, ActivationStatusType newAdministrativeStatus, String password) throws IOException {
        // nothing to do here
    }

    @Test
    public void test212UpdateBackingStoreAndGetAccountWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        PrismObject<ShadowType> shadowBefore = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
        display("Shadow before", shadowBefore);

        backingStoreUpdateWill(ACCOUNT_WILL_FULLNAME_PIRATE, ActivationStatusType.ENABLED, ACCOUNT_WILL_PASSWORD_OLD);

        // WHEN
        when();
        provisioningService.refreshShadow(shadowBefore, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<ShadowType> shadowRepo = getShadowRepoLegacy(ACCOUNT_WILL_OID);
        display("Repo shadow", shadowRepo);
        assertSinglePendingOperation(shadowRepo,
                accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
                OperationResultStatusType.SUCCESS,
                accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

        syncServiceMock.assertNoNotifyChange();
        syncServiceMock.assertNoNotifications();

        PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID, null, task, result);
        assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
        assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
        assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowProvisioning);

        assertSinglePendingOperation(shadowProvisioning,
                accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
                OperationResultStatusType.SUCCESS,
                accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

        PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID,
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
                task, result);
        display("Provisioning shadow (future)", shadowProvisioningFuture);
        assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
        assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
        assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
        assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowProvisioningFuture);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
    }

    /**
     * disable - do not complete yet (do not wait for delta to expire, we want several deltas at once).
     */
    @Test
    public void test220ModifyAccountWillDisable() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                ACCOUNT_WILL_OID, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
                ActivationStatusType.DISABLED);
        displayDumpable("ObjectDelta", delta);

        accountWillReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
                null, null, task, result);

        // THEN
        then();
        display("result", result);
        willLastCaseOid = assertInProgress(result);

        accountWillReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        PrismObject<ShadowType> shadowRepo = getShadowRepoLegacy(ACCOUNT_WILL_OID);
        display("Repo shadow", shadowRepo);
        assertPendingOperationDeltas(shadowRepo, 2);
        PendingOperationType pendingOperation = findPendingOperation(shadowRepo,
                OperationResultStatusType.IN_PROGRESS, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
        assertPendingOperation(shadowRepo, pendingOperation, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);

        assertNotNull("No ID in pending operation", pendingOperation.getId());
        // Still old data in the repo. The operation is not completed yet.
        assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
        assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);

        syncServiceMock.assertNoNotifyChange();
        syncServiceMock.assertSingleNotifyInProgressOnly();

        PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID, null, task, result);

        display("Provisioning shadow", shadowProvisioning);
        ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
        assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
        assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
        assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowProvisioning);

        assertPendingOperationDeltas(shadowProvisioning, 2);
        pendingOperation = findPendingOperation(shadowProvisioning,
                OperationResultStatusType.IN_PROGRESS, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
        assertPendingOperation(shadowProvisioning, pendingOperation, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);

        PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID,
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
                task, result);
        display("Provisioning shadow (future)", shadowProvisioningFuture);
        assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
        assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.DISABLED);
        assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
        assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowProvisioningFuture);

        assertNotNull("No async reference in result", willLastCaseOid);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
    }

    /**
     * Change password, enable. There is still pending disable delta. Make sure all the deltas are
     * stored correctly.
     */
    @Test
    public void test230ModifyAccountWillChangePasswordAndEnable() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                ACCOUNT_WILL_OID, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
                ActivationStatusType.ENABLED);
        ProtectedStringType ps = new ProtectedStringType();
        ps.setClearValue(ACCOUNT_WILL_PASSWORD_NEW);
        delta.addModificationReplaceProperty(SchemaConstants.PATH_PASSWORD_VALUE, ps);
        displayDumpable("ObjectDelta", delta);

        accountWillSecondReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
                null, null, task, result);

        // THEN
        then();
        display("result", result);
        willSecondLastCaseOid = assertInProgress(result);

        accountWillSecondReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        PrismObject<ShadowType> shadowRepo = getShadowRepoLegacy(ACCOUNT_WILL_OID);
        display("Repo shadow", shadowRepo);
        assertPendingOperationDeltas(shadowRepo, 3);
        PendingOperationType pendingOperation = findPendingOperation(shadowRepo,
                OperationResultStatusType.IN_PROGRESS, SchemaConstants.PATH_PASSWORD_VALUE);
        assertPendingOperation(shadowRepo, pendingOperation, accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd);

        assertNotNull("No ID in pending operation", pendingOperation.getId());
        // Still old data in the repo. The operation is not completed yet.
        assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
        assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);

        syncServiceMock.assertNoNotifyChange();
        syncServiceMock.assertSingleNotifyInProgressOnly();

        PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID, null, task, result);

        display("Provisioning shadow", shadowProvisioning);
        ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
        assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
        assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
        assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowProvisioning);

        assertPendingOperationDeltas(shadowProvisioning, 3);
        pendingOperation = findPendingOperation(shadowProvisioning,
                OperationResultStatusType.IN_PROGRESS, SchemaConstants.PATH_PASSWORD_VALUE);
        assertPendingOperation(shadowProvisioning, pendingOperation,
                accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd);

        PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID,
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
                task, result);
        display("Provisioning shadow (future)", shadowProvisioningFuture);
        assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
        assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
        assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
        assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        new PrismObjectAsserter<>(shadowProvisioningFuture)
                .assertSanity();

        assertNotNull("No async reference in result", willSecondLastCaseOid);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
        assertCaseState(willSecondLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
    }

    @Test(enabled = false) // This no longer works as the work item creation is now done in cases-impl module
    public void test231CaseHasExpectedNumbersOfWorkItemsWithExpectedAssignees() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        int expectedNumberOfWorkItems = 3;
        List<String> expectedWorkItemsAssigneesOids = Stream.of
                        (USER_OPERATOR_ROLE_MEMBER_OID, USER_OPERATOR_OID, USER_OPERATOR_ORG_MEMBER_OID)
                .sorted(String::compareToIgnoreCase).collect(Collectors.toList());

        CaseType caseType = repositoryService.getObject(CaseType.class, willLastCaseOid, null, result).asObjectable();

        List<String> actualWorkItemsAssigneesOids = caseType.getWorkItem().stream().flatMap(caseWorkItemType ->
                        caseWorkItemType.getAssigneeRef().stream())
                .map(ObjectReferenceType::getOid).sorted(String::compareToIgnoreCase).collect(Collectors.toList());
        int actualNumberOfWorkItems = caseType.getWorkItem().size();

        assertEquals(expectedNumberOfWorkItems, actualNumberOfWorkItems);
        assertEquals(expectedWorkItemsAssigneesOids, actualWorkItemsAssigneesOids);
    }

    /**
     * Disable case is closed. The account should be disabled. But there is still the other
     * delta pending.
     * Do NOT explicitly refresh the shadow in this case. Just reading it should cause the refresh.
     * Even though forceRefresh is NOT used here. The resource is set to automatic refresh.
     */
    @Test
    public void test240CloseDisableCaseAndReadAccountWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        closeCase(willLastCaseOid);

        accountWillCompletionTimestampStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        accountWillCompletionTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        display("Provisioning shadow", shadowProvisioning);

        PrismObject<ShadowType> shadowRepo = assertRepoShadow(ACCOUNT_WILL_OID)
                .pendingOperations()
                .assertOperations(3)
                .by()
                .executionStatus(PendingOperationExecutionStatusType.COMPLETED)
                .item(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS)
                .find()
                .assertRequestTimestamp(accountWillReqestTimestampStart, accountWillReqestTimestampEnd)
                .assertResultStatus(OperationResultStatusType.SUCCESS)
                .assertCompletionTimestamp(accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd)
                .end()
                .by()
                .executionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .item(SchemaConstants.PATH_PASSWORD_VALUE)
                .find()
                .assertRequestTimestamp(accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd)
                .assertResultStatus(OperationResultStatusType.IN_PROGRESS)
                .end()
                .end()
                .attributes()
                .assertValue(ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME)
                .end()
                .getObject();

        assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.DISABLED);
        assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);

        syncServiceMock.assertNoNotifyChange();
        syncServiceMock.assertSingleNotifySuccessOnly();

        ShadowAsserter.forShadow(shadowProvisioning, "provisioning")
                .assertName(ACCOUNT_WILL_USERNAME)
                .assertKind(ShadowKindType.ACCOUNT)
                .attributes()
                .assertValue(ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME)
                .assertValue(ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE)
                .end()
                .pendingOperations()
                .assertOperations(3)
                .by()
                .executionStatus(PendingOperationExecutionStatusType.COMPLETED)
                .item(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS)
                .find()
                .assertRequestTimestamp(accountWillReqestTimestampStart, accountWillReqestTimestampEnd)
                .assertResultStatus(OperationResultStatusType.SUCCESS)
                .assertCompletionTimestamp(accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd)
                .end()
                .by()
                .executionStatus(PendingOperationExecutionStatusType.EXECUTING)
                .item(SchemaConstants.PATH_PASSWORD_VALUE)
                .find()
                .assertRequestTimestamp(accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd)
                .assertResultStatus(OperationResultStatusType.IN_PROGRESS)
                .end()
                .end();

        if (supportsBackingStore()) {
            assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
        } else {
            assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.DISABLED);
        }
        assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowProvisioning);

        PrismObject<ShadowType> shadowProvisioningFuture = assertShadowFuture(ACCOUNT_WILL_OID)
                .assertName(ACCOUNT_WILL_USERNAME)
                .assertKind(ShadowKindType.ACCOUNT)
                .assertAdministrativeStatus(ActivationStatusType.ENABLED)
                .attributes()
                .assertValue(ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME)
                .assertValue(ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE)
                .end()
                .getObject();

        assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        // TODO
//        assertShadowPassword(shadowProvisioningFuture);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
        assertCaseState(willSecondLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
    }

    /**
     * lets ff 5min just for fun. Refresh, make sure everything should be the same (grace not expired yet)
     */
    @Test
    public void test250RefreshAccountWillAfter5min() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        clock.overrideDuration("PT5M");

        PrismObject<ShadowType> shadowBefore = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
        display("Shadow before", shadowBefore);

        // WHEN
        when();
        provisioningService.refreshShadow(shadowBefore, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<ShadowType> shadowRepo = getShadowRepoLegacy(ACCOUNT_WILL_OID);
        display("Repo shadow", shadowRepo);

        assertPendingOperationDeltas(shadowRepo, 3);

        PendingOperationType pendingOperation = findPendingOperation(shadowRepo,
                OperationResultStatusType.SUCCESS, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
        assertPendingOperation(shadowRepo, pendingOperation,
                accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
                OperationResultStatusType.SUCCESS,
                accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

        pendingOperation = findPendingOperation(shadowRepo,
                OperationResultStatusType.IN_PROGRESS, SchemaConstants.PATH_PASSWORD_VALUE);
        assertPendingOperation(shadowRepo, pendingOperation, accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd);

        assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.DISABLED);
        assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);

        syncServiceMock.assertNoNotifyChange();
        syncServiceMock.assertNoNotifications();

        PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID, null, task, result);

        display("Provisioning shadow", shadowProvisioning);
        ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
        assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        if (supportsBackingStore()) {
            assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
        } else {
            assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.DISABLED);
        }
        assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
        assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowProvisioning);

        assertPendingOperationDeltas(shadowProvisioning, 3);
        pendingOperation = findPendingOperation(shadowProvisioning,
                OperationResultStatusType.SUCCESS, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
        assertPendingOperation(shadowProvisioning, pendingOperation,
                accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
                OperationResultStatusType.SUCCESS,
                accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

        PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID,
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
                task, result);
        display("Provisioning shadow (future)", shadowProvisioningFuture);
        assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
        assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
        assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
        assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        // TODO
//        assertShadowPassword(shadowProvisioningFuture);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
        assertCaseState(willSecondLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
    }

    @Test
    public void test252UpdateBackingStoreAndGetAccountWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        PrismObject<ShadowType> shadowBefore = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
        display("Shadow before", shadowBefore);

        backingStoreUpdateWill(ACCOUNT_WILL_FULLNAME_PIRATE, ActivationStatusType.DISABLED, ACCOUNT_WILL_PASSWORD_OLD);

        // WHEN
        when();
        provisioningService.refreshShadow(shadowBefore, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<ShadowType> shadowRepo = getShadowRepoLegacy(ACCOUNT_WILL_OID);
        display("Repo shadow", shadowRepo);
        assertPendingOperationDeltas(shadowRepo, 3);

        syncServiceMock.assertNoNotifyChange();
        syncServiceMock.assertNoNotifications();

        PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID, null, task, result);

        display("Provisioning shadow", shadowProvisioning);
        ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
        assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.DISABLED);
        assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
        assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowProvisioning);

        assertPendingOperationDeltas(shadowProvisioning, 3);

        PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID,
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
                task, result);
        display("Provisioning shadow (future)", shadowProvisioningFuture);
        assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
        assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
        assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
        assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        // TODO
//        assertShadowPassword(shadowProvisioningFuture);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
        assertCaseState(willSecondLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
    }

    /**
     * Password change/enable case is closed. The account should be disabled. But there is still the other
     * delta pending.
     */
    @Test
    public void test260ClosePasswordChangeCaseAndRefreshAccountWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        closeCase(willSecondLastCaseOid);

        // Get repo shadow here. Make sure refresh works with this as well.
        PrismObject<ShadowType> shadowBefore = getShadowRepoLegacy(ACCOUNT_WILL_OID);
        provisioningService.applyDefinition(shadowBefore, task, result);
        display("Shadow before", shadowBefore);

        accountWillSecondCompletionTimestampStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        provisioningService.refreshShadow(shadowBefore, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        accountWillSecondCompletionTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        PrismObject<ShadowType> shadowRepo = getShadowRepoLegacy(ACCOUNT_WILL_OID);
        display("Repo shadow", shadowRepo);

        assertPendingOperationDeltas(shadowRepo, 3);

        PendingOperationType pendingOperation = findPendingOperation(shadowRepo,
                OperationResultStatusType.SUCCESS, SchemaConstants.PATH_PASSWORD_VALUE);
        assertPendingOperation(shadowRepo, pendingOperation,
                accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
                OperationResultStatusType.SUCCESS,
                accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd);

        pendingOperation = findPendingOperation(shadowRepo,
                OperationResultStatusType.SUCCESS, SchemaConstants.PATH_PASSWORD_VALUE);
        assertPendingOperation(shadowRepo, pendingOperation,
                accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
                OperationResultStatusType.SUCCESS,
                accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd);

        assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
        assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);

        syncServiceMock.assertNoNotifyChange();
        syncServiceMock.assertSingleNotifySuccessOnly();

        PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID, null, task, result);

        display("Provisioning shadow", shadowProvisioning);
        ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
        assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        if (supportsBackingStore()) {
            assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.DISABLED);
        } else {
            assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
        }
        assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
        assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowProvisioning);

        assertPendingOperationDeltas(shadowProvisioning, 3);
        pendingOperation = findPendingOperation(shadowProvisioning,
                OperationResultStatusType.SUCCESS, SchemaConstants.PATH_PASSWORD_VALUE);
        assertPendingOperation(shadowRepo, pendingOperation,
                accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
                OperationResultStatusType.SUCCESS,
                accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd);

        PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID,
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
                task, result);
        display("Provisioning shadow (future)", shadowProvisioningFuture);
        assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
        assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
        assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
        assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowProvisioningFuture);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
        assertCaseState(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
    }

    /**
     * ff 7min. Refresh. Oldest pending operation is over grace period. But is should still be
     * in the shadow.
     */
    @Test
    public void test270RefreshAccountWillAfter7min() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        clock.overrideDuration("PT7M");

        PrismObject<ShadowType> shadowBefore = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
        display("Shadow before", shadowBefore);

        // WHEN
        when();
        provisioningService.refreshShadow(shadowBefore, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<ShadowType> shadowRepo = getShadowRepoLegacy(ACCOUNT_WILL_OID);
        ShadowAsserter.forShadow(shadowRepo, "repo")
                .display()
                .assertBasicRepoProperties()
                .attributes()
                .assertValue(ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME)
                .end()
                .pendingOperations()
                .assertOperations(3)
                .by()
                .executionStatus(PendingOperationExecutionStatusType.COMPLETED)
                .item(SchemaConstants.PATH_PASSWORD_VALUE)
                .find()
                .assertRequestTimestamp(accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd)
                .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                .assertResultStatus(OperationResultStatusType.SUCCESS)
                .assertCompletionTimestamp(accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd)
                .end();

        assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
        assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);

        syncServiceMock.assertNoNotifyChange();
        syncServiceMock.assertNoNotifications();

        PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
        ShadowAsserter.forShadow(shadowProvisioning, "provisioning")
                .display()
                .assertName(ACCOUNT_WILL_USERNAME)
                .assertKind(ShadowKindType.ACCOUNT)
                .attributes()
                .assertValue(ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME)
                .assertValue(ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE)
                .end()
                .pendingOperations()
                .assertOperations(3)
                .by()
                .executionStatus(PendingOperationExecutionStatusType.COMPLETED)
                .item(SchemaConstants.PATH_PASSWORD_VALUE)
                .find()
                .assertRequestTimestamp(accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd)
                .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                .assertResultStatus(OperationResultStatusType.SUCCESS)
                .assertCompletionTimestamp(accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd)
                .end();

        if (supportsBackingStore()) {
            assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.DISABLED);
        } else {
            assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
        }
        assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowProvisioning);

        PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID,
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
                task, result);
        ShadowAsserter.forShadow(shadowProvisioningFuture, "future")
                .display()
                .assertName(ACCOUNT_WILL_USERNAME)
                .assertKind(ShadowKindType.ACCOUNT)
                .assertAdministrativeStatus(ActivationStatusType.ENABLED)
                .attributes()
                .assertValue(ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME)
                .assertValue(ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);

        assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowProvisioningFuture);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
        assertCaseState(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
    }

    /**
     * ff 5min. Refresh. Oldest pending operation is expired.
     */
    @Test
    public void test271RefreshAccountWillAfter12min() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        clock.overrideDuration("PT5M");

        PrismObject<ShadowType> shadowBefore = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
        display("Shadow before", shadowBefore);

        // WHEN
        when();
        provisioningService.refreshShadow(shadowBefore, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<ShadowType> shadowRepo = getShadowRepoLegacy(ACCOUNT_WILL_OID);
        ShadowAsserter.forShadow(shadowRepo, "repo")
                .display()
                .assertBasicRepoProperties()
                .attributes()
                .assertValue(ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME)
                .end()
                .pendingOperations()
                .assertOperations(2)
                .by()
                .executionStatus(PendingOperationExecutionStatusType.COMPLETED)
                .item(SchemaConstants.PATH_PASSWORD_VALUE)
                .find()
                .assertRequestTimestamp(accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd)
                .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                .assertResultStatus(OperationResultStatusType.SUCCESS)
                .assertCompletionTimestamp(accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd)
                .end();

        assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
        assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);

        syncServiceMock.assertNoNotifyChange();
        syncServiceMock.assertNoNotifications();

        PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
        ShadowAsserter.forShadow(shadowProvisioning, "provisioning")
                .display()
                .assertName(ACCOUNT_WILL_USERNAME)
                .assertKind(ShadowKindType.ACCOUNT)
                .attributes()
                .assertValue(ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME)
                .assertValue(ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE)
                .end()
                .pendingOperations()
                .assertOperations(2)
                .by()
                .executionStatus(PendingOperationExecutionStatusType.COMPLETED)
                .item(SchemaConstants.PATH_PASSWORD_VALUE)
                .find()
                .assertRequestTimestamp(accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd)
                .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                .assertResultStatus(OperationResultStatusType.SUCCESS)
                .assertCompletionTimestamp(accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd)
                .end();

        if (supportsBackingStore()) {
            assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.DISABLED);
        } else {
            assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
        }
        assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowProvisioning);

        PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID,
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
                task, result);
        ShadowAsserter.forShadow(shadowProvisioningFuture, "future")
                .display()
                .assertName(ACCOUNT_WILL_USERNAME)
                .assertKind(ShadowKindType.ACCOUNT)
                .assertAdministrativeStatus(ActivationStatusType.ENABLED)
                .attributes()
                .assertValue(ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME)
                .assertValue(ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);

        assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowProvisioningFuture);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
        assertCaseState(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
    }

    @Test
    public void test272UpdateBackingStoreAndGetAccountWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        PrismObject<ShadowType> shadowBefore = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
        display("Shadow before", shadowBefore);

        backingStoreUpdateWill(ACCOUNT_WILL_FULLNAME_PIRATE, ActivationStatusType.ENABLED, ACCOUNT_WILL_PASSWORD_NEW);

        // WHEN
        when();
        provisioningService.refreshShadow(shadowBefore, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<ShadowType> shadowRepo = getShadowRepoLegacy(ACCOUNT_WILL_OID);
        display("Repo shadow", shadowRepo);
        assertPendingOperationDeltas(shadowRepo, 2);

        syncServiceMock.assertNoNotifyChange();
        syncServiceMock.assertNoNotifications();

        PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID, null, task, result);

        display("Provisioning shadow", shadowProvisioning);
        ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
        assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
        assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
        assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowProvisioning);

        assertPendingOperationDeltas(shadowProvisioning, 2);

        PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID,
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
                task, result);
        display("Provisioning shadow (future)", shadowProvisioningFuture);
        assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
        assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
        assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
        assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        // TODO
//        assertShadowPassword(shadowProvisioningFuture);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
        assertCaseState(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
    }

    /**
     * ff 5min. Refresh. Another delta should expire.
     */
    @Test
    public void test280RefreshAccountWillAfter5min() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        clock.overrideDuration("PT5M");

        PrismObject<ShadowType> shadowBefore = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
        display("Shadow before", shadowBefore);

        // WHEN
        when();
        provisioningService.refreshShadow(shadowBefore, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<ShadowType> shadowRepo = getShadowRepoLegacy(ACCOUNT_WILL_OID);
        display("Repo shadow", shadowRepo);

        assertPendingOperationDeltas(shadowRepo, 1);

        PendingOperationType pendingOperation = findPendingOperation(shadowRepo,
                OperationResultStatusType.SUCCESS, SchemaConstants.PATH_PASSWORD_VALUE);
        assertPendingOperation(shadowRepo, pendingOperation,
                accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
                OperationResultStatusType.SUCCESS,
                accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd);

        pendingOperation = findPendingOperation(shadowRepo,
                OperationResultStatusType.SUCCESS, SchemaConstants.PATH_PASSWORD_VALUE);
        assertPendingOperation(shadowRepo, pendingOperation,
                accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
                OperationResultStatusType.SUCCESS,
                accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd);

        assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
        assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);

        syncServiceMock.assertNoNotifyChange();
        syncServiceMock.assertNoNotifications();

        PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID, null, task, result);

        display("Provisioning shadow", shadowProvisioning);
        ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
        assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
        assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
        assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowProvisioning);

        assertPendingOperationDeltas(shadowProvisioning, 1);
        pendingOperation = findPendingOperation(shadowProvisioning,
                OperationResultStatusType.SUCCESS, SchemaConstants.PATH_PASSWORD_VALUE);
        assertPendingOperation(shadowRepo, pendingOperation,
                accountWillSecondReqestTimestampStart, accountWillSecondReqestTimestampEnd,
                OperationResultStatusType.SUCCESS,
                accountWillSecondCompletionTimestampStart, accountWillSecondCompletionTimestampEnd);

        PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID,
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
                task, result);
        display("Provisioning shadow (future)", shadowProvisioningFuture);
        assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
        assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
        assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
        assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowProvisioningFuture);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
        assertCaseState(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
    }

    /**
     * ff 5min. Refresh. All delta should expire.
     */
    @Test
    public void test290RefreshAccountWillAfter5min() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        clock.overrideDuration("PT5M");

        PrismObject<ShadowType> shadowBefore = getShadowRepoLegacy(ACCOUNT_WILL_OID);
        display("Shadow before", shadowBefore);

        // WHEN
        when();
        provisioningService.refreshShadow(shadowBefore, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<ShadowType> shadowRepo = getShadowRepoLegacy(ACCOUNT_WILL_OID);
        display("Repo shadow", shadowRepo);

        assertPendingOperationDeltas(shadowRepo, 0);

        assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
        assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);

        syncServiceMock.assertNoNotifyChange();
        syncServiceMock.assertNoNotifications();

        PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID, null, task, result);

        display("Provisioning shadow", shadowProvisioning);
        ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
        assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
        assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
        assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowProvisioning);

        assertPendingOperationDeltas(shadowProvisioning, 0);

        PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID,
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
                task, result);
        display("Provisioning shadow (future)", shadowProvisioningFuture);
        assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
        assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
        assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
        assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowProvisioningFuture);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
        assertCaseState(willSecondLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
    }

    @Test
    public void test300DeleteAccountWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        accountWillReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        provisioningService.deleteObject(ShadowType.class, ACCOUNT_WILL_OID, null, null, task, result);

        // THEN
        then();
        display("result", result);
        willLastCaseOid = assertInProgress(result);

        accountWillReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        PrismObject<ShadowType> shadowRepo = getShadowRepoLegacy(ACCOUNT_WILL_OID);
        display("Repo shadow", shadowRepo);
        assertPendingOperationDeltas(shadowRepo, 1);
        PendingOperationType pendingOperation = findPendingOperation(shadowRepo,
                OperationResultStatusType.IN_PROGRESS, null);
        assertPendingOperation(shadowRepo, pendingOperation, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);

        assertNotNull("No ID in pending operation", pendingOperation.getId());
        // Still old data in the repo. The operation is not completed yet.
        assertShadowActivationAdministrativeStatusFromCache(shadowRepo, ActivationStatusType.ENABLED);
        assertAttribute(shadowRepo, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttributeFromCache(shadowRepo, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);

        syncServiceMock.assertNoNotifyChange();
        syncServiceMock.assertSingleNotifyInProgressOnly();

        PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID, null, task, result);

        display("Provisioning shadow", shadowProvisioning);
        ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
        assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
        assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, ACCOUNT_WILL_USERNAME);
        assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, ACCOUNT_WILL_FULLNAME_PIRATE);
        assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowProvisioning);

        assertPendingOperationDeltas(shadowProvisioning, 1);
        pendingOperation = findPendingOperation(shadowProvisioning,
                OperationResultStatusType.IN_PROGRESS, null);
        assertPendingOperation(shadowProvisioning, pendingOperation, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);

        PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID,
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
                task, result);
        display("Provisioning shadow (future)", shadowProvisioningFuture);
        assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
        assertShadowDead(shadowProvisioningFuture);
        assertShadowPassword(shadowProvisioningFuture);

        assertNotNull("No async reference in result", willLastCaseOid);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
    }

    @Test
    public void test302GetAccountWillFuture() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID,
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
                task, result);

        // THEN
        then();
        assertSuccess(result);

        display("Provisioning shadow (future)", shadowProvisioningFuture);
        assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
        assertShadowDead(shadowProvisioningFuture);
        assertShadowPassword(shadowProvisioningFuture);
    }

    @Test
    public void test303GetAccountWillFutureNoFetch() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        GetOperationOptions options = GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE);
        options.setNoFetch(true);

        // WHEN
        when();
        PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID,
                SelectorOptions.createCollection(options),
                task, result);

        // THEN
        then();
        assertSuccess(result);

        display("Provisioning shadow (future,noFetch)", shadowProvisioningFuture);
        assertNotNull("no OID", shadowProvisioningFuture.getOid());
        assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
        assertShadowDead(shadowProvisioningFuture);
        if (isNoPasswordExpected()) {
            assertNoShadowPassword(shadowProvisioningFuture);
        }
    }

    /**
     * Case is closed. The operation is complete.
     */
    @Test
    public void test310CloseCaseAndRefreshAccountWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        accountWillCompletionTimestampStart = clock.currentTimeXMLGregorianCalendar();

        closeCase(willLastCaseOid);

        PrismObject<ShadowType> shadowBefore = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
        display("Shadow before", shadowBefore);

        // WHEN
        when();
        provisioningService.refreshShadow(shadowBefore, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        accountWillCompletionTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        PrismObject<ShadowType> shadowRepo = getShadowRepoLegacy(ACCOUNT_WILL_OID);
        display("Repo shadow", shadowRepo);
        assertSinglePendingOperation(shadowRepo,
                accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
                OperationResultStatusType.SUCCESS,
                accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
        assertShadowDead(shadowRepo);

        syncServiceMock.assertNoNotifyChange();
        syncServiceMock.assertSingleNotifySuccessOnly();

        PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID, null, task, result);

        display("Provisioning shadow", shadowProvisioning);
        ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
        assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertShadowDead(shadowProvisioning);
        assertShadowPassword(shadowProvisioning);

        assertSinglePendingOperation(shadowProvisioning,
                accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
                OperationResultStatusType.SUCCESS,
                accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

        PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID,
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
                task, result);
        display("Provisioning shadow (future)", shadowProvisioningFuture);
        assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
        assertShadowDead(shadowProvisioningFuture);
        assertShadowPassword(shadowProvisioningFuture);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
    }

    /**
     * ff 5min, everything should be the same (grace not expired yet)
     */
    @Test
    public void test320RefreshAccountWillAfter5min() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        clock.overrideDuration("PT5M");

        PrismObject<ShadowType> shadowBefore = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
        display("Shadow before", shadowBefore);

        // WHEN
        when();
        provisioningService.refreshShadow(shadowBefore, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<ShadowType> shadowRepo = getShadowRepoLegacy(ACCOUNT_WILL_OID);
        display("Repo shadow", shadowRepo);
        assertSinglePendingOperation(shadowRepo,
                accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
                OperationResultStatusType.SUCCESS,
                accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
        assertShadowDead(shadowRepo);

        syncServiceMock.assertNoNotifyChange();
        syncServiceMock.assertNoNotifications();

        PrismObject<ShadowType> shadowProvisioning = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID, null, task, result);

        display("Provisioning shadow", shadowProvisioning);
        ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
        assertShadowName(shadowProvisioning, ACCOUNT_WILL_USERNAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertShadowDead(shadowProvisioning);
        assertShadowPassword(shadowProvisioning);

        assertSinglePendingOperation(shadowProvisioning,
                accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
                OperationResultStatusType.SUCCESS,
                accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

        PrismObject<ShadowType> shadowProvisioningFuture = provisioningService.getObject(ShadowType.class,
                ACCOUNT_WILL_OID,
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
                task, result);
        display("Provisioning shadow (future)", shadowProvisioningFuture);
        assertShadowName(shadowProvisioningFuture, ACCOUNT_WILL_USERNAME);
        assertShadowDead(shadowProvisioningFuture);
        assertShadowPassword(shadowProvisioningFuture);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
    }

    @Test
    public void test500AddAccountFiasco() throws Exception {
        // GIVEN

        Task givenTask = createTask(getTestName());
        OperationResult givenResult = givenTask.getResult();

        PrismObject<ShadowType> account = parseObject(ACCOUNT_FIASCO_FILE);
        account.checkConsistence();

        display("Adding shadow", account);
        String addedObjectOid = provisioningService.addObject(account, null, null, givenTask, givenResult);
        assertEquals(ACCOUNT_FIASCO_OID, addedObjectOid);
        String fiascoCaseOid = assertInProgress(givenResult);
        assertNotNull("No async reference in result", fiascoCaseOid);

        PrismObject<ShadowType> repoShadow = assertRepoShadow(ACCOUNT_FIASCO_OID)
                .assertConception()
                .pendingOperations()
                .singleOperation()
                .assertId()
                .assertType(PendingOperationTypeType.MANUAL)
                .end()
                .end()
                .getObject();

        assertCaseState(fiascoCaseOid, SchemaConstants.CASE_STATE_OPEN);

        XMLGregorianCalendar operationTimestampStart = clock.currentTimeXMLGregorianCalendar();

        closeCase(fiascoCaseOid, OperationResultStatusType.FATAL_ERROR);

        Task task = getTestTask();
        OperationResult result = task.getResult();
        syncServiceMock.reset();

        // WHEN
        when();
        provisioningService.refreshShadow(repoShadow, null, task, result);

        // THEN
        then();
        display(result);
        assertSuccess(result);

        XMLGregorianCalendar operationTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        assertRepoShadow(ACCOUNT_FIASCO_OID)
                .assertIsNotExists()
                .pendingOperations()
                .singleOperation()
                .assertId()
                .assertType(PendingOperationTypeType.MANUAL)
                .assertCompletionTimestamp(operationTimestampStart, operationTimestampEnd)
                .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                .assertResultStatus(OperationResultStatusType.FATAL_ERROR)
                .end()
                .end()
                //.assertNoPasswordIf(isNoPasswordExpected());
                .assertNoPassword(); // change when resolving MID-10050

        syncServiceMock.assertNoNotifyChange();
        syncServiceMock.assertSingleNotifySuccessOnly();

        assertCaseState(fiascoCaseOid, SchemaConstants.CASE_STATE_CLOSED);

    }

    /**
     * Tests renaming of account with pending changes. MID-7985, MID-7924
     */
    @Test
    public void test600RenameAccount() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("black account is created");
        PrismObject<ShadowType> account = parseObject(ACCOUNT_BLACK_FILE);
        account.checkConsistence();

        display("Creating account", account);
        OperationResult creationResult = result.createSubresult("create");
        provisioningService.addObject(account, null, null, task, creationResult);
        creationResult.close();
        String creationCaseOid = assertInProgress(creationResult);
        assertCaseState(creationCaseOid, SchemaConstants.CASE_STATE_OPEN);

        and("black account is renamed");
        OperationResult modificationResult = result.createSubresult("modify");
        provisioningService.modifyObject(
                ShadowType.class,
                ACCOUNT_BLACK_OID,
                deltaFor(ShadowType.class)
                        .item(
                                getAttributePath(ATTR_USERNAME),
                                getAttributeDefinition(resource, ATTR_USERNAME_QNAME))
                        .replace("white")
                        .asItemDeltas(),
                null,
                null,
                task,
                modificationResult);
        modificationResult.close();
        String modificationCaseOid = assertInProgress(modificationResult);
        assertCaseState(modificationCaseOid, SchemaConstants.CASE_STATE_OPEN);

        and("manual cases are closed");
        closeCase(creationCaseOid, OperationResultStatusType.SUCCESS);
        closeCase(modificationCaseOid, OperationResultStatusType.SUCCESS);

        when("shadow is refreshed");
        OperationResult refreshResult = result.createSubresult("refresh");
        provisioningService.refreshShadow(
                getShadowRepoLegacy(ACCOUNT_BLACK_OID),
                null,
                task,
                refreshResult);
        refreshResult.close();

        then("the operation should be successful");
        assertSuccess(refreshResult);

        and("the shadow is renamed");
        assertRepoShadow(ACCOUNT_BLACK_OID)
                .assertName("white");
    }

    // TODO: create, close case, then update backing store.

    // TODO: let grace period expire without updating the backing store (semi-manual-only)

    private void assertPendingOperationDeltas(PrismObject<ShadowType> shadow, int expectedNumber) {
        List<PendingOperationType> pendingOperations = shadow.asObjectable().getPendingOperation();
        assertEquals("Wrong number of pending operations in " + shadow, expectedNumber, pendingOperations.size());
    }

    private PendingOperationType assertSinglePendingOperation(PrismObject<ShadowType> shadow,
            XMLGregorianCalendar requestStart, XMLGregorianCalendar requestEnd) {
        return assertSinglePendingOperation(shadow, requestStart, requestEnd,
                OperationResultStatusType.IN_PROGRESS, null, null);
    }

    private PendingOperationType assertSinglePendingOperation(PrismObject<ShadowType> shadow,
            XMLGregorianCalendar requestStart, XMLGregorianCalendar requestEnd,
            OperationResultStatusType expectedStatus,
            XMLGregorianCalendar completionStart, XMLGregorianCalendar completionEnd) {
        assertPendingOperationDeltas(shadow, 1);
        return assertPendingOperation(shadow, shadow.asObjectable().getPendingOperation().get(0),
                requestStart, requestEnd, expectedStatus, completionStart, completionEnd);
    }

    private PendingOperationType assertPendingOperation(
            PrismObject<ShadowType> shadow, PendingOperationType pendingOperation,
            XMLGregorianCalendar requestStart, XMLGregorianCalendar requestEnd) {
        return assertPendingOperation(shadow, pendingOperation, requestStart, requestEnd,
                OperationResultStatusType.IN_PROGRESS, null, null);
    }

    private PendingOperationType assertPendingOperation(
            PrismObject<ShadowType> shadow, PendingOperationType pendingOperation,
            XMLGregorianCalendar requestStart, XMLGregorianCalendar requestEnd,
            OperationResultStatusType expectedStatus,
            XMLGregorianCalendar completionStart, XMLGregorianCalendar completionEnd) {
        assertNotNull("No operation ", pendingOperation);

        ObjectDeltaType deltaType = pendingOperation.getDelta();
        assertNotNull("No delta in pending operation in " + shadow, deltaType);
        // TODO: check content of pending operations in the shadow

        TestUtil.assertBetween("request timestamp in pending operation in " + shadow, requestStart, requestEnd, pendingOperation.getRequestTimestamp());

        OperationResultStatusType status = pendingOperation.getResultStatus();
        assertEquals("Wrong status in pending operation in " + shadow, expectedStatus, status);

        if (expectedStatus != OperationResultStatusType.IN_PROGRESS) {
            TestUtil.assertBetween("completion timestamp in pending operation in " + shadow, completionStart, completionEnd, pendingOperation.getCompletionTimestamp());
        }

        return pendingOperation;
    }

    private PendingOperationType findPendingOperation(PrismObject<ShadowType> shadow,
            OperationResultStatusType expectedResult, ItemPath itemPath) {
        List<PendingOperationType> pendingOperations = shadow.asObjectable().getPendingOperation();
        for (PendingOperationType pendingOperation : pendingOperations) {
            OperationResultStatusType result = pendingOperation.getResultStatus();
            if (result == null) {
                result = OperationResultStatusType.IN_PROGRESS;
            }
            if (pendingOperation.getResultStatus() != expectedResult) {
                continue;
            }
            if (itemPath == null) {
                return pendingOperation;
            }
            ObjectDeltaType delta = pendingOperation.getDelta();
            assertNotNull("No delta in pending operation in " + shadow, delta);
            for (ItemDeltaType itemDelta : delta.getItemDelta()) {
                ItemPath deltaPath = itemDelta.getPath().getItemPath();
                if (itemPath.equivalent(deltaPath)) {
                    return pendingOperation;
                }
            }
        }
        return null;
    }

    protected <T> void assertNoAttribute(PrismObject<ShadowType> shadow, QName attrName) {
        assertNoAttribute(shadow.asObjectable(), attrName);
    }

    protected void assertAttributeFromCache(ShadowAsserter<Void> shadowRepoAsserter, QName attrQName,
            String... attrVals) {
        assertAttributeFromCache(shadowRepoAsserter.getObject(), attrQName, attrVals);
    }

    protected void assertAttributeFromCache(PrismObject<ShadowType> shadow, QName attrQName,
            String... attrVals) {
        if (supportsBackingStore()) {
            assertNoAttribute(shadow, attrQName);
        } else {
            assertAttribute(shadow, attrQName, attrVals);
        }
    }

    protected void assertAttributeFromBackingStore(PrismObject<ShadowType> shadow, QName attrQName,
            String... attrVals) {
        if (supportsBackingStore()) {
            assertAttribute(shadow, attrQName, attrVals);
        } else {
            assertNoAttribute(shadow, attrQName);
        }
    }

    protected void assertShadowActivationAdministrativeStatusFromCache(ShadowAsserter<Void> shadowRepoAsserter, ActivationStatusType expectedStatus) {
        assertShadowActivationAdministrativeStatusFromCache(shadowRepoAsserter.getObject(), expectedStatus);
    }

    protected void assertShadowActivationAdministrativeStatusFromCache(PrismObject<ShadowType> shadow, ActivationStatusType expectedStatus) {
        if (supportsBackingStore()) {
            assertShadowActivationAdministrativeStatus(shadow, null);
        } else {
            assertShadowActivationAdministrativeStatus(shadow, expectedStatus);
        }
    }

    protected void assertShadowActivationAdministrativeStatus(PrismObject<ShadowType> shadow, ActivationStatusType expectedStatus) {
        assertActivationAdministrativeStatus(shadow, expectedStatus);
    }

    protected void assertShadowPassword(PrismObject<ShadowType> shadow) {
        // pure manual resource should never "read" password
        if (isNoPasswordExpected()) {
            assertNoShadowPassword(shadow);
        }
    }

    private void assertManual(AbstractWriteCapabilityType cap) {
        assertEquals("Manual flag not set in capability " + cap, Boolean.TRUE, cap.isManual());
    }

}
