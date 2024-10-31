/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.manual;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.polystring.PolyString;

import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.test.asserter.RepoShadowAsserter;

import com.evolveum.midpoint.util.exception.ConfigurationException;

import jakarta.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.schema.processor.*;

import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import com.evolveum.midpoint.model.intest.AbstractConfiguredModelIntegrationTest;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.ShadowAsserter;
import com.evolveum.midpoint.test.util.ParallelTestThread;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.tools.testng.UnusedTestElement;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.*;

/**
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public abstract class AbstractManualResourceTest extends AbstractConfiguredModelIntegrationTest {

    protected static final File TEST_DIR = new File("src/test/resources/manual/");

    public static final QName RESOURCE_ACCOUNT_OBJECTCLASS = RI_ACCOUNT_OBJECT_CLASS;

    protected static final String NS_MANUAL_CONF = "http://midpoint.evolveum.com/xml/ns/public/connector/builtin-1/bundle/com.evolveum.midpoint.provisioning.ucf.impl.builtin/ManualConnector";
    protected static final ItemName CONF_PROPERTY_DEFAULT_ASSIGNEE_QNAME = new ItemName(NS_MANUAL_CONF, "defaultAssignee");

    protected static final File USER_PHANTOM_FILE = new File(TEST_DIR, "user-phantom.xml");
    protected static final String USER_PHANTOM_OID = "5b12cc6e-575c-11e8-bc16-3744f9bfcac8";
    public static final String USER_PHANTOM_USERNAME = "phantom";
    public static final String USER_PHANTOM_FULL_NAME = "Thomas Phantom";
    public static final String USER_PHANTOM_FULL_NAME_WRONG = "Tom Funtom";
    public static final String ACCOUNT_PHANTOM_DESCRIPTION_MANUAL = "Phantom menace of the opera";
    public static final String ACCOUNT_PHANTOM_PASSWORD_MANUAL = "PhanthomaS";

    protected static final File USER_PHOENIX_FILE = new File(TEST_DIR, "user-phoenix.xml");
    protected static final String USER_PHOENIX_OID = "ed2ca15a-5ccb-11e8-b62d-4b94763188e4";
    public static final String USER_PHOENIX_USERNAME = "phoenix";
    public static final String USER_PHOENIX_FULL_NAME = "Phoebe Phoenix";
    public static final String ACCOUNT_PHOENIX_DESCRIPTION_MANUAL = "from the ashes";
    public static final String ACCOUNT_PHOENIX_PASSWORD_MANUAL = "VtakOhnivak";

    protected static final TestObject<UserType> USER_PHOENIX_2 =
            TestObject.file(TEST_DIR, "user-phoenix-2.xml", "e22bc5ed-0e31-4391-9cc8-1fbaa9a9dfeb");

    protected static final String USER_WILL_NAME = "will";
    protected static final String USER_WILL_GIVEN_NAME = "Will";
    protected static final String USER_WILL_FAMILY_NAME = "Turner";
    protected static final String USER_WILL_FULL_NAME = "Will Turner";
    protected static final String USER_WILL_FULL_NAME_PIRATE = "Pirate Will Turner";
    protected static final String ACCOUNT_WILL_DESCRIPTION_MANUAL = "manual";
    protected static final String USER_WILL_PASSWORD_OLD = "3lizab3th";
    protected static final String USER_WILL_PASSWORD_NEW = "ELIZAbeth";

    protected static final String ACCOUNT_JACK_DESCRIPTION_MANUAL = "Manuel";
    protected static final String USER_JACK_PASSWORD_OLD = "deadM3NtellN0tales";

    protected static final File TASK_SHADOW_REFRESH_FILE = new File(TEST_DIR, "task-shadow-refresh.xml");
    protected static final String TASK_SHADOW_REFRESH_OID = "eb8f5be6-2b51-11e7-848c-2fd84a283b03";

    protected static final String ATTR_USERNAME = "username";
    protected static final QName ATTR_USERNAME_QNAME = new QName(MidPointConstants.NS_RI, ATTR_USERNAME);

    protected static final String ATTR_FULLNAME = "fullname";
    protected static final QName ATTR_FULLNAME_QNAME = new QName(MidPointConstants.NS_RI, ATTR_FULLNAME);

    protected static final String ATTR_INTERESTS = "interests";
    protected static final QName ATTR_INTERESTS_QNAME = new QName(MidPointConstants.NS_RI, ATTR_INTERESTS);

    protected static final String ATTR_DESCRIPTION = "description";
    protected static final QName ATTR_DESCRIPTION_QNAME = new QName(MidPointConstants.NS_RI, ATTR_DESCRIPTION);

    protected static final String INTEREST_ONE = "one";

    protected BackingStore backingStore;

    protected PrismObject<ResourceType> resource;
    protected ResourceType resourceType;

    protected String userWillOid;
    protected String accountWillOid;

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

    protected String accountJackOid;
    protected String accountDrakeOid;

    protected XMLGregorianCalendar accountJackReqestTimestampStart;
    protected XMLGregorianCalendar accountJackReqestTimestampEnd;

    protected XMLGregorianCalendar accountJackCompletionTimestampStart;
    protected XMLGregorianCalendar accountJackCompletionTimestampEnd;

    protected String jackLastCaseOid;

    private String lastResourceVersion;

    protected String phoenixLastCaseOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        backingStore = createBackingStore();
        if (backingStore != null) {
            backingStore.initialize();
        }
        displayValue("Backing store", backingStore);

        repoAddObjectFromFile(SECURITY_POLICY_FILE, initResult);

        importObjectFromFile(getResourceFile(), initResult);

        importObjectFromFile(getRoleOneFile(), initResult);
        importObjectFromFile(getRoleTwoFile(), initResult);

        addObject(USER_JACK_FILE);
        addObject(USER_DRAKE_FILE);

        PrismObject<UserType> userWill = createUserWill();
        addObject(userWill, initTask, initResult);
        display("User will", userWill);
        userWillOid = userWill.getOid();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        setConflictResolutionAction(UserType.COMPLEX_TYPE, null, ConflictResolutionActionType.RECOMPUTE, initResult);

        // Turns on checks for connection in manual connector
        InternalsConfig.setSanityChecks(true);

    }

    protected BackingStore createBackingStore() {
        return null;
    }

    private PrismObject<UserType> createUserWill() throws SchemaException {
        PrismObject<UserType> user = prismContext.createObject(UserType.class);
        user.asObjectable()
                .name(USER_WILL_NAME)
                .givenName(USER_WILL_GIVEN_NAME)
                .familyName(USER_WILL_FAMILY_NAME)
                .fullName(USER_WILL_FULL_NAME)
                .beginActivation().administrativeStatus(ActivationStatusType.ENABLED).<UserType>end()
                .beginCredentials().beginPassword().setValue(new ProtectedStringType().clearValue(USER_WILL_PASSWORD_OLD));
        return user;
    }

    protected abstract File getResourceFile();

    protected abstract String getResourceOid();

    protected abstract File getRoleOneFile();

    protected abstract String getRoleOneOid();

    protected abstract File getRoleTwoFile();

    protected abstract String getRoleTwoOid();

    protected boolean supportsBackingStore() {
        return backingStore != null;
    }

    protected boolean hasMultivalueInterests() {
        return true;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean isDisablingInsteadOfDeletion() {
        return false;
    }

    @Test
    public void test000Sanity() throws Exception {
        OperationResult result = getTestOperationResult();

        ResourceType repoResource = repositoryService.getObject(
                ResourceType.class, getResourceOid(), null, result).asObjectable();
        assertNotNull("No connector ref", repoResource.getConnectorRef());

        String connectorOid = repoResource.getConnectorRef().getOid();
        assertNotNull("No connector ref OID", connectorOid);
        ConnectorType repoConnector = repositoryService
                .getObject(ConnectorType.class, connectorOid, null, result).asObjectable();
        assertNotNull(repoConnector);
        display("Manual Connector", repoConnector);

        // Check connector schema
        IntegrationTestTools.assertConnectorSchemaSanity(repoConnector);

        PrismObject<UserType> userWill = getUser(userWillOid);
        assertUser(userWill, userWillOid, USER_WILL_NAME, USER_WILL_FULL_NAME, USER_WILL_GIVEN_NAME, USER_WILL_FAMILY_NAME);
        assertAdministrativeStatus(userWill, ActivationStatusType.ENABLED);
        assertUserPassword(userWill, USER_WILL_PASSWORD_OLD);
    }

    @Test
    public void test012TestConnection() throws Exception {
        testConnection(false);
    }

    public void testConnection(boolean initialized) throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Check that there is a schema, but no capabilities before test (pre-condition)
        ResourceType resourceBefore = repositoryService.getObject(ResourceType.class, getResourceOid(),
                null, result).asObjectable();

        Element resourceXsdSchemaElementBefore = ResourceTypeUtil.getResourceXsdSchemaElement(resourceBefore);
        if (!initialized) {
            assertResourceSchemaBeforeTest(resourceXsdSchemaElementBefore);
        }

        CapabilitiesType capabilitiesBefore = resourceBefore.getCapabilities();
        if (initialized || nativeCapabilitiesEntered()) {
            AssertJUnit.assertNotNull("Native capabilities missing before test connection. Bad test setup?", capabilitiesBefore);
            AssertJUnit.assertNotNull("Native capabilities missing before test connection. Bad test setup?", capabilitiesBefore.getNative());
        } else {
            if (capabilitiesBefore != null) {
                AssertJUnit.assertNull("Native capabilities present before test connection. Bad test setup?", capabilitiesBefore.getNative());
            }
        }

        // WHEN
        when();
        OperationResult testResult = modelService.testResource(getResourceOid(), task, result);

        // THEN
        then();
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
        displayValue("Resource XML after test connection", resourceXml);

        CachingMetadataType schemaCachingMetadata = xmlSchemaTypeAfter.getCachingMetadata();
        assertNotNull("No caching metadata", schemaCachingMetadata);
        assertNotNull("No retrievalTimestamp", schemaCachingMetadata.getRetrievalTimestamp());
        assertNotNull("No serialNumber", schemaCachingMetadata.getSerialNumber());

        Element xsdElement = ObjectTypeUtil.findXsdElement(xmlSchemaTypeAfter);
        ResourceSchema parsedSchema = ResourceSchemaFactory.parseNativeSchemaAsBare(xsdElement);
        assertNotNull("No schema after parsing", parsedSchema);

        CapabilitiesType capabilitiesRepoAfter = resourceTypeRepoAfter.getCapabilities();
        AssertJUnit.assertNotNull("Capabilities missing after test connection.", capabilitiesRepoAfter);
        AssertJUnit.assertNotNull("Native capabilities missing after test connection.", capabilitiesRepoAfter.getNative());
        AssertJUnit.assertNotNull("Capabilities caching metadata missing after test connection.", capabilitiesRepoAfter.getCachingMetadata());

        ResourceType resourceModelAfter = modelService
                .getObject(ResourceType.class, getResourceOid(), null, task, result)
                .asObjectable();

        rememberSteadyResources();

        CapabilitiesType capabilitiesModelAfter = resourceModelAfter.getCapabilities();
        AssertJUnit.assertNotNull("Capabilities missing after test connection (model)", capabilitiesModelAfter);
        AssertJUnit.assertNotNull("Native capabilities missing after test connection (model)", capabilitiesModelAfter.getNative());
        AssertJUnit.assertNotNull("Capabilities caching metadata missing after test connection (model)", capabilitiesModelAfter.getCachingMetadata());
    }

    protected boolean nativeCapabilitiesEntered() {
        return false;
    }

    protected abstract void assertResourceSchemaBeforeTest(Element resourceXsdSchemaElementBefore);

    @Test
    public void test014Configuration() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        resource = modelService.getObject(ResourceType.class, getResourceOid(), null, task, result);
        resourceType = resource.asObjectable();

        PrismContainer<Containerable> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
        assertNotNull("No configuration container", configurationContainer);
        PrismContainerDefinition<?> confContDef = configurationContainer.getDefinition();
        assertNotNull("No configuration container definition", confContDef);
        PrismProperty<String> propDefaultAssignee = configurationContainer.findProperty(CONF_PROPERTY_DEFAULT_ASSIGNEE_QNAME);
        assertNotNull("No defaultAssignee conf prop", propDefaultAssignee);

        assertSteadyResources();
    }

    @Test
    public void test016ParsedSchema() throws Exception {
        // GIVEN

        // THEN
        // The returned type should have the schema pre-parsed
        assertTrue(ResourceSchemaFactory.hasParsedSchema(resourceType));

        // Also test if the utility method returns the same thing
        ResourceSchema resourceSchema = ResourceSchemaFactory.getBareSchema(resourceType);

        displayDumpable("Parsed resource schema", resourceSchema);

        // Check whether it is reusing the existing schema and not parsing it all over again
        // Not equals() but == ... we want to really know if exactly the same
        // object instance is returned
        assertSame("Broken caching",
                resourceSchema.getNativeSchema(),
                ResourceSchemaFactory.getBareSchema(resourceType).getNativeSchema());

        ResourceObjectClassDefinition accountDef =
                resourceSchema.findObjectClassDefinition(RESOURCE_ACCOUNT_OBJECTCLASS);
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

        assertSteadyResources();
    }

    @Test
    public void test017Capabilities() throws Exception {
        testCapabilities();
    }

    public void testCapabilities() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // WHEN
        ResourceType resource = modelService
                .getObject(ResourceType.class, getResourceOid(), null, task, result)
                .asObjectable();

        // THEN
        display("Resource from model", resource);
        displayValue("Resource from model (XML)", PrismTestUtil.serializeToXml(resource));

        assertSteadyResources();

        CapabilitiesType capabilities = resource.getCapabilities();
        assertNotNull("Missing capability caching metadata", capabilities.getCachingMetadata());

        CapabilityCollectionType nativeCapabilities = capabilities.getNative();
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

        assertSteadyResources();
    }

    /**
     * MID-4472, MID-4174
     */
    @Test
    public void test018ResourceCaching() throws Exception {
        testResourceCaching();
    }

    public void testResourceCaching() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modelService.getObject(ResourceType.class, getResourceOid(),
                GetOperationOptions.createReadOnlyCollection(), task, result);

        assertSteadyResources();

        // WHEN
        when();
        modelService.getObject(ResourceType.class, getResourceOid(),
                GetOperationOptions.createReadOnlyCollection(), task, result);

        assertSteadyResources();

        // WHEN
        when();
        modelService.getObject(ResourceType.class, getResourceOid(), null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertSteadyResources();
    }

    @Test
    public void test020ReimportResource() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ImportOptionsType options = new ImportOptionsType();
        options.setOverwrite(true);

        // WHEN
        when();
        modelService.importObjectsFromFile(getResourceFile(), options, task, result);

        // THEN
        then();
        assertSuccess(result);
    }

    @Test
    public void test022TestConnection() throws Exception {
        testConnection(false);
    }

    @Test
    public void test027Capabilities() throws Exception {
        testCapabilities();
    }

    /**
     * MID-4472, MID-4174
     */
    @Test
    public void test028ResourceCaching() throws Exception {
        testResourceCaching();
    }

    @Test
    public void test030ReimportResourceAgain() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ImportOptionsType options = new ImportOptionsType();
        options.setOverwrite(true);

        // WHEN
        when();
        modelService.importObjectsFromFile(getResourceFile(), options, task, result);

        // THEN
        then();
        assertSuccess(result);
    }

    // This time simply try to use the resource without any attempt to test connection
    @Test
    public void test032UseResource() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndKindIntent(getResourceOid(), ShadowKindType.ACCOUNT, null);

        // WHEN
        when();
        SearchResultList<PrismObject<ShadowType>> accounts = modelService.searchObjects(ShadowType.class, query, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        display("Found accounts", accounts);
        assertEquals("unexpected accounts: " + accounts, 0, accounts.size());

        rememberSteadyResources();

        PrismObject<ResourceType> resourceRepoAfter = repositoryService.getObject(ResourceType.class, getResourceOid(), null, result);
        ResourceType resourceTypeRepoAfter = resourceRepoAfter.asObjectable();
        display("Resource after test", resourceTypeRepoAfter);

        XmlSchemaType xmlSchemaTypeAfter = resourceTypeRepoAfter.getSchema();
        assertNotNull("No schema after test connection", xmlSchemaTypeAfter);
        Element resourceXsdSchemaElementAfter = ResourceTypeUtil.getResourceXsdSchemaElement(resourceTypeRepoAfter);
        assertNotNull("No schema after test connection", resourceXsdSchemaElementAfter);

        String resourceXml = prismContext.xmlSerializer().serialize(resourceRepoAfter);
        displayValue("Resource XML after test connection", resourceXml);

        CachingMetadataType schemaCachingMetadata = xmlSchemaTypeAfter.getCachingMetadata();
        assertNotNull("No schema caching metadata", schemaCachingMetadata);
        assertNotNull("No schema caching metadata retrievalTimestamp", schemaCachingMetadata.getRetrievalTimestamp());
        assertNotNull("No schema caching metadata serialNumber", schemaCachingMetadata.getSerialNumber());

        ResourceType resourceModelAfter =
                modelService.getObject(ResourceType.class, getResourceOid(), null, task, result).asObjectable();

        Element xsdElement = ObjectTypeUtil.findXsdElement(xmlSchemaTypeAfter);
        ResourceSchema parsedSchema = ResourceSchemaFactory.parseNativeSchemaAsBare(xsdElement);
        assertNotNull("No schema after parsing", parsedSchema);
        CapabilitiesType capabilitiesRepoAfter = resourceTypeRepoAfter.getCapabilities();
        AssertJUnit.assertNotNull("Capabilities missing after test connection.", capabilitiesRepoAfter);
        AssertJUnit.assertNotNull("Native capabilities missing after test connection.", capabilitiesRepoAfter.getNative());
        AssertJUnit.assertNotNull("Capabilities caching metadata missing after test connection.", capabilitiesRepoAfter.getCachingMetadata());

        CapabilitiesType capabilitiesModelAfter = resourceModelAfter.getCapabilities();
        AssertJUnit.assertNotNull("Capabilities missing after test connection (model)", capabilitiesModelAfter);
        AssertJUnit.assertNotNull("Native capabilities missing after test connection (model)", capabilitiesModelAfter.getNative());
        AssertJUnit.assertNotNull("Capabilities caching metadata missing after test connection (model)", capabilitiesModelAfter.getCachingMetadata());
    }

    @Test
    public void test037Capabilities() throws Exception {
        testCapabilities();
    }

    /**
     * MID-4472, MID-4174
     */
    @Test
    public void test038ResourceCaching() throws Exception {
        testResourceCaching();
    }

    /**
     * Make sure the resource is fully initialized, so steady resource asserts
     * in subsequent tests will work as expected.
     */
    @Test
    public void test099TestConnection() throws Exception {
        testConnection(true);
    }

    @Test
    public void test100AssignWillRoleOne() throws Exception {
        // The count will be checked only after propagation was run, so we can address both direct and grouping cases
        rememberCounter(InternalCounters.CONNECTOR_MODIFICATION_COUNT);

        assignWillRoleOne(USER_WILL_FULL_NAME, PendingOperationExecutionStatusType.EXECUTION_PENDING);

        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 0, 1);
        assertSteadyResources();
    }

    @Test
    public void test101GetAccountWillFuture() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE));

        // WHEN
        when();
        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class, accountWillOid, options, task, result);

        // THEN
        then();
        assertSuccess(result);

        display("Model shadow", shadowModel);
        ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
        assertShadowName(shadowModel, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME);
        assertAttribute(shadowModel, ATTR_INTERESTS_QNAME, INTEREST_ONE);
        assertNoAttribute(shadowModel, ATTR_DESCRIPTION_QNAME);
        assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
        assertShadowExists(shadowModel, true);
        // TODO
