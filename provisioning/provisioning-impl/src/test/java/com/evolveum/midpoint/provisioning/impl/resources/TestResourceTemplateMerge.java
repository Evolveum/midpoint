/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resources;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.SchemaConstantsGenerated.ICF_C_CONFIGURATION_PROPERTIES;
import static com.evolveum.midpoint.schema.SchemaConstantsGenerated.ICF_C_RESULTS_HANDLER_CONFIGURATION;
import static com.evolveum.midpoint.schema.constants.MidPointConstants.NS_RI;
import static com.evolveum.midpoint.schema.processor.ResourceSchemaTestUtil.findObjectTypeDefinitionRequired;
import static com.evolveum.midpoint.test.IntegrationTestTools.DUMMY_CONNECTOR_TYPE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.InboundMappingEvaluationPhaseType.BEFORE_CORRELATION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.InboundMappingEvaluationPhaseType.CLOCKWORK;
import static com.evolveum.midpoint.xml.ns._public.connector.icf_1.connector_schema_3.ResultsHandlerConfigurationType.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.provisioning.impl.AbstractProvisioningIntegrationTest;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

public class TestResourceTemplateMerge extends AbstractProvisioningIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/merge");

    private static final TestResource<ResourceType> RESOURCE_BASIC_TEMPLATE = new TestResource<>(
            TEST_DIR, "resource-basic-template.xml", "2d1bbd38-8292-4895-af07-15de1ae423ec");
    private static final TestResource<ResourceType> RESOURCE_BASIC_1 = new TestResource<>(
            TEST_DIR, "resource-basic-1.xml", "b6f77fb9-8bdf-42de-b7d4-639c77fa6805");
    private static final TestResource<ResourceType> RESOURCE_BASIC_2 = new TestResource<>(
            TEST_DIR, "resource-basic-2.xml", "969d0587-b049-4067-a749-2fe61d5fb2f6");

    private static final TestResource<ResourceType> RESOURCE_ADDITIONAL_CONNECTORS_TEMPLATE = new TestResource<>(
            TEST_DIR, "resource-additional-connectors-template.xml", "e17dfe38-727f-41b6-ab1c-9106c0bb046d");
    private static final TestResource<ResourceType> RESOURCE_ADDITIONAL_CONNECTORS_1 = new TestResource<>(
            TEST_DIR, "resource-additional-connectors-1.xml", "dcf805dc-afff-46c1-bf8c-876777ef4af5");

    private static final TestResource<ResourceType> RESOURCE_OBJECT_TYPES_TEMPLATE = new TestResource<>(
            TEST_DIR, "resource-object-types-template.xml", "873a5483-ded8-4607-ac06-ea5ae92ce755");
    private static final TestResource<ResourceType> RESOURCE_OBJECT_TYPES_1_RAW = new TestResource<>(
            TEST_DIR, "resource-object-types-1.xml", "8e355713-c785-441c-88b4-79bdb041103e");

    // This is object-types-1 but for schema-related tests
    private static final DummyTestResource RESOURCE_OBJECT_TYPES_1 = new DummyTestResource(
            TEST_DIR, "resource-object-types-1.xml", RESOURCE_OBJECT_TYPES_1_RAW.oid,
            "object-types-1", DummyResourceContoller::extendSchemaPirate);

    private static final DummyTestResource RESOURCE_EXPLICIT_TYPE_INHERITANCE = new DummyTestResource(
            TEST_DIR, "resource-explicit-type-inheritance.xml", "eb2f35d9-8147-413c-bfe0-f0890c14e702",
            "explicit-type-inheritance", DummyResourceContoller::extendSchemaPirate);

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        addResourceObject(RESOURCE_BASIC_TEMPLATE, List.of(DUMMY_CONNECTOR_TYPE), initResult);
        repoAdd(RESOURCE_BASIC_1, initResult); // No connectorRef is here, so basic add is OK
        repoAdd(RESOURCE_BASIC_2, initResult); // No connectorRef is here, so basic add is OK

        addResourceObject(
                RESOURCE_ADDITIONAL_CONNECTORS_TEMPLATE,
                List.of(DUMMY_CONNECTOR_TYPE, DUMMY_CONNECTOR_TYPE, DUMMY_CONNECTOR_TYPE),
                initResult);
        addResourceObject(
                RESOURCE_ADDITIONAL_CONNECTORS_1,
                Arrays.asList(null, null, DUMMY_CONNECTOR_TYPE),
                initResult); // No connectorRef is here, so basic add is OK

        addResourceObject(RESOURCE_OBJECT_TYPES_TEMPLATE, List.of(DUMMY_CONNECTOR_TYPE), initResult);
        repoAdd(RESOURCE_OBJECT_TYPES_1_RAW, initResult); // No connectorRef is here, so basic add is OK
    }

    /** Adds a resource to repository, fills-in connector OID externally. */
    private void addResourceObject(TestResource<ResourceType> resource, List<String> connectorTypes, OperationResult result)
            throws CommonException, EncryptionException, IOException {
        addResourceFromFile(resource.file, connectorTypes, false, result);
        resource.reload(result);
    }

    /**
     * `basic1` inherits from `basic-template`.
     */
    @Test
    public void test100Basic1() throws CommonException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("basic1 is expanded");
        ResourceType expanded = RESOURCE_BASIC_1.getObjectable().clone();
        provisioningService.expandConfigurationObject(expanded.asPrismObject(), task, result);

        then("expanded version can be displayed");
        // @formatter:off
        assertResource(expanded, "after")
                .displayXml();

        and("origins are OK");
        assertNoOrigin(expanded, ResourceType.F_NAME);
        assertNoOrigin(expanded, ResourceType.F_SUPER);
        assertNoOrigin(expanded, ItemPath.create(ResourceType.F_SUPER, SuperResourceDeclarationType.F_RESOURCE_REF));
        assertOrigin(expanded, ResourceType.F_CONNECTOR_REF, RESOURCE_BASIC_TEMPLATE.oid);
        assertNoOrigin(expanded, ResourceType.F_CONNECTOR_CONFIGURATION);
        assertNoOrigin(expanded, ItemPath.create(ResourceType.F_CONNECTOR_CONFIGURATION, ICF_C_RESULTS_HANDLER_CONFIGURATION));
        assertNoOrigin(expanded,
                ItemPath.create(
                        ResourceType.F_CONNECTOR_CONFIGURATION,
                        ICF_C_RESULTS_HANDLER_CONFIGURATION,
                        "filteredResultsHandlerInValidationMode")); // This is newly-added value
        assertOrigin(expanded,
                ItemPath.create(
                        ResourceType.F_CONNECTOR_CONFIGURATION,
                        ICF_C_RESULTS_HANDLER_CONFIGURATION,
                        "enableNormalizingResultsHandler"),
                RESOURCE_BASIC_TEMPLATE.oid); // This is inherited (with 2 other ones)
        assertNoOrigin(expanded,
                ItemPath.create(
                        ResourceType.F_CONNECTOR_CONFIGURATION,
                        ICF_C_CONFIGURATION_PROPERTIES,
                        "supportValidity")); // This is newly-added value
        assertOrigin(expanded,
                ItemPath.create(
                        ResourceType.F_CONNECTOR_CONFIGURATION,
                        ICF_C_CONFIGURATION_PROPERTIES,
                        "uselessString"),
                RESOURCE_BASIC_TEMPLATE.oid); // This is inherited (with the other ones)
        assertNoOrigin(expanded,
                ItemPath.create(
                        ResourceType.F_SCHEMA,
                        XmlSchemaType.F_GENERATION_CONSTRAINTS,
                        SchemaGenerationConstraintsType.F_GENERATE_OBJECT_CLASS));

        and("expanded values are OK");
        assertResource(expanded, "after")
                .assertName("Basic 1")
                .assertNotAbstract()
                .configurationProperties()
                    .assertSize(4)
                    .assertAllItemsHaveCompleteDefinition()
                    .assertPropertyEquals(new ItemName("supportValidity"), false) // overridden
                    .assertPropertyEquals(new ItemName("instanceId"), "basic") // from template
                    .assertPropertyEquals(new ItemName("uselessString"), "Shiver me timbers!") // from template
                    .assertPropertyEquals(new ItemName("uselessGuardedString"),
                            new ProtectedStringType().clearValue("Dead men tell no tales")) // from template
                .end()
                .resultsHandlerConfiguration()
                    .assertSize(4)
                    .assertAllItemsHaveCompleteDefinition()
                    .assertPropertyEquals(F_FILTERED_RESULTS_HANDLER_IN_VALIDATION_MODE, true) // added by basic-1
                    .assertPropertyEquals(F_ENABLE_FILTERED_RESULTS_HANDLER, false) // from template
                    .assertPropertyEquals(F_ENABLE_ATTRIBUTES_TO_GET_SEARCH_RESULTS_HANDLER, false) // from template
                    .assertPropertyEquals(F_ENABLE_NORMALIZING_RESULTS_HANDLER, false) // from template
                .end()
                .assertConnectorRefIgnoringMetadata(RESOURCE_BASIC_TEMPLATE.getObjectable().getConnectorRef())
                .assertGeneratedClasses(new QName("A"), new QName("B"))
                .assertConfiguredCapabilities(3) // 1 overridden, 1 inherited, 1 new
                .configuredCapability(CountObjectsCapabilityType.class)
                    // overridden in types-1
                    .assertPropertyEquals(CountObjectsCapabilityType.F_SIMULATE, CountObjectsSimulateType.SEQUENTIAL_SEARCH)
                .end()
                .configuredCapability(ReadCapabilityType.class) // new in types-1
                    .assertPropertyEquals(ReadCapabilityType.F_ENABLED, true)
                .end()
                .configuredCapability(CreateCapabilityType.class) // from the template
                .end();
        // @formatter:on
    }

    private void assertNoOrigin(Containerable containerable, ItemPath path) {
        assertOrigin(containerable, path, null, null);
    }

    private void assertNoOrigin(Containerable containerable, ItemPath path, Object realValue) {
        assertOrigin(containerable, path, realValue, null);
    }

    private void assertOrigin(Containerable containerable, ItemPath path, String oid) {
        assertOrigin(containerable, path, null, oid);
    }

    private void assertOrigin(Containerable containerable, ItemPath path, Object realValue, String oid) {
        Item<?, ?> item = containerable.asPrismContainerValue().findItem(path);
        boolean found = false;
        for (PrismValue value : item.getValues()) {
            if (realValueMatches(realValue, value)) {
                assertOrigin(value, oid);
                found = true;
            }
        }
        assertThat(found)
                .withFailMessage(() -> "value '" + realValue + "' was not found in " + path)
                .isTrue();
    }

    private boolean realValueMatches(Object query, PrismValue value) {
        return query == null || query.equals(value.getRealValue());
    }

    private void assertOrigin(PrismValue value, String oid) {
        List<PrismContainerValue<Containerable>> metadataValues = value.getValueMetadata().getValues();
        if (oid == null) {
            assertThat(metadataValues).as("metadata values").isEmpty();
        } else {
            assertThat(metadataValues).as("metadata values").hasSize(1);
            ProvenanceMetadataType provenance = ((ValueMetadataType) metadataValues.get(0).asContainerable()).getProvenance();
            assertThat(provenance).as("provenance metadata").isNotNull();
            List<ProvenanceAcquisitionType> acquisitionValues = provenance.getAcquisition();
            assertThat(acquisitionValues).as("acquisition metadata").hasSize(1);
            ObjectReferenceType originRef = acquisitionValues.get(0).getOriginRef();
            assertThat(originRef).as("origin ref").isNotNull();
            assertThat(originRef.getOid()).as("origin OID").isEqualTo(oid);
        }
    }

    /**
     * `basic2` inherits from `basic1` that inherits from `basic-template`.
     */
    @Test
    public void test110Basic2() throws CommonException {
        OperationResult result = getTestOperationResult();

        when("basic2 is expanded");
        ResourceExpansionOperation expansionOperation = new ResourceExpansionOperation(
                RESOURCE_BASIC_2.getObjectable().clone(),
                beans);
        expansionOperation.execute(result);

        then("expanded version can be displayed");
        ResourceType expanded = expansionOperation.getExpandedResource();
        assertResource(expanded, "after")
                .displayXml();

        and("origins are OK");
        assertNoOrigin(expanded, ResourceType.F_NAME);
        assertNoOrigin(expanded, ResourceType.F_SUPER);
        assertNoOrigin(expanded, ItemPath.create(ResourceType.F_SUPER, SuperResourceDeclarationType.F_RESOURCE_REF));
        assertOrigin(expanded, ResourceType.F_CONNECTOR_REF, RESOURCE_BASIC_TEMPLATE.oid);
        assertNoOrigin(expanded, ResourceType.F_CONNECTOR_CONFIGURATION);
        assertOrigin(
                expanded,
                ItemPath.create(ResourceType.F_CONNECTOR_CONFIGURATION, ICF_C_RESULTS_HANDLER_CONFIGURATION),
                RESOURCE_BASIC_1.oid); // this is not mentioned in basic-2
        assertNoOrigin(expanded,
                ItemPath.create(
                        ResourceType.F_CONNECTOR_CONFIGURATION,
                        ICF_C_RESULTS_HANDLER_CONFIGURATION,
                        "filteredResultsHandlerInValidationMode")); // Added in basic-1
        assertOrigin(expanded,
                ItemPath.create(
                        ResourceType.F_CONNECTOR_CONFIGURATION,
                        ICF_C_RESULTS_HANDLER_CONFIGURATION,
                        "enableNormalizingResultsHandler"),
                RESOURCE_BASIC_TEMPLATE.oid); // Present in template
        assertNoOrigin(
                expanded,
                ItemPath.create(ResourceType.F_CONNECTOR_CONFIGURATION, ICF_C_CONFIGURATION_PROPERTIES)); // defined in basic-2
        assertNoOrigin(expanded,
                ItemPath.create(
                        ResourceType.F_CONNECTOR_CONFIGURATION,
                        ICF_C_CONFIGURATION_PROPERTIES,
                        "uselessString")); // (re)defined in basic-2
        assertOrigin(expanded,
                ItemPath.create(
                        ResourceType.F_CONNECTOR_CONFIGURATION,
                        ICF_C_CONFIGURATION_PROPERTIES,
                        "uselessGuardedString"),
                RESOURCE_BASIC_TEMPLATE.oid); // inherited
        assertNoOrigin(expanded,
                ItemPath.create(
                        ResourceType.F_SCHEMA,
                        XmlSchemaType.F_GENERATION_CONSTRAINTS,
                        SchemaGenerationConstraintsType.F_GENERATE_OBJECT_CLASS),
                new QName("B"));
        assertNoOrigin(expanded,
                ItemPath.create(
                        ResourceType.F_SCHEMA,
                        XmlSchemaType.F_GENERATION_CONSTRAINTS,
                        SchemaGenerationConstraintsType.F_GENERATE_OBJECT_CLASS),
                new QName("C"));
        assertOrigin(expanded,
                ItemPath.create(
                        ResourceType.F_SCHEMA,
                        XmlSchemaType.F_GENERATION_CONSTRAINTS,
                        SchemaGenerationConstraintsType.F_GENERATE_OBJECT_CLASS),
                new QName("A"),
                RESOURCE_BASIC_1.oid);

        and("expanded values are OK");
        // @formatter:off
        assertResource(expanded, "after")
                .assertName("Basic 2")
                .assertNotAbstract()
                .configurationProperties()
                    .assertSize(4)
                    .assertAllItemsHaveCompleteDefinition()
                    .assertPropertyEquals(new ItemName("supportValidity"), false) // overridden (Basic 1)
                    .assertPropertyEquals(new ItemName("instanceId"), "basic") // from template
                    .assertPropertyEquals(new ItemName("uselessString"), "False!") // overridden (Basic 2)
                    .assertPropertyEquals(new ItemName("uselessGuardedString"),
                            new ProtectedStringType().clearValue("Dead men tell no tales")) // from template
                .end()
                .resultsHandlerConfiguration()
                    .assertSize(4)
                    .assertAllItemsHaveCompleteDefinition()
                    .assertPropertyEquals(F_FILTERED_RESULTS_HANDLER_IN_VALIDATION_MODE, true) // added by basic-1
                    .assertPropertyEquals(F_ENABLE_FILTERED_RESULTS_HANDLER, false) // from template
                    .assertPropertyEquals(F_ENABLE_ATTRIBUTES_TO_GET_SEARCH_RESULTS_HANDLER, false) // from template
                    .assertPropertyEquals(F_ENABLE_NORMALIZING_RESULTS_HANDLER, false) // from template
                .end()
                .assertConnectorRefIgnoringMetadata(RESOURCE_BASIC_TEMPLATE.getObjectable().getConnectorRef())
                .assertGeneratedClasses(new QName("A"), new QName("B"), new QName("C"));
        // @formatter:on

        and("ancestors are OK");
        assertThat(expansionOperation.getAncestorsOids())
                .as("ancestors OIDs")
                .containsExactlyInAnyOrder(RESOURCE_BASIC_TEMPLATE.oid, RESOURCE_BASIC_1.oid);
    }

    @Test
    public void test120MultipleConnectors() throws CommonException {
        OperationResult result = getTestOperationResult();

        when("additional-connectors-1 is expanded");
        ResourceExpansionOperation expansionOperation = new ResourceExpansionOperation(
                RESOURCE_ADDITIONAL_CONNECTORS_1.getObjectable().clone(),
                beans);
        expansionOperation.execute(result);

        then("expanded version is OK");
        ResourceType expanded = expansionOperation.getExpandedResource();
        // @formatter:off
        assertResource(expanded, "after")
                .assertName("With additional connectors 1")
                .assertNotAbstract()
                .configurationProperties()
                    .assertSize(2)
                    .assertAllItemsHaveCompleteDefinition()
                    .assertPropertyEquals(new ItemName("instanceId"), "main") // from template
                    .assertPropertyEquals(new ItemName("supportValidity"), true) // from specific
                .end()
                .assertConnectorRefIgnoringMetadata(RESOURCE_ADDITIONAL_CONNECTORS_TEMPLATE.getObjectable().getConnectorRef())
                .assertAdditionalConnectorsCount(3)
                .configurationProperties("first")
                    .assertSize(2)
                    .assertAllItemsHaveCompleteDefinition()
                    .assertPropertyEquals(new ItemName("instanceId"), "first") // from template
                    .assertPropertyEquals(new ItemName("uselessString"), "merged") // from specific
                .end()
                .configurationProperties("second") // from template
                    .assertSize(1)
                    .assertAllItemsHaveCompleteDefinition()
                    .assertPropertyEquals(new ItemName("instanceId"), "second")
                .end()
                .configurationProperties("third") // from specific
                    .assertSize(1)
                    .assertAllItemsHaveCompleteDefinition()
                    .assertPropertyEquals(new ItemName("instanceId"), "third")
                .end();
        // @formatter:on

        and("ancestors are OK");
        assertThat(expansionOperation.getAncestorsOids())
                .as("ancestors OIDs")
                .containsExactly(RESOURCE_ADDITIONAL_CONNECTORS_TEMPLATE.oid);
    }

    /**
     * Tests `object-types-1` without obtaining the schema. So we just check that there are four object types
     * with the inheritance relations.
     */
    @Test
    public void test200ObjectTypesRaw() throws CommonException {
        OperationResult result = getTestOperationResult();

        when("object-types-1 is expanded");
        ResourceExpansionOperation expansionOperation = new ResourceExpansionOperation(
                RESOURCE_OBJECT_TYPES_1_RAW.getObjectable().clone(),
                beans);
        expansionOperation.execute(result);

        then("expanded version is OK");
        ResourceType expandedResource = expansionOperation.getExpandedResource();
        // @formatter:off
        assertResource(expandedResource, "after")
                .displayXml()
                .assertName("object-types-1")
                .assertNotAbstract()
                .configurationProperties() // all from specific
                    .assertSize(1)
                    .assertAllItemsHaveCompleteDefinition()
                    .assertPropertyEquals(new ItemName("instanceId"), "object-types-1")
                .end()
                .assertConnectorRefIgnoringMetadata(RESOURCE_OBJECT_TYPES_TEMPLATE.getObjectable().getConnectorRef())
                .assertAdditionalConnectorsCount(0);
        // @formatter:on

        and("there are 3 object types");
        List<ResourceObjectTypeDefinitionType> typeDefBeans = expandedResource.getSchemaHandling().getObjectType();
        assertThat(typeDefBeans).as("type definition beans").hasSize(3);

        and("account has a single (merged) definition");
        List<ResourceObjectTypeDefinitionType> accountDefs = typeDefBeans.stream()
                .filter(def -> def.getKind() == ShadowKindType.ACCOUNT)
                .collect(Collectors.toList());
        assertThat(accountDefs).as("account definitions").hasSize(1);

        and("origins in account definition are OK");
        ResourceObjectTypeDefinitionType accountDef = accountDefs.get(0);
        assertNoOrigin(accountDef, ResourceObjectTypeDefinitionType.F_KIND);
        assertNoOrigin(accountDef, ResourceObjectTypeDefinitionType.F_INTENT);
        assertOrigin(accountDef, ResourceObjectTypeDefinitionType.F_DISPLAY_NAME, RESOURCE_OBJECT_TYPES_TEMPLATE.oid);
        assertOrigin(accountDef, ResourceObjectTypeDefinitionType.F_OBJECT_CLASS, RESOURCE_OBJECT_TYPES_TEMPLATE.oid);
    }

    /**
     * Tests `object-types-1` in full - i.e. with the schema.
     */
    @Test
    public void test210ObjectTypesFull() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("old repo object is deleted (if exists)");
        try {
            repositoryService.deleteObject(ResourceType.class, RESOURCE_OBJECT_TYPES_1.oid, new OperationResult("delete"));
        } catch (ObjectNotFoundException e) {
            // ignored
        }

        when("object-types-1 is initialized");
        initDummyResource(RESOURCE_OBJECT_TYPES_1, result);

        then("object-types-1 is successfully tested");
        testResourceAssertSuccess(RESOURCE_OBJECT_TYPES_1, task, result); // updates the object

        and("schema can be retrieved");
        PrismObject<ResourceType> current =
                provisioningService.getObject(
                        ResourceType.class, RESOURCE_OBJECT_TYPES_1.oid, null, task, result);
        ResourceSchema schema = ResourceSchemaFactory.getCompleteSchema(current);

        displayDumpable("schema", schema);

        and("there is a definition of account/default");
        ResourceObjectTypeDefinition accountDef =
                findObjectTypeDefinitionRequired(schema, ShadowKindType.ACCOUNT, SchemaConstants.INTENT_DEFAULT);

        and("gossip is added in types-1");
        ResourceAttributeDefinition<?> gossipDef =
                accountDef.findAttributeDefinitionRequired(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_QNAME);
        PropertyLimitations gossipModelLimitations = gossipDef.getLimitations(LayerType.MODEL);
        assertThat(gossipModelLimitations.canRead()).as("read access to gossip").isFalse();
        assertThat(gossipModelLimitations.canAdd()).as("add access to gossip").isTrue();
        assertThat(gossipModelLimitations.canModify()).as("modify access to gossip").isTrue();

        and("drink is updated");
        ResourceAttributeDefinition<?> drinkDef =
                accountDef.findAttributeDefinitionRequired(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_QNAME);
        PropertyLimitations drinkModelLimitations = drinkDef.getLimitations(LayerType.MODEL);
        assertThat(drinkModelLimitations.canRead()).as("read access to drink").isTrue();
        assertThat(drinkModelLimitations.canAdd()).as("add access to drink").isTrue(); // overridden in types-1
        assertThat(drinkModelLimitations.canModify()).as("modify access to drink").isTrue();
        assertThat(drinkDef.isTolerant()).as("drink 'tolerant' flag").isFalse(); // overridden in types-1

        and("inbound mapping in weapon is updated");
        ResourceAttributeDefinition<?> weaponDef =
                accountDef.findAttributeDefinitionRequired(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_QNAME);
        List<InboundMappingType> weaponInbounds = weaponDef.getInboundMappingBeans();
        assertThat(weaponInbounds).as("weapon inbound mappings").hasSize(1);
        InboundMappingType weaponInbound = weaponInbounds.get(0);
        assertThat(weaponInbound.isTrace()).as("weapon mapping traced flag").isTrue(); // in types-1
        assertThat(weaponInbound.getStrength()).as("weapon mapping strength").isEqualTo(MappingStrengthType.STRONG);
        assertThat(weaponInbound.getName()).as("weapon mapping name").isEqualTo("weapon-mapping"); // this is the key

        and("one protected pattern is added (there are two, but one is exactly the same as one in parent)");
        Collection<ResourceObjectPattern> protectedPatterns = accountDef.getProtectedObjectPatterns();
        assertThat(protectedPatterns).as("protected object patterns").hasSize(3); // 2 inherited, 1 added
        Set<String> names = protectedPatterns.stream().map(this::getFilterValue).collect(Collectors.toSet());
        assertThat(names)
                .as("protected objects names")
                .containsExactlyInAnyOrder("root", "daemon", "extra");

        and("association ri:group is updated");
        Collection<ResourceAssociationDefinition> associationDefinitions = accountDef.getAssociationDefinitions();
        displayCollection("associations", associationDefinitions);
        assertThat(associationDefinitions).as("association definitions").hasSize(2);

        QName groupQName = new QName(NS_RI, "group");
        ResourceAssociationDefinition groupDef = accountDef.findAssociationDefinitionRequired(groupQName, () -> "");
        assertThat(groupDef.requiresExplicitReferentialIntegrity())
                .as("requiresExplicitReferentialIntegrity flag")
                .isFalse();
        assertThat(groupDef.getName())
                .as("group name")
                .isEqualTo(groupQName); // i.e. it's qualified

        and("synchronization reactions are correctly merged");
        Collection<SynchronizationReactionDefinition> reactions = accountDef.getSynchronizationReactions();
        assertThat(reactions).as("sync reactions").hasSize(2);
        SynchronizationReactionDefinition unnamed =
                reactions.stream().filter(r -> r.getName() == null).findFirst().orElseThrow();
        assertThat(unnamed.getSituations())
                .as("situations in unnamed")
                .containsExactly(SynchronizationSituationType.UNMATCHED);
        assertThat(unnamed.getActions())
                .as("actions in unnamed")
                .hasSize(1);
        assertThat(unnamed.getActions().get(0).getNewDefinitionBeanClass())
                .as("action in unnamed")
                .isEqualTo(DeleteResourceObjectSynchronizationActionType.class);
        SynchronizationReactionDefinition reaction1 =
                reactions.stream().filter(r -> "reaction1".equals(r.getName())).findFirst().orElseThrow();
        assertThat(reaction1.getSituations())
                .as("situations in reaction1")
                .containsExactly(SynchronizationSituationType.UNMATCHED);
        assertThat(reaction1.getChannels())
                .as("channels in reaction1")
                .containsExactly("channel1"); // types-1
        List<SynchronizationActionDefinition> actions = reaction1.getActions();
        assertThat(actions)
                .as("actions in reaction1")
                .hasSize(2);
        SynchronizationActionDefinition unnamedAction =
                actions.stream().filter(a -> a.getName() == null).findFirst().orElseThrow();
        assertThat(unnamedAction.getNewDefinitionBeanClass())
                .as("definition bean class in unnamed action")
                .isEqualTo(InactivateResourceObjectSynchronizationActionType.class);
        AddFocusSynchronizationActionType addFocusAction =
                (AddFocusSynchronizationActionType) actions.stream()
                        .filter(a -> "add-focus".equals(a.getName()))
                        .findFirst().orElseThrow()
                        .getNewDefinitionBean();
        assert addFocusAction != null;
        assertThat(addFocusAction.getName()).isEqualTo("add-focus");
        assertThat(addFocusAction.isSynchronize()).isTrue(); // types-1
        assertThat(addFocusAction.getDocumentation()).isEqualTo("Adding a focus"); // parent

        and("capabilities are OK");
        ReadCapabilityType read = accountDef.getEnabledCapability(ReadCapabilityType.class, current.asObjectable());
        assertThat(read).as("read capability").isNotNull();

        // Note that resource-level and object-type-level capabilities are currently NOT combined (merged) together.
        // They are being replaced. This is years-old behavior that we won't change today.
        // (In this particular case, caching-only is set at the resource level in the template.)
        assertThat(read.isCachingOnly()).as("caching-only flag").isNull();

        // Here we check that
        // @formatter:off
        assertResource(current, "after")
                .assertConfiguredCapabilities(2)
                .configuredCapability(ReadCapabilityType.class) // inherited
                    .assertPropertyEquals(ReadCapabilityType.F_ENABLED, false)
                    .assertPropertyEquals(ReadCapabilityType.F_CACHING_ONLY, true)
                .end()
                .configuredCapability(UpdateCapabilityType.class) // types-1
                .end();
        // @formatter:on

        // This one is object-class specific and is inherited from the template
        AsyncUpdateCapabilityType asyncUpdate =
                accountDef.getEnabledCapability(AsyncUpdateCapabilityType.class, current.asObjectable());
        assertThat(asyncUpdate).as("async update capability").isNotNull();

        // And this one is new in types-1/account/default
        PagedSearchCapabilityType pagedSearch =
                accountDef.getEnabledCapability(PagedSearchCapabilityType.class, current.asObjectable());
        assertThat(pagedSearch).as("paged search capability").isNotNull();

        and("there are two 'items' correlators");
        CorrelationDefinitionType correlationDefinitionBean = Objects.requireNonNull(accountDef.getCorrelationDefinitionBean());
        List<ItemsSubCorrelatorType> itemsCorrelators = correlationDefinitionBean.getCorrelators().getItems();
        assertThat(itemsCorrelators).as("items correlators").hasSize(2);
        ItemsCorrelatorType empNo = MiscUtil.extractSingletonRequired(
                itemsCorrelators.stream()
                        .filter(c -> "empNo".equals(c.getName()))
                        .collect(Collectors.toList()),
                () -> new AssertionError("multiple empNo correlators"),
                () -> new AssertionError("no empNo correlator"));
        // Use of toString() is a hack (it's hard to compare paths using equals())
        assertThat(empNo.getItem().get(0).getRef().getItemPath().toString()).as("empNo path")
                .isEqualTo(prismContext.toUniformPath(UserType.F_EMPLOYEE_NUMBER).toString());
        ItemsCorrelatorType unnamedCorrelator = MiscUtil.extractSingletonRequired(
                itemsCorrelators.stream()
                        .filter(c -> c.getName() == null)
                        .collect(Collectors.toList()),
                () -> new AssertionError("multiple unnamed correlators"),
                () -> new AssertionError("no unnamed correlator"));
        assertThat(unnamedCorrelator.getItem()).hasSize(2);

        and("default inbound mappings phases are merged");
        // This is a bit counter-intuitive, but according to the general merging algorithm.
        // Maybe we should create a special rule that would allow overriding these phases, but I think
        // that the current approach is OK. If some overriding would be needed, it should be more generic,
        // to be usable also for other cases.
        DefaultInboundMappingEvaluationPhasesType phases = accountDef.getDefaultInboundMappingEvaluationPhases();
        assertThat(phases).isNotNull();
        assertThat(phases.getPhase())
                .as("default inbound mapping evaluation phases")
                .containsExactlyInAnyOrder(CLOCKWORK, BEFORE_CORRELATION);

        and("resource is cached");
        rememberResourceCacheStats();
        PrismObject<ResourceType> reloaded =
                provisioningService.getObject(
                        ResourceType.class, RESOURCE_OBJECT_TYPES_1.oid, null, task, result);
        assertResourceCacheHitsIncrement(1);

        and("origins are preserved in the cached version");
        assertResource(reloaded, "after")
                .displayXml();
        assertOrigin(reloaded.asObjectable(), ResourceType.F_CONNECTOR_REF, RESOURCE_OBJECT_TYPES_TEMPLATE.oid);
    }

    /**
     * Tests explicit type inheritance (in a single resource).
     */
    @Test
    public void test220ExplicitTypeInheritance() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("resource is initialized");
        initDummyResource(RESOURCE_EXPLICIT_TYPE_INHERITANCE, result);

        then("resource is successfully tested");
        testResourceAssertSuccess(RESOURCE_EXPLICIT_TYPE_INHERITANCE, task, result); // updates the object

        and("schema can be retrieved");
        PrismObject<ResourceType> current =
                provisioningService.getObject(
                        ResourceType.class, RESOURCE_EXPLICIT_TYPE_INHERITANCE.oid, null, task, result);
        ResourceSchema schema = ResourceSchemaFactory.getCompleteSchema(current);

        displayDumpable("schema", schema);

        and("there is a definition of account/employee");
        ResourceObjectTypeDefinition employeeDef =
                findObjectTypeDefinitionRequired(schema, ShadowKindType.ACCOUNT, "employee");

        and("drink is updated");
        ResourceAttributeDefinition<?> employeeDrinkDef =
                employeeDef.findAttributeDefinitionRequired(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_QNAME);
        PropertyLimitations drinkModelLimitations = employeeDrinkDef.getLimitations(LayerType.MODEL);
        assertThat(drinkModelLimitations.canRead()).as("read access to drink").isTrue();
        assertThat(drinkModelLimitations.canAdd()).as("add access to drink").isFalse();
        assertThat(drinkModelLimitations.canModify()).as("modify access to drink").isTrue();
        assertThat(employeeDrinkDef.isTolerant()).as("drink 'tolerant' flag").isFalse(); // overridden in this type

        and("there is a definition of account/admin");
        ResourceObjectTypeDefinition adminDef =
                findObjectTypeDefinitionRequired(schema, ShadowKindType.ACCOUNT, "admin");

        and("drink is updated");
        ResourceAttributeDefinition<?> adminDrinkDef =
                adminDef.findAttributeDefinitionRequired(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_QNAME);
        assertThat(adminDrinkDef.isTolerant()).as("drink 'tolerant' flag").isTrue(); // default
        assertThat(adminDrinkDef.isIgnored(LayerType.MODEL)).as("drink 'ignored' flag").isTrue(); // overridden
        assertThat(adminDrinkDef.getDocumentation()).isEqualTo("Administrators do not drink!");

        and("there is no definition of account/general");
        ResourceObjectTypeDefinition generalDef =
                schema.getObjectTypeDefinition(ShadowKindType.ACCOUNT, "general");
        assertThat(generalDef).as("definition of account/general").isNull();

        List<? extends ResourceObjectTypeDefinition> accountDefs = schema.getObjectTypeDefinitions(ShadowKindType.ACCOUNT);
        assertThat(accountDefs).as("account definitions").hasSize(2);
    }

    /** Hacked: gets the value of (assuming) single property value filter in the pattern. */
    private String getFilterValue(ResourceObjectPattern pattern) {
        //noinspection unchecked
        return Objects.requireNonNull(((PropertyValueFilter<String>) pattern.getObjectFilter()).getValues())
                .get(0).getRealValue();
    }
}
