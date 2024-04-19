/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.dummy;

import static com.evolveum.midpoint.schema.GetOperationOptions.createReadOnlyCollection;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.ICFS_NAME_PATH;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.util.AbstractShadow;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import org.jetbrains.annotations.Nullable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.test.DummyHrScenario;
import com.evolveum.midpoint.test.DummyHrScenario.OrgUnit;
import com.evolveum.midpoint.test.DummyHrScenario.Person;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

import java.io.File;
import java.util.Collection;

/**
 * Testing the native associations.
 *
 * TEMPORARY. This functionality will be moved to standard {@link TestDummy} later:
 * see {@link TestDummyNativeAssociations} (currently failing).
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummyAssociations extends AbstractDummyTest {

    public static final File RESOURCE_DUMMY_HR_FILE = new File(TEST_DIR, "resource-dummy-hr.xml");

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
}