//        assertShadowPassword(shadowProvisioning);

        assertSteadyResources();
    }

    @Test
    public void test102RecomputeWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        recomputeUser(userWillOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertAccountWillAfterAssign(USER_WILL_FULL_NAME, PendingOperationExecutionStatusType.EXECUTION_PENDING);

        assertSteadyResources();
    }

    /**
     * ff 2min, run propagation task. Grouping resources should execute the operations now.
     */
    @Test
    public void test103RunPropagation() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT2M");

        // WHEN
        when();
        runPropagation();

        // THEN
        then();
        assertSuccess(result);

        assertCounterIncrement(InternalCounters.CONNECTOR_MODIFICATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 0, 1);

        assertAccountWillAfterAssign(USER_WILL_FULL_NAME, PendingOperationExecutionStatusType.EXECUTING);

        assertSteadyResources();
    }

    @Test
    public void test104RecomputeWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        rememberCounter(InternalCounters.CONNECTOR_MODIFICATION_COUNT);

        // WHEN
        when();
        recomputeUser(userWillOid, task, result);

        // THEN
        then();
        assertSuccess(result);
        assertCounterIncrement(InternalCounters.CONNECTOR_MODIFICATION_COUNT, 0);

        assertAccountWillAfterAssign(USER_WILL_FULL_NAME, PendingOperationExecutionStatusType.EXECUTING);

        assertSteadyResources();
    }

    /**
     * Operation is executing. Propagation has nothing to do.
     * Make sure nothing is done even if propagation task is run.
     */
    @Test
    public void test105RunPropagationAgain() throws Exception {
        // GIVEN
        rememberCounter(InternalCounters.CONNECTOR_MODIFICATION_COUNT);

        // WHEN
        when();
        runPropagation();

        // THEN
        then();
        assertCounterIncrement(InternalCounters.CONNECTOR_MODIFICATION_COUNT, 0);

        assertAccountWillAfterAssign(USER_WILL_FULL_NAME, PendingOperationExecutionStatusType.EXECUTING);

        assertSteadyResources();
    }

    /**
     * Case is still open. But we add object to the backing store anyway.
     * This simulates a lazy admin that forgets to close the case.
     */
    @Test
    public void test106AddToBackingStoreAndGetAccountWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        backingStoreProvisionWill(INTEREST_ONE);
        displayBackingStore();

        // WHEN
        when();
        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class, accountWillOid, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        display("Model shadow", shadowModel);
        ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
        assertShadowName(shadowModel, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME);
        assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
        assertShadowExists(shadowModel, supportsBackingStore());
        assertShadowPassword(shadowModel);

        var repoShadow = getShadowRepo(accountWillOid);
        var repoShadowObj = RepoShadowAsserter.forRepoShadow(repoShadow, getCachedAttributes())
                .display()
                .assertCachedOrigValues(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .assertCachedOrigValues(ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME)
                .assertCachedOrigValues(ATTR_DESCRIPTION_QNAME)
                .assertNoPasswordIf(!isCaching())
                .getObject();

        assertSinglePendingOperation(repoShadowObj, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);
        assertShadowActivationAdministrativeStatusFromCache(repoShadowObj, ActivationStatusType.ENABLED);
        assertShadowExists(repoShadowObj, supportsBackingStore());

        assertSteadyResources();
    }

    Collection<? extends QName> getCachedAttributes() throws SchemaException, ConfigurationException {
        return getAccountDefinition().getAllSimpleAttributesNames();
    }

    @NotNull ResourceObjectDefinition getAccountDefinition() throws SchemaException, ConfigurationException {
        return Resource.of(resource)
                .getCompleteSchemaRequired()
                .findDefinitionForObjectClassRequired(RI_ACCOUNT_OBJECT_CLASS);
    }

    @Test
    public void test108GetAccountWillFuture() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE));

        // WHEN
        when();
        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class, accountWillOid, options, task, result);

        // THEN
        then();
        assertSuccess(result);

        display("Model shadow", shadowModel);
        ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
        assertShadowName(shadowModel, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME);
        assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
        assertShadowExists(shadowModel, true);
        // TODO
