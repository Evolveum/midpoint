/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.async.update;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.provisioning.impl.ProvisioningTestUtil.checkRepoAccountShadow;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeoutException;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.processor.*;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.impl.AbstractProvisioningIntegrationTest;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.ShadowAsserter;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author semancik
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public abstract class TestAsyncUpdate extends AbstractProvisioningIntegrationTest {

    protected static final File TEST_DIR = new File("src/test/resources/async/update/");

    private static final String RESOURCE_ASYNC_OID = "fb04d113-ebf8-41b4-b13b-990a597d110b";

    private static final File CHANGE_100 = new File(TEST_DIR, "change-100-banderson-first-occurrence.xml");
    private static final File CHANGE_110 = new File(TEST_DIR, "change-110-banderson-delta-add-values.xml");
    private static final File CHANGE_112 = new File(TEST_DIR, "change-112-banderson-delta-add-more-values.xml");
    private static final File CHANGE_115 = new File(TEST_DIR, "change-115-banderson-delta-delete-values.xml");
    private static final File CHANGE_117 = new File(TEST_DIR, "change-117-banderson-delta-replace-values.xml");
    private static final File CHANGE_120 = new File(TEST_DIR, "change-120-banderson-new-state.xml");
    private static final File CHANGE_125 = new File(TEST_DIR, "change-125-banderson-notification-only.xml");
    private static final File CHANGE_130 = new File(TEST_DIR, "change-130-banderson-delete.xml");

    private static final QName RESOURCE_ACCOUNT_OBJECTCLASS = RI_ACCOUNT_OBJECT_CLASS;

    static final String ASYNC_UPDATE_CONNECTOR = "AsyncUpdateConnector";

    private static final String ATTR_TEST = "test";
    private static final String ATTR_MEMBER_OF = "memberOf";

    protected PrismObject<ResourceType> resource;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        syncServiceMock.setSupportActivation(false);
        resource = addResourceFromFile(getResourceFile(), getConnectorTypes(), false, initResult);

        InternalsConfig.setSanityChecks(true);
    }

    @NotNull
    public abstract List<String> getConnectorTypes();

    protected abstract File getResourceFile();

    @Test
    public void test000Sanity() throws Exception {
        OperationResult result = getTestOperationResult();
        assertNotNull("Resource is null", resource);

        ResourceType repoResource = repositoryService.getObject(ResourceType.class, RESOURCE_ASYNC_OID, null, result).asObjectable();
        assertNotNull("No connector ref", repoResource.getConnectorRef());
        String connectorOid = repoResource.getConnectorRef().getOid();
        assertNotNull("No connector ref OID", connectorOid);
        ConnectorType repoConnector = repositoryService
                .getObject(ConnectorType.class, connectorOid, null, result).asObjectable();
        assertNotNull(repoConnector);
        display("Async Connector", repoConnector);

        // Check connector schema
        IntegrationTestTools.assertConnectorSchemaSanity(repoConnector);
    }

    @Test
    public void test003Connection() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // Check that there is a schema, but no capabilities before test (pre-condition)
        ResourceType resourceBefore = repositoryService.getObject(
                ResourceType.class, RESOURCE_ASYNC_OID, null, result).asObjectable();

        ResourceTypeUtil.getResourceXsdSchemaElement(resourceBefore);

        CapabilitiesType capabilities = resourceBefore.getCapabilities();
        if (capabilities != null) {
            AssertJUnit.assertNull("Native capabilities present before test connection. Bad test setup?", capabilities.getNative());
        }

        // WHEN
        OperationResult testResult = provisioningService.testResource(RESOURCE_ASYNC_OID, task, result);

        // THEN
        display("Test result", testResult);
        TestUtil.assertSuccess("Test resource failed (result)", testResult);

        PrismObject<ResourceType> resourceRepoAfter = repositoryService.getObject(ResourceType.class, RESOURCE_ASYNC_OID, null, result);
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

    @Test
    public void test004Configuration() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        // WHEN
        resource = provisioningService.getObject(ResourceType.class, RESOURCE_ASYNC_OID, null, task, result);

        PrismContainer<Containerable> configurationContainer = resource.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
        assertNotNull("No configuration container", configurationContainer);
        PrismContainerDefinition<?> confContDef = configurationContainer.getDefinition();
        assertNotNull("No configuration container definition", confContDef);
    }

    @Test
    public void test005ParsedSchema() throws Exception {
        // GIVEN

        // THEN
        // The returned type should have the schema pre-parsed
        assertTrue(ResourceSchemaFactory.hasParsedSchema(resource.asObjectable()));

        ResourceSchema resourceSchema = ResourceSchemaFactory.getCompleteSchemaRequired(resource.asObjectable());

        displayDumpable("Parsed resource schema", resourceSchema);

        ResourceObjectClassDefinition accountDef = resourceSchema.findObjectClassDefinition(RESOURCE_ACCOUNT_OBJECTCLASS);
        assertNotNull("Account definition is missing", accountDef);
        assertNotNull("Null identifiers in account", accountDef.getPrimaryIdentifiers());
        assertFalse("Empty identifiers in account", accountDef.getPrimaryIdentifiers().isEmpty());
        assertNotNull("No naming attribute in account", accountDef.getNamingAttribute());

        assertEquals("Unexpected number of definitions", 4, accountDef.getDefinitions().size());
    }

    @Test
    public void test100ListeningForShadowAdd() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        // GIVEN

        prepareMessage(CHANGE_100);

        syncServiceMock.reset();

        addDummyAccount("banderson");

        mockAsyncUpdateTaskHandler.processUpdates(
                ResourceOperationCoordinates.ofResource(RESOURCE_ASYNC_OID), task, result);

        ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
        assertNotNull("No last change", lastChange);
        displayDumpable("The change", lastChange);

        assertNotNull("Delta is missing", lastChange.getObjectDelta());
        assertNotNull("Current shadow is not present", lastChange.getShadowedResourceObject());

        var repoShadow = findAccountShadowByUsername("banderson", resource, result);
        assertNotNull("Shadow was not created in the repository", repoShadow);
        display("Repository shadow", repoShadow);
        checkRepoAccountShadow(repoShadow);
        assertNoUnacknowledgedMessages();
    }

    @Test
    public void test110ListeningForValueAdd() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        prepareMessage(CHANGE_110);

        syncServiceMock.reset();

        setDummyAccountTestAttribute("banderson", "value1", "value2", "value3");

        mockAsyncUpdateTaskHandler.processUpdates(
                ResourceOperationCoordinates.ofResource(RESOURCE_ASYNC_OID), task, result);

        ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
        displayDumpable("The change", lastChange);

        assertNotNull("Delta is missing", lastChange.getObjectDelta());
        assertTrue("Delta is not a MODIFY one", lastChange.getObjectDelta().isModify());
        Collection<? extends ItemDelta<?, ?>> modifications = lastChange.getObjectDelta().getModifications();
        assertEquals("Wrong # of modifications", 2, modifications.size());
        Iterator<? extends ItemDelta<?, ?>> iterator = modifications.iterator();
        assertEquals("Wrong # of values added (first mod)", 3, iterator.next().getValuesToAdd().size());
        assertEquals("Wrong # of values added (second mod)", 6, iterator.next().getValuesToAdd().size());
        assertNotNull("Current shadow is not present", lastChange.getShadowedResourceObject());

        ShadowAsserter<Void> asserter = getAndersonFull(false, task, result);
        if (isCached()) {
            asserter.attributes()
                    .simpleAttribute(ATTR_TEST).assertRealValues("value1", "value2", "value3").end()
                    .simpleAttribute(ATTR_MEMBER_OF).assertRealValues("group1", "group2", "group3", "group4", "group5", "group6").end();
        }
        assertNoUnacknowledgedMessages();
    }

    @Test
    public void test112ListeningForValueAddMore() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        prepareMessage(CHANGE_112);

        syncServiceMock.reset();

        setDummyAccountTestAttribute("banderson", "value1", "value2", "value3", "value4");

        mockAsyncUpdateTaskHandler.processUpdates(
                ResourceOperationCoordinates.ofResource(RESOURCE_ASYNC_OID), task, result);

        ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
        displayDumpable("The change", lastChange);

        assertNotNull("Delta is missing", lastChange.getObjectDelta());
        assertTrue("Delta is not a MODIFY one", lastChange.getObjectDelta().isModify());
        Collection<? extends ItemDelta<?, ?>> modifications = lastChange.getObjectDelta().getModifications();
        assertEquals("Wrong # of modifications", 2, modifications.size());
        Iterator<? extends ItemDelta<?, ?>> iterator = modifications.iterator();
        assertEquals("Wrong # of values added (first mod)", 1, iterator.next().getValuesToAdd().size());
        assertEquals("Wrong # of values added (second mod)", 1, iterator.next().getValuesToAdd().size());
        assertNotNull("Current shadow is not present", lastChange.getShadowedResourceObject());

        ShadowAsserter<Void> asserter = getAndersonFull(false, task, result);
        if (isCached()) {
            asserter.attributes()
                    .simpleAttribute(ATTR_TEST).assertRealValues("value1", "value2", "value3", "value4").end()
                    .simpleAttribute(ATTR_MEMBER_OF).assertRealValues("group1", "group2", "group3", "group4", "group5", "group6", "group7").end();
        }
        assertNoUnacknowledgedMessages();
    }

    @Test // MID-5832
    public void test115ListeningForValueDelete() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        prepareMessage(CHANGE_115);

        syncServiceMock.reset();

        setDummyAccountTestAttribute("banderson", "value1", "value3", "value4");

        mockAsyncUpdateTaskHandler.processUpdates(
                ResourceOperationCoordinates.ofResource(RESOURCE_ASYNC_OID), task, result);

        ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
        displayDumpable("The change", lastChange);

        assertNotNull("Delta is missing", lastChange.getObjectDelta());
        assertTrue("Delta is not a MODIFY one", lastChange.getObjectDelta().isModify());
        Collection<? extends ItemDelta<?, ?>> modifications = lastChange.getObjectDelta().getModifications();
        assertEquals("Wrong # of modifications", 2, modifications.size());
        Iterator<? extends ItemDelta<?, ?>> iterator = modifications.iterator();
        assertEquals("Wrong # of values deleted (first mod)", 1, iterator.next().getValuesToDelete().size());
        assertEquals("Wrong # of values deleted (second mod)", 2, iterator.next().getValuesToDelete().size());
        assertNotNull("Current shadow is not present", lastChange.getShadowedResourceObject());

        ShadowAsserter<Void> asserter = getAndersonFull(false, task, result);
        if (isCached()) {
            asserter.attributes()
                    .simpleAttribute(ATTR_TEST).assertRealValues("value1", "value3", "value4").end()
                    .simpleAttribute(ATTR_MEMBER_OF).assertRealValues("group1", "group4", "group5", "group6", "group7").end();
        }
        assertNoUnacknowledgedMessages();
    }

    @Test // MID-5832
    public void test117ListeningForValueReplace() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        prepareMessage(CHANGE_117);

        syncServiceMock.reset();

        setDummyAccountTestAttribute("banderson", "value100");

        mockAsyncUpdateTaskHandler.processUpdates(
                ResourceOperationCoordinates.ofResource(RESOURCE_ASYNC_OID), task, result);

        ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
        displayDumpable("The change", lastChange);

        assertNotNull("Delta is missing", lastChange.getObjectDelta());
        assertTrue("Delta is not a MODIFY one", lastChange.getObjectDelta().isModify());
        Collection<? extends ItemDelta<?, ?>> modifications = lastChange.getObjectDelta().getModifications();
        assertEquals("Wrong # of modifications", 2, modifications.size());
        Iterator<? extends ItemDelta<?, ?>> iterator = modifications.iterator();
        assertEquals("Wrong # of values replaced (first mod)", 1, iterator.next().getValuesToReplace().size());
        assertEquals("Wrong # of values replaced (second mod)", 2, iterator.next().getValuesToReplace().size());
        assertNotNull("Current shadow is not present", lastChange.getShadowedResourceObject());

        ShadowAsserter<Void> asserter = getAndersonFull(false, task, result);
        if (isCached()) {
            asserter.attributes()
                    .simpleAttribute(ATTR_TEST).assertRealValues("value100").end()
                    .simpleAttribute(ATTR_MEMBER_OF).assertRealValues("group100", "group101").end();
        }
        assertNoUnacknowledgedMessages();
    }

    @Test
    public void test120ListeningForShadowReplace() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        prepareMessage(CHANGE_120);

        syncServiceMock.reset();

        setDummyAccountTestAttribute("banderson", "value4");

        mockAsyncUpdateTaskHandler.processUpdates(
                ResourceOperationCoordinates.ofResource(RESOURCE_ASYNC_OID), task, result);

        ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
        displayDumpable("The change", lastChange);

        assertNull("Delta is present although it should not be", lastChange.getObjectDelta());
        assertNotNull("Current shadow is missing", lastChange.getShadowedResourceObject());

        getAndersonFull(false, task, result);
        assertNoUnacknowledgedMessages();
    }

    @Test
    public void test125ListeningForNotificationOnly() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        if (!hasReadCapability()) {
            System.out.println("Skipping this test because there's no real read capability");
            return;
        }

        prepareMessage(CHANGE_125);

        syncServiceMock.reset();

        setDummyAccountTestAttribute("banderson", "value125");

        mockAsyncUpdateTaskHandler.processUpdates(
                ResourceOperationCoordinates.ofResource(RESOURCE_ASYNC_OID), task, result);

        ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
        displayDumpable("The change", lastChange);

        assertNull("Delta is present although it should not be", lastChange.getObjectDelta());
        assertNotNull("Current shadow is missing", lastChange.getShadowedResourceObject());

        display("change current shadow", lastChange.getShadowedResourceObject());

        getAndersonFull(false, task, result);
        assertNoUnacknowledgedMessages();
    }

    @Test
    public void test130ListeningForShadowDelete() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        prepareMessage(CHANGE_130);

        syncServiceMock.reset();

        mockAsyncUpdateTaskHandler.processUpdates(
                ResourceOperationCoordinates.ofResource(RESOURCE_ASYNC_OID), task, result);

        ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
        displayDumpable("The change", lastChange);

        assertNotNull("Delta is missing", lastChange.getObjectDelta());
        assertTrue("Delta is not a DELETE one", lastChange.getObjectDelta().isDelete());
        //assertNull("Current shadow is present while not expecting it", lastChange.getCurrentShadow());
        //current shadow was added during the processing

        getAndersonFull(true, task, result);
        assertNoUnacknowledgedMessages();
    }

    @Test
    public void test140ProcessingEmptyMessage() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        prepareMessage(null);

        syncServiceMock.reset();

        mockAsyncUpdateTaskHandler.processUpdates(
                ResourceOperationCoordinates.ofResource(RESOURCE_ASYNC_OID), task, result);

        ResourceObjectShadowChangeDescription lastChange = syncServiceMock.getLastChange();
        assertThat(lastChange).as("last change").isNull();

        assertNoUnacknowledgedMessages();
    }

    @SuppressWarnings("SameParameterValue")
    void addDummyAccount(String name) {
    }

    @SuppressWarnings("SameParameterValue")
    void setDummyAccountTestAttribute(String name, String... values) {
    }

    private int getNumberOfAccountAttributes() {
        if (isCached()) {
            return 4;
        } else if (InternalsConfig.isShadowCachingFullByDefault()) {
            return 3; // for some reason, "test" is provided, but "memberOf" is not; TODO does not work universally
        } else {
            return 2;
        }
    }

    abstract boolean isCached();

    boolean hasReadCapability() {
        return false;
    }

    void prepareMessage(File messageFile)
            throws java.io.IOException, com.evolveum.midpoint.util.exception.SchemaException, TimeoutException {
        MockAsyncUpdateSource.INSTANCE.reset();
        if (messageFile != null) {
            MockAsyncUpdateSource.INSTANCE.prepareMessage(prismContext.parserFor(messageFile).parseRealValue());
        } else {
            MockAsyncUpdateSource.INSTANCE.prepareMessage(null);
        }
    }

    void assertNoUnacknowledgedMessages() {
        assertThat(MockAsyncUpdateSource.INSTANCE.getUnacknowledgedMessagesCount())
                .as("unacknowledged messages count")
                .isEqualTo(0);
    }

    @Contract("false,_,_ -> !null")
    private ShadowAsserter<Void> getAndersonFull(boolean dead, Task task, OperationResult result)
            throws SchemaException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        var repoShadow = findAccountShadowByUsername("banderson", resource, result);
        assertNotNull("No Anderson shadow in repo", repoShadow);
        Collection<SelectorOptions<GetOperationOptions>> options = schemaService.getOperationOptionsBuilder()
                .noFetch()
                .retrieve()
                .build();
        try {
            var shadow = provisioningService.getObject(ShadowType.class, repoShadow.getOid(), options, task, result);
            if (dead) {
                fail("Shadow should be gone now but it is not: " + shadow.debugDump());
            }
            return assertShadow(shadow, "after")
                    .assertKind(ShadowKindType.ACCOUNT)
                    .attributes()
                    .assertSize(getNumberOfAccountAttributes())
                    .primaryIdentifier()
                    .assertRealValues("banderson")
                    .end()
                    .secondaryIdentifier()
                    .assertRealValues("banderson")
                    .end()
                    .end();
        } catch (ObjectNotFoundException e) {
            if (!dead) {
                e.printStackTrace();
                fail("Shadow is gone but it should not be");
            }
            return null;
        }
    }
}
