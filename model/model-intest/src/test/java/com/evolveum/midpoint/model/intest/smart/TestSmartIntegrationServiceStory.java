/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.smart;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.test.util.MidPointTestConstants.TEST_RESOURCES_PATH;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.util.CloneUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.model.test.smart.MockServiceClientImpl;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.smart.impl.SmartIntegrationServiceImpl;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * A story test covering {@link SmartIntegrationService} functionality.
 *
 * Maybe we will move this test into `story` module later.
 * But for now, it is a good place to put it.
 *
 * This test ignores external service client and uses a mock service client instead.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestSmartIntegrationServiceStory extends AbstractEmptyModelIntegrationTest {

    public static final File TEST_DIR = new File(TEST_RESOURCES_PATH, "smart/story");

    private static final int TIMEOUT = 20000;

    /** Using the implementation in order to set mock service client for testing. */
    @Autowired private SmartIntegrationServiceImpl smartIntegrationService;

    private static final ResourceObjectTypeIdentification HR_PERSON =
            ResourceObjectTypeIdentification.of(ShadowKindType.ACCOUNT, "person");
    private static final ResourceObjectTypeIdentification HR_DEPARTMENT =
            ResourceObjectTypeIdentification.of(ShadowKindType.GENERIC, "department");

    private static DummyHrScenario hrScenario;

    private static final DummyTestResource RESOURCE_DUMMY_HR = new DummyTestResource(
            TEST_DIR, "resource-dummy-hr.xml", "8185c3f1-032e-400b-ac3e-ece3ee801326",
            "hr",
            c -> hrScenario = DummyHrScenario.on(c).initialize());

    private static final DummyTestResource RESOURCE_DUMMY_DIRECTORY = new DummyTestResource(
            TEST_DIR, "resource-dummy-directory.xml", "c7c97e57-4485-41b7-a23d-c441bbb0c395",
            "directory",
            c -> DummyDirectoryScenario.on(c).initialize());

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initTestObjects(initTask, initResult,
                CommonInitialObjects.ARCHETYPE_UTILITY_TASK);

        initAndTestDummyResource(RESOURCE_DUMMY_HR, initTask, initResult);
        initAndTestDummyResource(RESOURCE_DUMMY_DIRECTORY, initTask, initResult);

        createHrAccounts();
        createHrDepartments();
    }

    private void createHrAccounts() throws Exception {
        hrScenario.person.add("10000001")
                .addAttributeValues(DummyHrScenario.Person.AttributeNames.FULLNAME.local(), "Jack Sparrow")
                .addAttributeValues(DummyHrScenario.Person.AttributeNames.FIRSTNAME.local(), "Jack")
                .addAttributeValues(DummyHrScenario.Person.AttributeNames.LASTNAME.local(), "Sparrow")
                .addAttributeValues(DummyHrScenario.Person.AttributeNames.EMAIL.local(), "jack@evolveum.com")
                .addAttributeValues(DummyHrScenario.Person.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyHrScenario.Person.AttributeNames.TYPE.local(), "e") // e = employee
                .addAttributeValues(DummyHrScenario.Person.AttributeNames.PHONE.local(), "+420601040027");

        hrScenario.person.add("10000002")
                .addAttributeValues(DummyHrScenario.Person.AttributeNames.FULLNAME.local(), "Jim Hacker")
                .addAttributeValues(DummyHrScenario.Person.AttributeNames.FIRSTNAME.local(), "Jim")
                .addAttributeValues(DummyHrScenario.Person.AttributeNames.LASTNAME.local(), "Hacker")
                .addAttributeValues(DummyHrScenario.Person.AttributeNames.EMAIL.local(), "jim@evolveum.com")
                .addAttributeValues(DummyHrScenario.Person.AttributeNames.STATUS.local(), "inactive");

        hrScenario.person.add("10000003")
                .addAttributeValues(DummyHrScenario.Person.AttributeNames.FULLNAME.local(), "Alice Wonderland")
                .addAttributeValues(DummyHrScenario.Person.AttributeNames.FIRSTNAME.local(), "Alice")
                .addAttributeValues(DummyHrScenario.Person.AttributeNames.LASTNAME.local(), "Wonderland")
                .addAttributeValues(DummyHrScenario.Person.AttributeNames.EMAIL.local(), "alice@evolveum.com")
                .addAttributeValues(DummyHrScenario.Person.AttributeNames.PHONE.local(), "+421900111222")
                .addAttributeValues(DummyHrScenario.Person.AttributeNames.STATUS.local(), "active")
                .addAttributeValues(DummyHrScenario.Person.AttributeNames.TYPE.local(), "e");
                //.addAttributeValues(DummyHrScenario.Person.AttributeNames.DEPARTMENT.local(), "HR");

        hrScenario.person.add("10000004")
                .addAttributeValues(DummyHrScenario.Person.AttributeNames.FULLNAME.local(), "Bob Builder")
                .addAttributeValues(DummyHrScenario.Person.AttributeNames.FIRSTNAME.local(), "Bob")
                .addAttributeValues(DummyHrScenario.Person.AttributeNames.LASTNAME.local(), "Builder")
                .addAttributeValues(DummyHrScenario.Person.AttributeNames.EMAIL.local(), "bob@evolveum.com")
                .addAttributeValues(DummyHrScenario.Person.AttributeNames.PHONE.local(), "+421900333444")
                .addAttributeValues(DummyHrScenario.Person.AttributeNames.STATUS.local(), "inactive")
                .addAttributeValues(DummyHrScenario.Person.AttributeNames.TYPE.local(), "c"); // c = contractor
                //.addAttributeValues(DummyHrScenario.Person.AttributeNames.DEPARTMENT.local(), "Engineering");

        hrScenario.person.add("10000005")
                .addAttributeValues(DummyHrScenario.Person.AttributeNames.FULLNAME.local(), "Eve Adams")
                .addAttributeValues(DummyHrScenario.Person.AttributeNames.FIRSTNAME.local(), "Eve")
                .addAttributeValues(DummyHrScenario.Person.AttributeNames.LASTNAME.local(), "Adams")
                .addAttributeValues(DummyHrScenario.Person.AttributeNames.EMAIL.local(), "eve@evolveum.com")
                .addAttributeValues(DummyHrScenario.Person.AttributeNames.STATUS.local(), "locked")
                .addAttributeValues(DummyHrScenario.Person.AttributeNames.TYPE.local(), "e")
                .addAttributeValue(DummyHrScenario.Person.AttributeNames.VALID_FROM.local(), ZonedDateTime.parse("2023-09-01T12:00:00Z"));
    }

    private void createHrDepartments() throws Exception {
        hrScenario.department.add("1001")
                .addAttributeValue(DummyHrScenario.Department.AttributeNames.NAME.local(), "Engineering");
        hrScenario.department.add("1002")
                .addAttributeValue(DummyHrScenario.Department.AttributeNames.NAME.local(), "Marketing");
        hrScenario.department.add("1003")
                .addAttributeValue(DummyHrScenario.Department.AttributeNames.NAME.local(), "Sales");
        hrScenario.department.add("1004")
                .addAttributeValue(DummyHrScenario.Department.AttributeNames.NAME.local(), "Manufacturing");
        hrScenario.department.add("9999")
                .addAttributeValue(DummyHrScenario.Department.AttributeNames.NAME.local(), "HQ");
    }

    /** Obtains suggestions for object types for HR persons. */
    @Test
    public void test100SuggestObjectTypesForHrPerson() throws CommonException {
        //noinspection resource
        var mockServiceClient = new MockServiceClientImpl(
                new SiSuggestObjectTypesResponseType()
                        .objectType(new SiSuggestedObjectTypeType()
                                .kind(HR_PERSON.getKind().value())
                                .intent(HR_PERSON.getIntent())),
                new SiSuggestFocusTypeResponseType()
                        .focusTypeName(UserType.COMPLEX_TYPE));
        smartIntegrationService.setServiceClientSupplier(() -> mockServiceClient);

        when("submitting 'suggest object types' operation request and waiting for the result");
        var task = getTestTask();
        var result = task.getResult();
        QName personOcName = DummyHrScenario.Person.OBJECT_CLASS_NAME.xsd();
        var token = smartIntegrationService.submitSuggestObjectTypesOperation(RESOURCE_DUMMY_HR.oid, personOcName, task, result);

        when("waiting for the operation to finish successfully");
        var response = waitForFinish(
                () -> smartIntegrationService.getSuggestObjectTypesOperationStatus(token, task, result),
                TIMEOUT);

        then("there is the correct suggested object type");
        assertThat(response).isNotNull();
        var typeSuggestion = assertThat(response.getObjectType())
                .as("suggested object types collection")
                .hasSize(1)
                .element(0)
                .as("object type suggestion")
                .actual();
        assertThat(typeSuggestion.getKind()).isEqualTo(HR_PERSON.getKind());
        assertThat(typeSuggestion.getIntent()).isEqualTo(HR_PERSON.getIntent());
        var delineation = typeSuggestion.getDelineation();
        assertThat(delineation.getObjectClass()).isEqualTo(personOcName);
        assertThat(delineation.getAuxiliaryObjectClass()).isEmpty();
        assertThat(delineation.getFilter()).isEmpty();
        assertThat(delineation.getBaseContext()).isNull();

        applyObjectTypeSuggestions(RESOURCE_DUMMY_HR, response.getObjectType(), task, result);
    }

    /** Obtains suggestions for object types for HR departments. */
    @Test
    public void test110SuggestObjectTypesForHrDepartments() throws CommonException {
        //noinspection resource
        var mockServiceClient = new MockServiceClientImpl(
                new SiSuggestObjectTypesResponseType()
                        .objectType(new SiSuggestedObjectTypeType()
                                .kind(HR_DEPARTMENT.getKind().value())
                                .intent(HR_DEPARTMENT.getIntent())),
                new SiSuggestFocusTypeResponseType()
                        .focusTypeName(OrgType.COMPLEX_TYPE));
        smartIntegrationService.setServiceClientSupplier(() -> mockServiceClient);

        when("submitting 'suggest object types' operation request and waiting for the result");
        var task = getTestTask();
        var result = task.getResult();
        QName ocName = DummyHrScenario.Department.OBJECT_CLASS_NAME.xsd();
        var token = smartIntegrationService.submitSuggestObjectTypesOperation(RESOURCE_DUMMY_HR.oid, ocName, task, result);

        when("waiting for the operation to finish successfully");
        var response = waitForFinish(
                () -> smartIntegrationService.getSuggestObjectTypesOperationStatus(token, task, result),
                TIMEOUT);

        then("there is the correct suggested object type");
        assertThat(response).isNotNull();
        var typeSuggestion = assertThat(response.getObjectType())
                .as("suggested object types collection")
                .hasSize(1)
                .element(0)
                .as("object type suggestion")
                .actual();
        assertThat(typeSuggestion.getKind()).isEqualTo(HR_DEPARTMENT.getKind());
        assertThat(typeSuggestion.getIntent()).isEqualTo(HR_DEPARTMENT.getIntent());
        var delineation = typeSuggestion.getDelineation();
        assertThat(delineation.getObjectClass()).isEqualTo(ocName);
        assertThat(delineation.getAuxiliaryObjectClass()).isEmpty();
        assertThat(delineation.getFilter()).isEmpty();
        assertThat(delineation.getBaseContext()).isNull();

        applyObjectTypeSuggestions(RESOURCE_DUMMY_HR, response.getObjectType(), task, result);
    }

    private void applyObjectTypeSuggestions(
            DummyTestResource resource, List<ResourceObjectTypeDefinitionType> suggestions, Task task, OperationResult result)
            throws CommonException {
        executeChanges(convertTypeSuggestionsToDelta(resource.oid, suggestions), null, task, result);
    }

    /** Assuming the types do not exist yet. */
    private ObjectDelta<ResourceType> convertTypeSuggestionsToDelta(
            String oid, List<ResourceObjectTypeDefinitionType> suggestions) throws SchemaException {
        return PrismContext.get().deltaFor(ResourceType.class)
                .item(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE)
                .addRealValues(CloneUtil.cloneCollectionMembers(suggestions))
                .asObjectDelta(oid);
    }

    /** Tests the "suggest focus type" method for both HR object types. */
    @Test
    public void test120SuggestFocusTypesForHr() throws CommonException {
        var task = getTestTask();
        var result = task.getResult();

        when("suggesting focus type for HR person object type");
        smartIntegrationService.setServiceClientSupplier(
                () -> new MockServiceClientImpl(
                        new SiSuggestFocusTypeResponseType()
                                .focusTypeName(UserType.COMPLEX_TYPE)));
        var focusTypeForPerson = smartIntegrationService
                .suggestFocusType(RESOURCE_DUMMY_HR.oid, HR_PERSON, task, result)
                .getFocusType();

        then("the focus type for person is correct");
        assertThat(focusTypeForPerson)
                .as("Focus type")
                .isEqualTo(UserType.COMPLEX_TYPE);

        when("suggesting focus type for HR department object type");
        smartIntegrationService.setServiceClientSupplier(
                () -> new MockServiceClientImpl(
                        new SiSuggestFocusTypeResponseType()
                                .focusTypeName(OrgType.COMPLEX_TYPE)));
        var focusTypeForDepartment = smartIntegrationService
                .suggestFocusType(RESOURCE_DUMMY_HR.oid, HR_DEPARTMENT, task, result)
                .getFocusType();

        then("the focus type for person is correct");
        assertThat(focusTypeForDepartment)
                .as("Focus type")
                .isEqualTo(OrgType.COMPLEX_TYPE);
    }
}
