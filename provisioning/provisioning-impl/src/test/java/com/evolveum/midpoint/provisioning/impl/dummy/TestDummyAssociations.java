/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.GetOperationOptions.createReadOnlyCollection;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;

import java.io.File;
import java.util.Collection;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyObject;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.processor.ShadowAssociationValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyHrScenario;
import com.evolveum.midpoint.test.DummyHrScenario.Contract;
import com.evolveum.midpoint.test.DummyHrScenario.OrgUnit;
import com.evolveum.midpoint.test.DummyHrScenario.Person;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

/**
 * Testing the native associations.
 *
 * TEMPORARY. This functionality will be moved to standard {@link TestDummy} later:
 * see {@link TestDummyNativeAssociations} (currently failing).
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummyAssociations extends AbstractDummyTest {

    private static final File RESOURCE_DUMMY_HR_FILE = new File(TEST_DIR, "resource-dummy-hr.xml");

    private DummyHrScenario hrScenario;

    @Override
    protected File getResourceDummyFile() {
        return RESOURCE_DUMMY_HR_FILE;
    }

    @Override
    protected void extraDummyResourceInit() throws Exception {
        hrScenario = DummyHrScenario.on(dummyResourceCtl);
        hrScenario.initialize();

        createCommonHrObjects();

        var task = getTestTask();
        var result = task.getResult();

        OperationResult testResult = provisioningService.testResource(RESOURCE_DUMMY_OID, task, result);
        assertSuccess(testResult);

        resource = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, task, result);
        resourceBean = resource.asObjectable();
    }

    /** These objects should be usable in all tests. */
    private void createCommonHrObjects() throws Exception {
        DummyObject sciences = hrScenario.orgUnit.add("sciences")
                .addAttributeValues(OrgUnit.AttributeNames.DESCRIPTION.local(), "Faculty of Sciences");
        DummyObject law = hrScenario.orgUnit.add("law")
                .addAttributeValues(OrgUnit.AttributeNames.DESCRIPTION.local(), "Faculty of Law");

        DummyObject john = hrScenario.person.add("john")
                .addAttributeValue(Person.AttributeNames.FIRST_NAME.local(), "John")
                .addAttributeValue(Person.AttributeNames.LAST_NAME.local(), "Doe")
                .addAttributeValue(Person.AttributeNames.TITLE.local(), "Ing.");

        DummyObject johnContractSciences = hrScenario.contract.add("john-sciences");
        DummyObject johnContractLaw = hrScenario.contract.add("john-law");

        hrScenario.personContract.add(john, johnContractSciences);
        hrScenario.personContract.add(john, johnContractLaw);

        hrScenario.contractOrgUnit.add(johnContractSciences, sciences);
        hrScenario.contractOrgUnit.add(johnContractLaw, law);
    }

    /** Are associations in the schema? */
    @Test
    public void test100CheckResourceSchema() throws Exception {
        given("the resource schema, fetched from the resource");
        SchemaDefinitionType definition = resourceBean.getSchema().getDefinition();
        displayValue("schema definition",
                prismContext.xmlSerializer().root(new QName("schema")).serializeRealValue(definition));

        when("getting schema for 'person'");
        var schema = Resource.of(resource).getCompleteSchemaRequired();
        var person = schema.findObjectClassDefinitionRequired(Person.OBJECT_CLASS_NAME.xsd());

        then("there is an association 'person->contract'");
        displayDumpable("person OC schema", person);
        var contractAssocDef = person.findAssociationDefinitionRequired(Person.LinkNames.CONTRACT.q());
    }

    /** Just read an account with associations (in various modes: get vs search, read-write vs read-only). */
    @Test
    public void test110GetObjectWithAssociations() throws Exception {
        executeSearchForJohnWithAssociations(null);
        var oid = executeSearchForJohnWithAssociations(createReadOnlyCollection());

        executeGetJohnWithAssociations(oid, null);
        executeGetJohnWithAssociations(oid, createReadOnlyCollection());
    }

    private String executeSearchForJohnWithAssociations(Collection<SelectorOptions<GetOperationOptions>> options)
            throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("searching for person 'john'");
        var objects = provisioningService.searchObjects(
                ShadowType.class,
                Resource.of(resource)
                        .queryFor(Person.OBJECT_CLASS_NAME.xsd())
                        .and().item(ICFS_NAME_PATH).eq("john")
                        .build(),
                options, task, result);

        then("there is a person 'john'");
        assertThat(objects).as("persons named john").hasSize(1);
        PrismObject<ShadowType> john = objects.get(0);

        assertJohn(john);

        return john.getOid();
    }

    private void executeGetJohnWithAssociations(String oid, Collection<SelectorOptions<GetOperationOptions>> options)
            throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("getting person 'john' by OID");
        var john = provisioningService.getObject(ShadowType.class, oid, options, task, result);

        then("John is OK");
        assertJohn(john);
    }

    private void assertJohn(PrismObject<ShadowType> shadow) {
        assertShadow(shadow, "john")
                .display()
                .associations()
                .assertValuesCount(2);

        var johnLawContract = AbstractShadow.of(shadow)
                .getAssociationValues(Person.LinkNames.CONTRACT.q())
                .stream()
                .map(val -> val.getShadow())
                .filter(s -> s.getName().getOrig().equals("john-law"))
                .findFirst().orElseThrow();
        var def = johnLawContract.getObjectDefinition();
        displayDumpable("johnLaw contract definition", def);
        assertThat(def.getTypeIdentification())
                .as("johnLaw contract object definition type")
                .isEqualTo(ResourceObjectTypeIdentification.of(ShadowKindType.ASSOCIATED, "contract"));
    }

    @Test
    public void test200AddAnnWithContract() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        given("ann with a contract in sciences");
        var sciencesShadow = getOrgUnitByName("sciences", task, result);

        var annContractShadow = Resource.of(resource)
                .getCompleteSchemaRequired()
                .findObjectClassDefinitionRequired(hrScenario.contract.getObjectClassName().xsd())
                .createBlankShadow();
        annContractShadow.getAttributesContainer()
                .add(ICFS_NAME, "ann-sciences");
        annContractShadow.getOrCreateAssociationsContainer()
                .add(Contract.LinkNames.ORG.q(), AbstractShadow.of(sciencesShadow));

        var annShadow = Resource.of(resource)
                .getCompleteSchemaRequired()
                .findObjectClassDefinitionRequired(hrScenario.person.getObjectClassName().xsd())
                .createBlankShadow();
        annShadow.getAttributesContainer()
                .add(Person.AttributeNames.NAME.q(), "ann")
                .add(Person.AttributeNames.FIRST_NAME.q(), "Ann")
                .add(Person.AttributeNames.LAST_NAME.q(), "Green");
        annShadow.getOrCreateAssociationsContainer()
                .add(Person.LinkNames.CONTRACT.q(), annContractShadow);

        when("ann is created on the resource");
        provisioningService.addObject(annShadow.getPrismObject(), null, null, task, result);

        then("she's there");
        var annShadowAfter = getPersonByName("ann", task, result);

        assertShadow(annShadowAfter, "ann")
                .display()
                .attributes()
                .assertValue(ICFS_UID, "ann")
                .assertValue(Person.AttributeNames.NAME.q(), "ann")
                .assertValue(Person.AttributeNames.FIRST_NAME.q(), "Ann")
                .assertValue(Person.AttributeNames.LAST_NAME.q(), "Green");

        var annContractsAfter = AbstractShadow.of(annShadowAfter)
                .getAssociationValues(Person.LinkNames.CONTRACT.q())
                .stream()
                .map(val -> val.getShadowBean())
                .toList();

        assertThat(annContractsAfter)
                .as("ann's contracts")
                .hasSize(1);

        var annContractAfter = annContractsAfter.get(0);
        assertShadow(annContractAfter, "ann's contract")
                .display()
                .attributes()
                .assertValue(ICFS_UID, "ann-sciences")
                .assertValue(Contract.AttributeNames.NAME.q(), "ann-sciences");

        var contractOrgsAfter = AbstractShadow.of(annContractAfter)
                .getAssociationValues(Contract.LinkNames.ORG.q())
                .stream()
                .map(val -> val.getShadowBean())
                .toList();

        assertThat(contractOrgsAfter)
                .as("ann's contract's orgs")
                .hasSize(1);

        var contractOrgAfter = contractOrgsAfter.get(0);
        assertShadow(contractOrgAfter, "ann's contract's org")
                .display()
                .attributes()
                .assertValue(ICFS_UID, "sciences")
                .assertValue(OrgUnit.AttributeNames.NAME.q(), "sciences");
    }

    @Test
    public void test210AddDeleteContract() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        given("bob account is on resource (no contract)");
        var sciencesShadow = getOrgUnitByName("sciences", task, result);

        var bobShadow = Resource.of(resource)
                .getCompleteSchemaRequired()
                .findObjectClassDefinitionRequired(hrScenario.person.getObjectClassName().xsd())
                .createBlankShadow();
        bobShadow.getAttributesContainer()
                .add(Person.AttributeNames.NAME.q(), "bob")
                .add(Person.AttributeNames.FIRST_NAME.q(), "Bob")
                .add(Person.AttributeNames.LAST_NAME.q(), "Black");

        provisioningService.addObject(bobShadow.getPrismObject(), null, null, task, result);

        when("bob's contract on sciences is created");

        var bobContractShadow = Resource.of(resource)
                .getCompleteSchemaRequired()
                .findObjectClassDefinitionRequired(hrScenario.contract.getObjectClassName().xsd())
                .createBlankShadow();
        bobContractShadow.getAttributesContainer()
                .add(ICFS_NAME, "bob-sciences");
        bobContractShadow.getOrCreateAssociationsContainer()
                .add(Contract.LinkNames.ORG.q(), AbstractShadow.of(sciencesShadow));

        provisioningService.modifyObject(
                ShadowType.class,
                bobShadow.getOidRequired(),
                Resource.of(resource)
                        .deltaFor(Person.OBJECT_CLASS_NAME.xsd())
                        .item(Person.LinkNames.CONTRACT.path())
                        .add(ShadowAssociationValue.of(bobContractShadow.clone(), false))
                        .asItemDeltas(),
                null, null, task, result);

        then("the contract is there");
        var bobShadowAfterAdding = getPersonByName("bob", task, result);

        assertShadow(bobShadowAfterAdding, "after adding")
                .display()
                .attributes()
                .assertValue(ICFS_UID, "bob")
                .assertValue(Person.AttributeNames.NAME.q(), "bob")
                .assertValue(Person.AttributeNames.FIRST_NAME.q(), "Bob")
                .assertValue(Person.AttributeNames.LAST_NAME.q(), "Black");

        var contractsAfterAdding = AbstractShadow.of(bobShadowAfterAdding)
                .getAssociationValues(Person.LinkNames.CONTRACT.q())
                .stream()
                .map(val -> val.getShadowBean())
                .toList();

        assertThat(contractsAfterAdding)
                .as("contracts after adding")
                .hasSize(1);

        var contractAfterAdding = contractsAfterAdding.get(0);
        assertShadow(contractAfterAdding, "contract after adding")
                .display()
                .attributes()
                .assertValue(ICFS_UID, "bob-sciences")
                .assertValue(Contract.AttributeNames.NAME.q(), "bob-sciences");

        var contractOrgsAfter = AbstractShadow.of(contractAfterAdding)
                .getAssociationValues(Contract.LinkNames.ORG.q())
                .stream()
                .map(val -> val.getShadowBean())
                .toList();

        assertThat(contractOrgsAfter)
                .as("contract's orgs after adding")
                .hasSize(1);

        var contractOrgAfter = contractOrgsAfter.get(0);
        assertShadow(contractOrgAfter, "contract's org")
                .display()
                .attributes()
                .assertValue(ICFS_UID, "sciences")
                .assertValue(OrgUnit.AttributeNames.NAME.q(), "sciences");

        when("contract is removed (by value)");
        bobContractShadow.getBean().setOid(null);
        provisioningService.modifyObject(
                ShadowType.class,
                bobShadow.getOidRequired(),
                Resource.of(resource)
                        .deltaFor(Person.OBJECT_CLASS_NAME.xsd())
                        .item(Person.LinkNames.CONTRACT.path())
                        .delete(ShadowAssociationValue.of(bobContractShadow.clone(), false))
                        .asItemDeltas(),
                null, null, task, result);

        then("the contract is no longer there");
        var bobShadowAfterDeleting = getPersonByName("bob", task, result);

        assertShadow(bobShadowAfterDeleting, "after deleting")
                .display()
                .attributes()
                .assertValue(ICFS_UID, "bob")
                .assertValue(Person.AttributeNames.NAME.q(), "bob")
                .assertValue(Person.AttributeNames.FIRST_NAME.q(), "Bob")
                .assertValue(Person.AttributeNames.LAST_NAME.q(), "Black");

        var contractsAfterDeleting = AbstractShadow.of(bobShadowAfterDeleting)
                .getAssociationValues(Person.LinkNames.CONTRACT.q());

        assertThat(contractsAfterDeleting)
                .as("contracts after deleting")
                .isEmpty();
    }

    private @NotNull PrismObject<ShadowType> getOrgUnitByName(String name, Task task, OperationResult result) throws Exception {
        return MiscUtil.extractSingletonRequired(
                provisioningService.searchObjects(
                        ShadowType.class,
                        Resource.of(resource)
                                .queryFor(OrgUnit.OBJECT_CLASS_NAME.xsd())
                                .and().item(ICFS_NAME_PATH).eq(name)
                                .build(),
                        null, task, result));
    }

    private @NotNull PrismObject<ShadowType> getPersonByName(String name, Task task, OperationResult result) throws Exception {
        return MiscUtil.extractSingletonRequired(
                provisioningService.searchObjects(
                        ShadowType.class,
                        Resource.of(resource)
                                .queryFor(Person.OBJECT_CLASS_NAME.xsd())
                                .and().item(Person.AttributeNames.NAME.path()).eq(name)
                                .build(),
                        null, task, result));
    }
}