//        assertShadowPassword(shadowProvisioning);

        var repoShadow = getShadowRepo(accountWillOid);
        var repoShadowObj = RepoShadowAsserter.forRepoShadow(repoShadow, getCachedAttributes())
                .display()
                .assertCachedOrigValues(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .assertCachedOrigValues(ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME)
                .assertCachedOrigValues(ATTR_DESCRIPTION_QNAME)
                .assertNoPasswordIf(!isCaching())
                .getObject();

        assertSinglePendingOperation(repoShadowObj, accountWillReqestTimestampStart, accountWillReqestTimestampEnd);
        assertShadowActivationAdministrativeStatusFromCache(repoShadowObj, ActivationStatusType.ENABLED);
        assertShadowExists(repoShadowObj, supportsBackingStore());

        assertSteadyResources();
    }

    /**
     * Case is closed. The operation is complete.
     */
    @Test
    public void test110CloseCaseAndRecomputeWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        closeCase(willLastCaseOid);

        accountWillCompletionTimestampStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        // We need reconcile and not recompute here. We need to fetch the updated case status.
        reconcileUser(userWillOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        accountWillCompletionTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        assertWillAfterCreateCaseClosed(true);

        assertSteadyResources();
    }

    /**
     * Case is closed. The operation is complete. Nothing to do.
     * Make sure nothing is done even if propagation task is run.
     */
    @Test
    public void test114RunPropagation() throws Exception {
        // GIVEN
        rememberCounter(InternalCounters.CONNECTOR_MODIFICATION_COUNT);

        // WHEN
        when();
        runPropagation();

        // THEN
        then();
        assertCounterIncrement(InternalCounters.CONNECTOR_MODIFICATION_COUNT, 0);

        assertWillAfterCreateCaseClosed(true);

        assertSteadyResources();
    }

    /**
     * ff 5min, everything should be the same (grace not expired yet)
     */
    @Test
    public void test120RecomputeWillAfter5min() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT5M");

        // WHEN
        when();
        recomputeUser(userWillOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertRepoShadow(accountWillOid)
                .pendingOperations()
                .singleOperation()
                .assertRequestTimestamp(accountWillReqestTimestampStart, accountWillReqestTimestampEnd)
                .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                .assertResultStatus(OperationResultStatusType.SUCCESS)
                .assertCompletionTimestamp(accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

        assertModelShadow(accountWillOid)
                .pendingOperations()
                .singleOperation()
                .assertRequestTimestamp(accountWillReqestTimestampStart, accountWillReqestTimestampEnd)
                .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                .assertResultStatus(OperationResultStatusType.SUCCESS)
                .assertCompletionTimestamp(accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);

        assertSteadyResources();
    }

    /**
     * ff 20min, grace should expire. But operation is still kept in the shadow.
     * Retention period is longer.
     */
    @Test
    public void test130RecomputeWillAfter25min() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT20M");

        // WHEN
        when();
        recomputeUser(userWillOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertRepoShadow(accountWillOid)
                .pendingOperations()
                .singleOperation()
                .assertRequestTimestamp(accountWillReqestTimestampStart, accountWillReqestTimestampEnd)
                .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                .assertResultStatus(OperationResultStatusType.SUCCESS)
                .assertCompletionTimestamp(accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

        assertModelShadow(accountWillOid)
                .pendingOperations()
                .singleOperation()
                .assertRequestTimestamp(accountWillReqestTimestampStart, accountWillReqestTimestampEnd)
                .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                .assertResultStatus(OperationResultStatusType.SUCCESS)
                .assertCompletionTimestamp(accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);

        assertSteadyResources();
    }

    /**
     * ff 7min, pending operation retention period is over.
     * Pending operations should be gone.
     */
    @Test
    public void test132RecomputeWillAfter32min() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT7M");

        // WHEN
        when();
        recomputeUser(userWillOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertRepoShadow(accountWillOid)
                .pendingOperations()
                .assertNone();

        assertModelShadow(accountWillOid)
                .pendingOperations()
                .assertNone();

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);

        assertSteadyResources();
    }

    @Test
    public void test200ModifyUserWillFullname() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object().createModificationReplaceProperty(ShadowType.class,
                accountWillOid, ItemPath.create(ShadowType.F_ATTRIBUTES, ATTR_FULLNAME_QNAME),
                USER_WILL_FULL_NAME_PIRATE);
        displayDumpable("ObjectDelta", delta);

        accountWillReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        modifyUserReplace(userWillOid, UserType.F_FULL_NAME, task, result, PolyString.fromOrig(USER_WILL_FULL_NAME_PIRATE));

        // THEN
        then();
        display("result", result);
        willLastCaseOid = assertInProgress(result);

        accountWillReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        assertAccountWillAfterFullNameModification(PendingOperationExecutionStatusType.EXECUTION_PENDING);

        assertSteadyResources();
    }

    @Test
    public void test202RecomputeWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        recomputeUser(userWillOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertAccountWillAfterFullNameModification(PendingOperationExecutionStatusType.EXECUTION_PENDING);

        assertSteadyResources();
    }

    @Test
    public void test203RunPropagation() throws Exception {
        // GIVEN

        clockForward("PT2M");

        // WHEN
        when();
        runPropagation();

        // THEN
        then();

        assertAccountWillAfterFullNameModification(PendingOperationExecutionStatusType.EXECUTING);

        assertSteadyResources();
    }

    @Test
    public void test204RecomputeWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        recomputeUser(userWillOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertAccountWillAfterFullNameModification(PendingOperationExecutionStatusType.EXECUTING);

        assertSteadyResources();
    }

    /**
     * Case is closed. The operation is complete.
     */
    @Test
    public void test206CloseCaseAndRecomputeWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        closeCase(willLastCaseOid);

        accountWillCompletionTimestampStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        refreshShadowIfNeeded(accountWillOid);
        recomputeUser(userWillOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        accountWillCompletionTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        var repoShadow = getShadowRepo(accountWillOid);
        var repoShadowObj = RepoShadowAsserter.forRepoShadow(repoShadow, getCachedAttributes())
                .display()
                .assertCachedOrigValues(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .assertCachedOrigValues(ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE)
                .getObject();

        assertSinglePendingOperation(repoShadowObj,
                accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
                OperationResultStatusType.SUCCESS,
                accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);
        assertShadowActivationAdministrativeStatusFromCache(repoShadowObj, ActivationStatusType.ENABLED);

        PrismObject<ShadowType> shadowProvisioning = modelService.getObject(ShadowType.class,
                accountWillOid, null, task, result);

        display("Model shadow", shadowProvisioning);
        ShadowType shadowTypeProvisioning = shadowProvisioning.asObjectable();
        assertShadowName(shadowProvisioning, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertShadowActivationAdministrativeStatus(shadowProvisioning, ActivationStatusType.ENABLED);
        assertAttribute(shadowProvisioning, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        if (supportsBackingStore()) {
            assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME);
        } else {
            assertAttribute(shadowProvisioning, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
        }
        assertAttributeFromBackingStore(shadowProvisioning, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowProvisioning);

        assertSinglePendingOperation(shadowProvisioning,
                accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
                OperationResultStatusType.SUCCESS,
                accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

        PrismObject<ShadowType> shadowProvisioningFuture = modelService.getObject(ShadowType.class,
                accountWillOid,
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
                task, result);
        display("Model shadow (future)", shadowProvisioningFuture);
        assertShadowName(shadowProvisioningFuture, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
        assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
        assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
        assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowProvisioningFuture);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);

        assertSteadyResources();
    }

    /**
     * ff 5min, everything should be the same (grace not expired yet)
     */
    @Test
    public void test210RecomputeWillAfter5min() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT5M");

        // WHEN
        when();
        recomputeUser(userWillOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
        display("Repo shadow", shadowRepo);
        assertSinglePendingOperation(shadowRepo,
                accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
                OperationResultStatusType.SUCCESS,
                accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
                accountWillOid, null, task, result);
        assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
        assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        if (supportsBackingStore()) {
            assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME);
        } else {
            assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
        }
        assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModel);

        assertSinglePendingOperation(shadowModel,
                accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
                OperationResultStatusType.SUCCESS,
                accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

        PrismObject<ShadowType> shadowModelFuture = modelService.getObject(ShadowType.class,
                accountWillOid,
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
                task, result);
        display("Model shadow (future)", shadowModelFuture);
        assertShadowName(shadowModelFuture, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowModelFuture.asObjectable().getKind());
        assertShadowActivationAdministrativeStatus(shadowModelFuture, ActivationStatusType.ENABLED);
        assertAttribute(shadowModelFuture, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        assertAttribute(shadowModelFuture, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
        assertAttributeFromBackingStore(shadowModelFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModelFuture);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);

        assertSteadyResources();
    }

    @Test
    public void test212UpdateBackingStoreAndGetAccountWill() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        backingStoreUpdateWill(USER_WILL_FULL_NAME_PIRATE, INTEREST_ONE, ActivationStatusType.ENABLED, USER_WILL_PASSWORD_OLD);

        // WHEN
        when();
        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
                accountWillOid, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
        assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
        assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModel);

        assertSinglePendingOperation(shadowModel,
                accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
                OperationResultStatusType.SUCCESS,
                accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

        PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountWillOid, null, result);
        display("Repo shadow", shadowRepo);
        assertSinglePendingOperation(shadowRepo,
                accountWillReqestTimestampStart, accountWillReqestTimestampEnd,
                OperationResultStatusType.SUCCESS,
                accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd);

        PrismObject<ShadowType> shadowModelFuture = modelService.getObject(ShadowType.class,
                accountWillOid,
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
                task, result);
        display("Model shadow (future)", shadowModelFuture);
        assertShadowName(shadowModelFuture, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowModelFuture.asObjectable().getKind());
        assertShadowActivationAdministrativeStatus(shadowModelFuture, ActivationStatusType.ENABLED);
        assertAttribute(shadowModelFuture, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        assertAttribute(shadowModelFuture, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
        assertAttributeFromBackingStore(shadowModelFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModelFuture);

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);

        assertSteadyResources();
    }

    /**
     * Create phantom account in the backing store. MidPoint does not know anything about it.
     * At the same time, there is phantom user that has the account assigned. But it is not yet
     * provisioned. MidPoint should find existing account and it it should figure out that
     * there is nothing to do. Just to link the account.
     * related to MID-4614
     */
    @Test
    public void test400PhantomAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        setupPhantom();

        // WHEN
        when();
        reconcileUser(USER_PHANTOM_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        // This should theoretically always return IN_PROGRESS, as there is
        // reconciliation operation going on. But due to various "peculiarities"
        // of a consistency mechanism this sometimes returns in progress and
        // sometimes it is just success.
        OperationResultStatus status = result.getStatus();
        if (status != OperationResultStatus.IN_PROGRESS && status != OperationResultStatus.SUCCESS) {
            fail("Unexpected result status in " + result);
        }

        // WHEN
        when();
        runPropagation();

        // THEN
        then();

        PrismObject<UserType> userAfter = getUser(USER_PHANTOM_OID);
        display("User after", userAfter);
        String shadowOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> shadowModel = getShadowModel(shadowOid);
        display("Shadow after", shadowModel);

        assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_PHANTOM_USERNAME);
        if (supportsBackingStore()) {
            assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_PHANTOM_FULL_NAME_WRONG);
        } else {
            assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_PHANTOM_FULL_NAME);
        }
        assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_PHANTOM_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModel);

        if (isDirect()) {
            assertSinglePendingOperation(shadowModel, PendingOperationExecutionStatusType.EXECUTING, OperationResultStatusType.IN_PROGRESS);
        } else {
            assertSinglePendingOperation(shadowModel, PendingOperationExecutionStatusType.EXECUTION_PENDING, null);
        }

        assertSteadyResources();
    }

    protected void setupPhantom() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        addObject(USER_PHANTOM_FILE);
        ObjectDelta<UserType> userDelta = createAccountAssignmentUserDelta(USER_PHANTOM_OID, getResourceOid(), null, true);
        repositoryService.modifyObject(UserType.class, USER_PHANTOM_OID, userDelta.getModifications(), result);
        PrismObject<UserType> userBefore = getUser(USER_PHANTOM_OID);
        display("User before", userBefore);

        backingStoreAddPhantom();
    }

    /**
     * MID-4587
     */
    @Test
    public void test410AssignPhoenixAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        addObject(USER_PHOENIX_FILE);

        // WHEN
        when();
        assignAccountToUser(USER_PHOENIX_OID, getResourceOid(), null, task, result);

        // THEN
        then();
        assertInProgress(result);

        // Make sure the operation will be picked up by propagation task
        clockForward("PT3M");

        // WHEN
        when();
        runPropagation();

        // THEN
        then();

        PrismObject<UserType> userAfter = getUser(USER_PHOENIX_OID);
        display("User after", userAfter);
        String shadowOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> shadowModel = getShadowModel(shadowOid);
        display("Shadow after", shadowModel);

        assertShadowNotDead(shadowModel);
        assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_PHOENIX_USERNAME);
        assertAttributeFromCache(shadowModel, ATTR_FULLNAME_QNAME, USER_PHOENIX_FULL_NAME);

        PendingOperationType pendingOperation = assertSinglePendingOperation(shadowModel, PendingOperationExecutionStatusType.EXECUTING, OperationResultStatusType.IN_PROGRESS);
        phoenixLastCaseOid = pendingOperation.getAsynchronousOperationReference();

        assertSteadyResources();
    }

    /**
     * MID-4587
     */
    @Test
    public void test412AddPhoenixToBackingStoreAndCloseTicket() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        backingStoreAddPhoenix();
        closeCase(phoenixLastCaseOid);

        // WHEN
        when();
        reconcileUser(USER_PHOENIX_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_PHOENIX_OID);
        display("User after", userAfter);
        String shadowOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> shadowModel = getShadowModel(shadowOid);
        display("Shadow after", shadowModel);

        assertShadowNotDead(shadowModel);
        assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_PHOENIX_USERNAME);
        assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_PHOENIX_FULL_NAME);
        assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_PHOENIX_DESCRIPTION_MANUAL);

        assertSinglePendingOperation(shadowModel, PendingOperationExecutionStatusType.COMPLETED, OperationResultStatusType.SUCCESS);

        assertSteadyResources();
    }

    /**
     * Let the pending operations expire. So we have simpler situation in following tests.
     * MID-4587
     */
    @Test
    public void test413PhoenixLetOperationsExpire() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        clockForward("PT1H");

        // WHEN
        when();
        reconcileUser(USER_PHOENIX_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> userAfter = getUser(USER_PHOENIX_OID);
        display("User after", userAfter);
        String shadowOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> shadowModel = getShadowModel(shadowOid);
        display("Shadow after", shadowModel);

        assertShadowNotDead(shadowModel);
        assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_PHOENIX_USERNAME);
        assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_PHOENIX_FULL_NAME);
        assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_PHOENIX_DESCRIPTION_MANUAL);

        assertNoPendingOperation(shadowModel);

        assertSteadyResources();
    }

    /**
     * Close case, the account is deleted. But it still *is* in the backing store.
     * MID-4587
     */
    @Test
    public void test414UnassignPhoenixAccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        unassignAccountFromUser(USER_PHOENIX_OID, getResourceOid(), null, task, result);

        // THEN
        then();
        assertInProgress(result);

        // Make sure the operation will be picked up by propagation task
        clockForward("PT3M");

        // WHEN
        when();
        runPropagation();

        // THEN
        then();

        PrismObject<UserType> userAfter = getUser(USER_PHOENIX_OID);
        display("User after", userAfter);
        String shadowOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> shadowModel = getShadowModel(shadowOid);
        display("Shadow after", shadowModel);

        assertShadowNotDead(shadowModel);
        assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_PHOENIX_USERNAME);

        PendingOperationType pendingOperation = assertSinglePendingOperation(shadowModel, PendingOperationExecutionStatusType.EXECUTING, OperationResultStatusType.IN_PROGRESS);
        phoenixLastCaseOid = pendingOperation.getAsynchronousOperationReference();

        assertSteadyResources();
    }

    /**
     * MID-4587
     */
    @Test
    public void test416PhoenixAccountUnassignCloseCase() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        closeCase(phoenixLastCaseOid);

        // WHEN
        when();
        reconcileUser(USER_PHOENIX_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        // Make sure the operation will be picked up by propagation task
        clockForward("PT3M");

        // WHEN
        when();
        runPropagation();

        // THEN
        then();

        PrismObject<UserType> userAfter = getUser(USER_PHOENIX_OID);
        display("User after", userAfter);
        String shadowOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> shadowModel = getShadowModel(shadowOid);
        display("Shadow after", shadowModel);

        assertShadowDead(shadowModel);
        assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_PHOENIX_USERNAME);

        assertSinglePendingOperation(shadowModel, PendingOperationExecutionStatusType.COMPLETED, OperationResultStatusType.SUCCESS);

        assertSteadyResources();
    }

    /**
     * Account assigned again. But it is still in backing store and still in grace period.
     * This is the core test for MID-4587.
     * MID-4587
     */
    @Test
    public void test418AssignPhoenixAccountAgain() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        assignAccountToUser(USER_PHOENIX_OID, getResourceOid(), null, task, result);

        // THEN
        then();
        phoenixLastCaseOid = assertInProgress(result);

        // Make sure the operation will be picked up by propagation task
        clockForward("PT3M");

        // WHEN
        when();
        runPropagation();

        // THEN
        then();

        PrismObject<UserType> userAfter = getUser(USER_PHOENIX_OID);
        display("User after", userAfter);

        // Yes, we really expect two shadows here.
        // First shadow is dead (the one from previous test).
        // Second shadow is new one.
        // MidPoint cannot collapse these two shadows to one, because they do not
        // necessarily represent the same account. E.g. the new account may have
        // different primary identifier (in case that those identifiers are generated).
        assertLinks(userAfter, 1, 1);

        int deadShadows = 0;
        int liveShadows = 0;
        for (ObjectReferenceType linkRef : userAfter.asObjectable().getLinkRef()) {
            try {

                PrismObject<ShadowType> shadowModel = getShadowModelNoFetch(linkRef.getOid());
                display("Shadow after", shadowModel);
                Boolean dead = shadowModel.asObjectable().isDead();
                if (dead != null && dead) {
                    deadShadows++;
                    assertSinglePendingOperation(shadowModel, PendingOperationExecutionStatusType.COMPLETED, OperationResultStatusType.SUCCESS);
                } else {
                    liveShadows++;
                    assertSinglePendingOperation(shadowModel, PendingOperationExecutionStatusType.EXECUTING, OperationResultStatusType.IN_PROGRESS);
                }

            } catch (ObjectNotFoundException e) {
                displayValue("Shadow after", "NOT FOUND: " + linkRef.getOid());
            }
        }
        assertEquals("Unexpected number of dead shadows", 1, deadShadows);
        assertEquals("Unexpected number of live shadows", 1, liveShadows);

        assertSteadyResources();
    }

    /**
     * Creates an account, then deletes and re-creates it again.
     *
     * Uses `phoenix-2` to avoid clashing with the original one.
     *
     * MID-8069
     */
    @Test
    public void test430CreateDeleteCreate() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        clock.resetOverride();

        given("there is a phoenix-2 user");
        addObject(USER_PHOENIX_2, task, result);

        when("the account is assigned to it");

        assignAccountToUser(USER_PHOENIX_2.oid, getResourceOid(), null, task, result);

        assertUser(USER_PHOENIX_2.oid, "after creation (not propagated)")
                .withObjectResolver(createSimpleModelObjectResolver())
                .singleLink()
                .resolveTarget()
                .display();

        clockForward("PT3M"); // Make sure the operation will be picked up by propagation task
        runPropagation();

        String shadowOid = getSingleLinkOid(getUser(USER_PHOENIX_2.oid));
        PrismObject<ShadowType> shadowAfterCreation = getShadowModel(shadowOid);
        display("Shadow after creation and propagation (model)", shadowAfterCreation);
        assertShadowNotDead(shadowAfterCreation);

        PendingOperationType pendingOperation = assertSinglePendingOperation(
                shadowAfterCreation, PendingOperationExecutionStatusType.EXECUTING, OperationResultStatusType.IN_PROGRESS);
        closeCase(pendingOperation.getAsynchronousOperationReference());

        PrismObject<ShadowType> shadowAfterCreationAndCaseClosure = getShadowModel(shadowOid);
        display("Shadow after creation, propagation, and case closure (model)",
                shadowAfterCreationAndCaseClosure);
        assertShadowExists(shadowAfterCreationAndCaseClosure, true);

        and("the account is deleted");

        unassignAccountFromUser(USER_PHOENIX_2.oid, getResourceOid(), null, task, result);

        assertUser(USER_PHOENIX_2.oid, "after deletion (not propagated)")
                .withObjectResolver(createSimpleModelObjectResolver())
                .singleLink()
                .resolveTarget()
                .display();

        if (!isDisablingInsteadOfDeletion()) {
            PrismObject<ShadowType> shadowAfterDeletionFuture = getShadowModelFuture(shadowOid);
            display("Shadow after deletion (model, future)", shadowAfterDeletionFuture);
            assertShadowDead(shadowAfterDeletionFuture);
        }

        and("the account is re-created (during grouping period) - with a changed property value");

        modifyUserReplace(
                USER_PHOENIX_2.oid,
                UserType.F_FULL_NAME,
                ModelExecuteOptions.create().raw(),
                task,
                result,
                PolyString.fromOrig("Phoenix The Second"));

        assignAccountToUser(USER_PHOENIX_2.oid, getResourceOid(), null, task, result);

        then("there is a consistent (newly created) shadow");
        PrismObject<UserType> userAfterRecreation = getUser(USER_PHOENIX_2.oid);
        display("User after re-creation", userAfterRecreation);

        List<ObjectReferenceType> linkRefs = userAfterRecreation.asObjectable().getLinkRef();
        if (!isDisablingInsteadOfDeletion()) {
            assertThat(linkRefs).as("link refs").hasSize(2);
            List<ObjectReferenceType> originalLinkRefs = linkRefs.stream()
                    .filter(ref -> ref.getOid().equals(shadowOid))
                    .collect(Collectors.toList());
            assertThat(originalLinkRefs).as("original link refs").hasSize(1);
            List<ObjectReferenceType> newLinkRefs = linkRefs.stream()
                    .filter(ref -> !ref.getOid().equals(shadowOid))
                    .collect(Collectors.toList());
            assertThat(newLinkRefs).as("other link refs").hasSize(1);
            ObjectReferenceType newLinkRef = newLinkRefs.get(0);

            PrismObject<ShadowType> shadowAfterRecreation = getShadowModel(newLinkRef.getOid());
            display("Newly-created shadow after re-creation", shadowAfterRecreation);
            PrismObject<ShadowType> originalShadowAfterRecreation = getShadowModel(shadowOid);
            display("Original shadow after re-creation", originalShadowAfterRecreation);
            PrismObject<ShadowType> originalShadowAfterRecreationFuture = getShadowModelFuture(shadowOid);
            display("Original shadow after re-creation (model, future)", originalShadowAfterRecreationFuture);
            assertShadowDead(originalShadowAfterRecreationFuture);
        } else {
            assertThat(linkRefs).as("link refs").hasSize(1);
            PrismObject<ShadowType> originalShadowAfterRecreation = getShadowModel(shadowOid);
            display("Original shadow after re-creation", originalShadowAfterRecreation);
        }

        // To avoid messing with downstream tests
        for (var linkRef : linkRefs) {
            repositoryService.deleteObject(ShadowType.class, linkRef.getOid(), result);
        }
    }

    /** If a case is deleted manually, midPoint should treat it gracefully. MID-9286. */
    @Test
    public void test440DeleteCaseManually() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var userName = getTestNameShort();

        given("there is a user");
        var user = new UserType()
                .name(userName)
                .fullName("Full name of " + userName)
                .assignment(new AssignmentType()
                        .construction(new ConstructionType()
                                .resourceRef(getResourceOid(), ResourceType.COMPLEX_TYPE)));
        var userOid = addObject(user, task, result);

        clockForward("PT3M"); // Make sure the operation will be picked up by propagation task
        runPropagation();

        var shadowOid = getSingleLinkOid(getUser(userOid));
        var shadowAfterCreation = getShadowModel(shadowOid);
        display("Shadow after creation and propagation (model)", shadowAfterCreation);

        var pendingOperation = assertSinglePendingOperation(
                shadowAfterCreation, PendingOperationExecutionStatusType.EXECUTING, OperationResultStatusType.IN_PROGRESS);
        var caseOid = pendingOperation.getAsynchronousOperationReference();
        assertCase(caseOid, "case after creation")
                .display();

        when("the case is deleted manually and user is recomputed");
        repositoryService.deleteObject(CaseType.class, caseOid, result);
        recomputeUser(userOid, task, result);

        then("everything is OK");
        assertInProgressOrSuccess(result);

        var shadowOidAfterRecomputation = getSingleLinkOid(getUser(userOid));
        var shadowAfterRecomputation = getShadowModel(shadowOidAfterRecomputation, null, false);
        display("Shadow after recomputation (model)", shadowAfterRecomputation);

        var pendingOperationAfterRecomputation = assertSinglePendingOperation(
                shadowAfterRecomputation, PendingOperationExecutionStatusType.EXECUTING, OperationResultStatusType.IN_PROGRESS);
        var caseOidAfterRecomputation = pendingOperationAfterRecomputation.getAsynchronousOperationReference();
        displayValue("Case OID after recomputation", caseOidAfterRecomputation);

        // There should be no removal/recreation of the shadow.
        assertThat(shadowOidAfterRecomputation).isEqualTo(shadowOid);

        // The current behavior is that the orphaned case OID is kept intact.
        // In the future, we may implement some auto-healing mechanism that would re-create the case.
        // For now, it is left to the administrator: you broke it, you fix it.
        assertThat(caseOidAfterRecomputation)
                .as("case OID after recomputation")
                .isEqualTo(caseOid);

        // To avoid messing with downstream tests
        repositoryService.deleteObject(ShadowType.class, shadowOidAfterRecomputation, result);
    }

    protected boolean are9xxTestsEnabled() {
        // Disabled by default. These are intense parallel tests and they will fail for resources
        // that do not have extra consistency checks and do not use proposed shadows.
        return false;
    }

    protected int getConcurrentTestNumberOfThreads() {
        return 4;
    }

    protected int getConcurrentTestRandomStartDelayRangeAssign() {
        return 1000;
    }

    protected int getConcurrentTestRandomStartDelayRangeUnassign() {
        return 5;
    }

    /**
     * Set up roles used in parallel tests.
     */
    @Test
    public void test900SetUpRoles() throws Exception {
        if (!are9xxTestsEnabled()) {
            displaySkip();
            return;
        }

        // GIVEN
        SystemConfigurationType systemConfiguration = getSystemConfiguration();
        display("System config", systemConfiguration);
        ConflictResolutionActionType conflictResolutionAction = systemConfiguration.getDefaultObjectPolicyConfiguration().get(0).getConflictResolution().getAction();
        if (!ConflictResolutionActionType.RECOMPUTE.equals(conflictResolutionAction) && !ConflictResolutionActionType.RECONCILE.equals(conflictResolutionAction)) {
            fail("Wrong conflict resolution action: " + conflictResolutionAction);
        }

        for (int i = 0; i < getConcurrentTestNumberOfThreads(); i++) {
            PrismObject<RoleType> role = parseObject(getRoleOneFile());
            role.setOid(getRoleOid(i));
            role.asObjectable().setName(createPolyStringType(getRoleName(i)));
            List<ResourceAttributeDefinitionType> outboundAttributes = role.asObjectable().getInducement().get(0).getConstruction().getAttribute();
            if (hasMultivalueInterests()) {
                ExpressionType outboundExpression = outboundAttributes.get(0).getOutbound().getExpression();
                JAXBElement jaxbElement = outboundExpression.getExpressionEvaluator().get(0);
                jaxbElement.setValue(getRoleInterest(i));
            } else {
                outboundAttributes.remove(0);
            }
            addObject(role);
        }
    }

    protected String getRoleOid(int i) {
        return String.format("f363260a-8d7a-11e7-bd67-%012d", i);
    }

    protected String getRoleName(int i) {
        return String.format("role-%012d", i);
    }

    protected String getRoleInterest(int i) {
        return String.format("i%012d", i);
    }

    // MID-4047, MID-4112
    @Test
    public void test910ConcurrentRolesAssign() throws Exception {
        if (!are9xxTestsEnabled()) {
            displaySkip();
            return;
        }

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_DRAKE_OID);
        display("user before", userBefore);
        assertLiveLinks(userBefore, 0);

        //noinspection CheckStyle
        final long TIMEOUT = 60000L;

        // WHEN
        when();
        String testName = getTestNameShort();
        ParallelTestThread[] threads = multithread(
                (i) -> {
                    login(userAdministrator);
                    Task localTask = createTask(testName + "-thread-" + i);

                    assignRole(USER_DRAKE_OID, getRoleOid(i), localTask, localTask.getResult());
                },
                getConcurrentTestNumberOfThreads(),
                getConcurrentTestRandomStartDelayRangeAssign());

        // THEN
        then();
        waitForThreads(threads, TIMEOUT);

        PrismObject<UserType> userAfter = getUser(USER_DRAKE_OID);
        display("user after", userAfter);
        assertAssignments(userAfter, getConcurrentTestNumberOfThreads());
        assertEquals("Wrong # of links", 1, userAfter.asObjectable().getLinkRef().size());
        accountDrakeOid = userAfter.asObjectable().getLinkRef().get(0).getOid();

        PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountDrakeOid, null, result);
        display("Repo shadow", shadowRepo);
        assertShadowNotDead(shadowRepo);

        assertTest910ShadowRepo(shadowRepo);

        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE));
        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class, accountDrakeOid, options, task, result);
        display("Shadow after (model, future)", shadowModel);

//        assertObjects(CaseType.class, numberOfCasesBefore + getConcurrentTestNumberOfThreads());
    }

    protected void assertTest910ShadowRepo(PrismObject<ShadowType> shadowRepo) throws Exception {
        assertShadowNotDead(shadowRepo);
        ObjectDeltaType addPendingDelta = null;
        for (PendingOperationType pendingOperation : shadowRepo.asObjectable().getPendingOperation()) {
            ObjectDeltaType delta = pendingOperation.getDelta();
            if (delta.getChangeType() == ChangeTypeType.ADD) {
                ObjectType objectToAdd = delta.getObjectToAdd();
                display("Pending ADD object", objectToAdd.asPrismObject());
                if (addPendingDelta != null) {
                    fail("More than one add pending delta found:\n" + addPendingDelta + "\n" + delta);
                }
                addPendingDelta = delta;
            }
            if (delta.getChangeType() == ChangeTypeType.DELETE) {
                fail("Unexpected delete pending delta found:\n" + delta);
            }
            if (isActivationStatusModifyDelta(delta, ActivationStatusType.ENABLED)) {
                fail("Unexpected enable pending delta found:\n" + delta);
            }
        }
        assertNotNull("No add pending delta", addPendingDelta);
    }

    // MID-4112
    @Test
    public void test919ConcurrentRoleUnassign() throws Exception {
        if (!are9xxTestsEnabled()) {
            displaySkip();
            return;
        }

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_DRAKE_OID);
        display("user before", userBefore);
        assertAssignments(userBefore, getConcurrentTestNumberOfThreads());

        //noinspection CheckStyle
        final long TIMEOUT = 60_000L;

        // WHEN
        when();
        String testName = getTestNameShort();
        ParallelTestThread[] threads = multithread(
                (i) -> {
                    display("Thread " + Thread.currentThread().getName() + " START");
                    login(userAdministrator);
                    Task localTask = createTask(testName + "-thread-" + i);
                    OperationResult localResult = localTask.getResult();

                    unassignRole(USER_DRAKE_OID, getRoleOid(i), localTask, localResult);

                    localResult.computeStatus();

                    display("Thread " + Thread.currentThread().getName() + " DONE, result", localResult);
                },
                getConcurrentTestNumberOfThreads(),
                getConcurrentTestRandomStartDelayRangeUnassign());

        // THEN
        then();
        waitForThreads(threads, TIMEOUT);

        PrismObject<UserType> userAfter = getUser(USER_DRAKE_OID);
        display("user after", userAfter);
        assertAssignments(userAfter, 0);
        assertEquals("Wrong # of links", 1, userAfter.asObjectable().getLinkRef().size());

        PrismObject<ShadowType> shadowRepo = repositoryService.getObject(ShadowType.class, accountDrakeOid, null, result);
        display("Repo shadow", shadowRepo);

        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class, accountDrakeOid, null, task, result);
        display("Shadow after (model)", shadowModel);

        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE));
        PrismObject<ShadowType> shadowModelFuture = modelService.getObject(ShadowType.class, accountDrakeOid, options, task, result);
        display("Shadow after (model, future)", shadowModelFuture);

        assertTest919ShadowRepo(shadowRepo, task, result);

        assertTest919ShadowFuture(shadowModelFuture, task, result);

//        assertObjects(CaseType.class, numberOfCasesBefore + getConcurrentTestNumberOfThreads() + 1);
    }

    protected void assertTest919ShadowRepo(PrismObject<ShadowType> shadowRepo, Task task, OperationResult result) throws Exception {
        ObjectDeltaType deletePendingDelta = null;
        for (PendingOperationType pendingOperation : shadowRepo.asObjectable().getPendingOperation()) {
            ObjectDeltaType delta = pendingOperation.getDelta();
            if (delta.getChangeType() == ChangeTypeType.ADD) {
                ObjectType objectToAdd = delta.getObjectToAdd();
                display("Pending ADD object", objectToAdd.asPrismObject());
            }
            if (delta.getChangeType() == ChangeTypeType.DELETE) {
                if (deletePendingDelta != null) {
                    fail("More than one delete pending delta found:\n" + deletePendingDelta + "\n" + delta);
                }
                deletePendingDelta = delta;
            }
        }
        assertNotNull("No delete pending delta", deletePendingDelta);
    }

    protected boolean isActivationStatusModifyDelta(ObjectDeltaType delta, ActivationStatusType expected) throws SchemaException {
        if (delta.getChangeType() != ChangeTypeType.MODIFY) {
            return false;
        }
        for (ItemDeltaType itemDelta : delta.getItemDelta()) {
            ItemPath deltaPath = itemDelta.getPath().getItemPath();
            if (SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS.equivalent(deltaPath)) {
                List<RawType> value = itemDelta.getValue();
                PrismProperty<ActivationStatusType> parsedItem = (PrismProperty<ActivationStatusType>) (Item)
                        value.get(0).getParsedItem(getUserDefinition().findPropertyDefinition(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS));
                ActivationStatusType status = parsedItem.getRealValue();
                displayValue("Delta status " + status, itemDelta);
                if (expected.equals(status)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected void assertTest919ShadowFuture(PrismObject<ShadowType> shadowModelFuture, Task task,
            OperationResult result) {
        assertShadowDead(shadowModelFuture);
    }

    protected void assertAccountWillAfterFullNameModification(
            PendingOperationExecutionStatusType executionStage) throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        var repoShadow = getShadowRepo(accountWillOid);
        var repoShadowObj = RepoShadowAsserter.forRepoShadow(repoShadow, getCachedAttributes())
                .display()
                .assertCachedOrigValues(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .assertCachedOrigValues(ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME)
                .assertNoPasswordIf(!isCaching())
                .getObject();

        var pendingOperation = assertSinglePendingOperation(
                repoShadowObj, accountWillReqestTimestampStart, accountWillReqestTimestampEnd, executionStage);
        assertNotNull("No ID in pending operation", pendingOperation.getId());

        // Still old data in the repo. The operation is not completed yet.
        assertShadowActivationAdministrativeStatusFromCache(repoShadowObj, ActivationStatusType.ENABLED);

        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
                accountWillOid, null, task, result);

        display("Model shadow", shadowModel);
        ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
        assertShadowName(shadowModel, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertShadowActivationAdministrativeStatus(shadowModel, ActivationStatusType.ENABLED);
        assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        assertAttribute(shadowModel, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME);
        assertAttributeFromBackingStore(shadowModel, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowModel);

        PendingOperationType pendingOperationType = assertSinglePendingOperation(
                shadowModel, accountWillReqestTimestampStart, accountWillReqestTimestampEnd, executionStage);

        PrismObject<ShadowType> shadowProvisioningFuture = modelService.getObject(ShadowType.class,
                accountWillOid,
                SelectorOptions.createCollection(GetOperationOptions.createPointInTimeType(PointInTimeType.FUTURE)),
                task, result);
        display("Model shadow (future)", shadowProvisioningFuture);
        assertShadowName(shadowProvisioningFuture, USER_WILL_NAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowProvisioningFuture.asObjectable().getKind());
        assertShadowActivationAdministrativeStatus(shadowProvisioningFuture, ActivationStatusType.ENABLED);
        assertAttribute(shadowProvisioningFuture, ATTR_USERNAME_QNAME, USER_WILL_NAME);
        assertAttribute(shadowProvisioningFuture, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME_PIRATE);
        assertAttributeFromBackingStore(shadowProvisioningFuture, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
        assertShadowPassword(shadowProvisioningFuture);

        assertWillCase(SchemaConstants.CASE_STATE_OPEN, pendingOperationType, executionStage);
    }

    protected int getNumberOfAccountAttributeDefinitions() {
        return 4;
    }

    protected void backingStoreProvisionWill(String interest) throws IOException {
        if (backingStore != null) {
            backingStore.provisionWill(interest);
        }
    }

    protected void backingStoreUpdateWill(String newFullName, String interest, ActivationStatusType newAdministrativeStatus, String password) throws IOException {
        if (backingStore != null) {
            backingStore.updateWill(newFullName, interest, newAdministrativeStatus, password);
        }
    }

    protected void backingStoreDeprovisionWill() throws IOException {
        if (backingStore != null) {
            backingStore.deprovisionWill();
        }
    }

    protected void displayBackingStore() throws IOException {
        if (backingStore != null) {
            backingStore.displayContent();
        }
    }

    protected void backingStoreAddJack() throws IOException {
        if (backingStore != null) {
            backingStore.addJack();
        }
    }

    protected void backingStoreDeleteJack() throws IOException {
        if (backingStore != null) {
            backingStore.deleteJack();
        }
    }

    protected void backingStoreAddPhantom() throws IOException {
        if (backingStore != null) {
            backingStore.addPhantom();
        }
    }

    protected void backingStoreAddPhoenix() throws IOException {
        if (backingStore != null) {
            backingStore.addPhoenix();
        }
    }

    protected void assignWillRoleOne(String expectedFullName, PendingOperationExecutionStatusType executionStage) throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        accountWillReqestTimestampStart = clock.currentTimeXMLGregorianCalendar();

        // WHEN
        when();
        assignRole(userWillOid, getRoleOneOid(), task, result);

        // THEN
        then();
        display("result", result);
        willLastCaseOid = assertInProgress(result);

        PrismObject<UserType> userAfter = getUser(userWillOid);
        display("User after", userAfter);
        accountWillOid = getSingleLinkOid(userAfter);

        accountWillReqestTimestampEnd = clock.currentTimeXMLGregorianCalendar();

        assertAccountWillAfterAssign(expectedFullName, executionStage);
    }

    protected void assertAccountWillAfterAssign(
            String expectedFullName, PendingOperationExecutionStatusType propagationExecutionStage)
            throws Exception {
        ShadowAsserter<Void> shadowRepoAsserter = assertRepoShadow(accountWillOid)
                .assertConception()
                .pendingOperations()
                .singleOperation()
                .assertId()
                .assertRequestTimestamp(accountWillReqestTimestampStart, accountWillReqestTimestampEnd)
                .assertExecutionStatus(getExpectedExecutionStatus(propagationExecutionStage))
                .delta()
                .display()
                .end()
                .end()
                .end()
                .attributes()
                .assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .end()
                .assertNoPasswordIf(!isCaching());
        assertAttributeFromCache(shadowRepoAsserter, ATTR_FULLNAME_QNAME, expectedFullName);
        assertShadowActivationAdministrativeStatusFromCache(shadowRepoAsserter.getObject(), ActivationStatusType.ENABLED);

        ShadowAsserter<Void> shadowModelAsserter = assertModelShadow(accountWillOid)
                .assertName(USER_WILL_NAME)
                .assertKind(ShadowKindType.ACCOUNT)
                .assertConception()
                .attributes()
                .assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .end()
                .assertNoPasswordIf(!isCaching())
                .pendingOperations()
                .singleOperation()
                .assertRequestTimestamp(accountWillReqestTimestampStart, accountWillReqestTimestampEnd)
                .assertExecutionStatus(getExpectedExecutionStatus(propagationExecutionStage))
                .end()
                .end();
        assertAttributeFromCache(shadowModelAsserter, ATTR_FULLNAME_QNAME, expectedFullName);
        assertShadowActivationAdministrativeStatusFromCache(shadowModelAsserter.getObject(), ActivationStatusType.ENABLED);

        CaseType aCase = assertWillCase(SchemaConstants.CASE_STATE_OPEN,
                shadowModelAsserter.pendingOperations().singleOperation().getOperation(),
                propagationExecutionStage);
        if (aCase != null) {
            assertThat(aCase.getTargetRef()).as("case targetRef").isNotNull();
        }
    }

    protected CaseType assertWillCase(String expectedCaseState, PendingOperationType pendingOperation,
            PendingOperationExecutionStatusType propagationExecutionStage) throws ObjectNotFoundException, SchemaException {
        String pendingOperationRef = pendingOperation.getAsynchronousOperationReference();
        CaseType aCase;
        if (isDirect()) {
            // Case number should be in willLastCaseOid. It will get there from operation result.
            assertNotNull("No async reference in pending operation", willLastCaseOid);
            aCase = assertCaseState(willLastCaseOid, expectedCaseState);
            assertEquals("Wrong case ID in pending operation", willLastCaseOid, pendingOperationRef);
        } else {
            if (caseShouldExist(propagationExecutionStage)) {
                assertNotNull("No async reference in pending operation", pendingOperationRef);
                aCase = assertCaseState(pendingOperationRef, expectedCaseState);
            } else {
                aCase = null;
            }
            willLastCaseOid = pendingOperationRef;
        }
        return aCase;
    }

    protected boolean caseShouldExist(PendingOperationExecutionStatusType executionStage) {
        return getExpectedExecutionStatus(executionStage) == PendingOperationExecutionStatusType.EXECUTING;
    }

    protected abstract boolean isDirect();

    protected abstract OperationResultStatusType getExpectedResultStatus(PendingOperationExecutionStatusType executionStage);

    protected abstract PendingOperationExecutionStatusType getExpectedExecutionStatus(PendingOperationExecutionStatusType executionStage);

    protected void assertAccountJackAfterAssign() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        var repoShadow = getShadowRepo(accountJackOid);
        PrismObject<ShadowType> repoShadowObj = repoShadow.getPrismObject();
        display("Repo shadow", repoShadowObj);
        PendingOperationType pendingOperation = assertSinglePendingOperation(repoShadowObj, accountJackReqestTimestampStart, accountJackReqestTimestampEnd);
        assertNotNull("No ID in pending operation", pendingOperation.getId());
        RepoShadowAsserter.forRepoShadow(repoShadow, getCachedAttributes())
                .assertCachedOrigValues(ATTR_USERNAME_QNAME, USER_JACK_USERNAME)
                .assertCachedOrigValues(ATTR_FULLNAME_QNAME, USER_JACK_FULL_NAME);
        assertShadowActivationAdministrativeStatusFromCache(repoShadowObj, ActivationStatusType.ENABLED);
        assertShadowExists(repoShadowObj, false);
        if (!isCaching()) {
            assertNoShadowPassword(repoShadowObj);
        }

        PrismObject<ShadowType> shadowModel = modelService.getObject(ShadowType.class,
                accountJackOid, null, task, result);

        display("Model shadow", shadowModel);
        ShadowType shadowTypeProvisioning = shadowModel.asObjectable();
        assertShadowName(shadowModel, USER_JACK_USERNAME);
        assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, shadowTypeProvisioning.getKind());
        assertAttribute(shadowModel, ATTR_USERNAME_QNAME, USER_JACK_USERNAME);
        assertAttributeFromCache(shadowModel, ATTR_FULLNAME_QNAME, USER_JACK_FULL_NAME);
        assertShadowActivationAdministrativeStatusFromCache(shadowModel, ActivationStatusType.ENABLED);
        assertShadowExists(shadowModel, false);
        if (!isCaching()) {
            assertNoShadowPassword(shadowModel);
        }

        assertSinglePendingOperation(shadowModel, accountJackReqestTimestampStart, accountJackReqestTimestampEnd);

        assertNotNull("No async reference in result", jackLastCaseOid);

        assertCaseState(jackLastCaseOid, SchemaConstants.CASE_STATE_OPEN);
    }

    protected void assertWillAfterCreateCaseClosed(boolean backingStoreUpdated) throws Exception {
        ShadowAsserter<Void> shadowRepoAsserter = assertRepoShadow(accountWillOid)
                .pendingOperations()
                .singleOperation()
                .assertRequestTimestamp(accountWillReqestTimestampStart, accountWillReqestTimestampEnd)
                .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                .assertResultStatus(OperationResultStatusType.SUCCESS)
                .assertCompletionTimestamp(accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd)
                .end()
                .end()
                .attributes()
                .assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .end();
        assertAttributeFromCache(shadowRepoAsserter, ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME);
        assertShadowActivationAdministrativeStatusFromCache(shadowRepoAsserter, ActivationStatusType.ENABLED);

        ShadowAsserter<Void> shadowModelAsserter = assertModelShadow(accountWillOid)
                .assertName(USER_WILL_NAME)
                .assertKind(ShadowKindType.ACCOUNT)
                .attributes()
                .assertValue(ATTR_USERNAME_QNAME, USER_WILL_NAME)
                .end();
        if (!supportsBackingStore() || backingStoreUpdated) {
            shadowRepoAsserter
                    .assertLive();

            shadowModelAsserter
                    .assertLive()
                    .assertAdministrativeStatus(ActivationStatusType.ENABLED)
                    .attributes()
                    .assertValue(ATTR_FULLNAME_QNAME, USER_WILL_FULL_NAME)
                    .end()
                    .pendingOperations()
                    .singleOperation()
                    .assertRequestTimestamp(accountWillReqestTimestampStart, accountWillReqestTimestampEnd)
                    .assertExecutionStatus(PendingOperationExecutionStatusType.COMPLETED)
                    .assertResultStatus(OperationResultStatusType.SUCCESS)
                    .assertCompletionTimestamp(accountWillCompletionTimestampStart, accountWillCompletionTimestampEnd)
                    .end()
                    .end();
            assertAttributeFromBackingStore(shadowModelAsserter, ATTR_DESCRIPTION_QNAME, ACCOUNT_WILL_DESCRIPTION_MANUAL);
            assertShadowPassword(shadowModelAsserter);

        } else {

            shadowRepoAsserter
                    .assertGestation();

            shadowModelAsserter
                    .assertGestation();
        }

        assertCaseState(willLastCaseOid, SchemaConstants.CASE_STATE_CLOSED);
    }

    protected void assertWillUnassignedFuture(ShadowAsserter<?> shadowModelAsserterFuture, boolean assertPassword) {
        shadowModelAsserterFuture
                .assertName(USER_WILL_NAME);
        assertUnassignedFuture(shadowModelAsserterFuture, assertPassword);
    }

    protected void assertUnassignedFuture(ShadowAsserter<?> shadowModelAsserterFuture, boolean assertPassword) {
        shadowModelAsserterFuture
                .assertDead();
        if (assertPassword) {
            assertShadowPassword(shadowModelAsserterFuture);
        }
    }

    protected void assertNoAttribute(PrismObject<ShadowType> shadow, QName attrName) {
        assertNoAttribute(shadow.asObjectable(), attrName);
    }

    protected void assertAttributeFromCache(ShadowAsserter<?> shadowAsserter, QName attrQName,
            Object... attrVals) {
        assertAttributeFromCache(shadowAsserter.getObject(), attrQName, attrVals);
    }

    protected void assertAttributeFromCache(PrismObject<ShadowType> shadow, QName attrQName,
            Object... attrVals) {
        if (supportsBackingStore()) {
            assertNoAttribute(shadow, attrQName);
        } else {
            assertAttribute(shadow, attrQName, attrVals);
        }
    }

    protected void assertAttributeFromBackingStore(ShadowAsserter<?> shadowAsserter, QName attrQName,
            String... attrVals) {
        assertAttributeFromBackingStore(shadowAsserter.getObject(), attrQName, attrVals);
    }

    protected void assertAttributeFromBackingStore(PrismObject<ShadowType> shadow, QName attrQName,
            String... attrVals) {
        if (supportsBackingStore()) {
            assertAttribute(shadow, attrQName, attrVals);
        } else {
            assertNoAttribute(shadow, attrQName);
        }
    }

    protected void assertShadowActivationAdministrativeStatusFromCache(ShadowAsserter<?> shadowAsserter, ActivationStatusType expectedStatus) {
        assertShadowActivationAdministrativeStatusFromCache(shadowAsserter.getObject(), expectedStatus);
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

    protected void assertShadowPassword(ShadowAsserter<?> shadowAsserter) {
        assertShadowPassword(shadowAsserter.getObject());
    }

    protected void assertShadowPassword(PrismObject<ShadowType> shadow) {
        // pure manual resource should never "read" password, except for caching
        if (!isCaching()) {
            assertNoShadowPassword(shadow);
        }
    }

    private void assertManual(AbstractWriteCapabilityType cap) {
        assertEquals("Manual flag not set in capability " + cap, Boolean.TRUE, cap.isManual());
    }

    protected void cleanupUser(String userOid, String username, String accountOid) throws Exception {
        // nothing to do here
    }

    @UnusedTestElement
    protected void assertCase(
            String oid, String expectedState, PendingOperationExecutionStatusType executionStage)
            throws ObjectNotFoundException, SchemaException {
        if (caseShouldExist(executionStage)) {
            assertCaseState(oid, expectedState);
        }
    }

    protected void runPropagation() throws Exception {
        runPropagation(null);
    }

    protected void runPropagation(OperationResultStatusType expectedStatus) throws Exception {
        // nothing by default
    }

    @Override
    protected void rememberSteadyResources() {
        super.rememberSteadyResources();
        OperationResult result = createOperationResult("rememberSteadyResources");
        try {
            lastResourceVersion = repositoryService.getVersion(ResourceType.class, getResourceOid(), result);
        } catch (ObjectNotFoundException | SchemaException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    protected void assertSteadyResources() {
        super.assertSteadyResources();
        try {
            OperationResult result = createOperationResult("assertSteadyResources");
            String currentResourceVersion =
                    repositoryService.getVersion(ResourceType.class, getResourceOid(), result);
            assertEquals("Resource version mismatch", lastResourceVersion, currentResourceVersion);
        } catch (ObjectNotFoundException | SchemaException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @UnusedTestElement
    protected void assertHasModification(ObjectDeltaType deltaType, ItemPath itemPath) {
        for (ItemDeltaType itemDelta : deltaType.getItemDelta()) {
            if (itemPath.equivalent(itemDelta.getPath().getItemPath())) {
                return;
            }
        }
        fail("No modification for " + itemPath + " in delta");
    }

    protected PendingOperationType assertSinglePendingOperation(PrismObject<ShadowType> shadow,
            XMLGregorianCalendar requestStart, XMLGregorianCalendar requestEnd, PendingOperationExecutionStatusType executionStage) {
        assertPendingOperationDeltas(shadow, 1);
        return assertPendingOperation(shadow, shadow.asObjectable().getPendingOperation().get(0),
                requestStart, requestEnd,
                getExpectedExecutionStatus(executionStage), getExpectedResultStatus(executionStage),
                null, null);
    }

    protected boolean isCaching() {
        return InternalsConfig.isShadowCachingOnByDefault();
    }
}
