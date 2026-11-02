/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.List;
import java.util.Set;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.web.page.admin.resources.ResourceTaskFlavors;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testng.annotations.Test;

import com.evolveum.midpoint.gui.test.TestMidPointSpringApplication;
import com.evolveum.midpoint.model.api.simulation.ProcessedObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.task.ActivityDefinitionBuilder;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/** Regression coverage for mapping simulation sampling filters. */
@ActiveProfiles("test")
@SpringBootTest(classes = TestMidPointSpringApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestMappingSimulationSampling extends AbstractGuiIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/mapping-simulation");
    private static final File USERS = new File(TEST_DIR, "users.xml");

    private static final String RESOURCE_OID = "9e776e33-90b0-4550-a367-46948b317e34";
    private static final String TASK_OID = "1c670eb2-e07f-477d-a2cd-b5377d281260";
    private static final String ATTR_DELINEATOR_NAME = "delineator";
    private static final String ATTR_CORRELATOR_NAME = "correlator";
    private static final String ATTR_GIVEN_NAME = "givenName";
    private static final String ATTR_FAMILY_NAME = "familyName";
    private static final String ATTR_EMPLOYEE_NUMBER_NAME = "employeeNumber";
    private static final QName ACCOUNT_OBJECT_CLASS = new QName(SchemaConstants.NS_RI, "AccountObjectClass");
    private static final QName ATTR_EMPLOYEE_NUMBER = new QName(SchemaConstants.NS_RI, ATTR_EMPLOYEE_NUMBER_NAME);
    private static final QName ATTR_CORRELATOR = new QName(ATTR_CORRELATOR_NAME);

    private static final Set<String> MARKING_USERS = Set.of("cena", "snow", "rambo", "connor");

    private ShadowSimpleAttributeDefinition<?> correlatorDefinition;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        DummyTestResource resource = new DummyTestResource(
                TEST_DIR, "dummy-resource.xml", RESOURCE_OID, "mapping-test",
                controller -> {
                    var accountObjectClass = controller.getDummyResource().getAccountObjectClass();
                    controller.addAttrDef(accountObjectClass, ATTR_DELINEATOR_NAME, String.class, false, false);
                    controller.addAttrDef(accountObjectClass, ATTR_CORRELATOR_NAME, String.class, false, false);
                    controller.addAttrDef(accountObjectClass, ATTR_GIVEN_NAME, String.class, false, false);
                    controller.addAttrDef(accountObjectClass, ATTR_FAMILY_NAME, String.class, false, false);
                    controller.addAttrDef(accountObjectClass, ATTR_EMPLOYEE_NUMBER_NAME, String.class, false, false);
                });
        resource.init(this, initTask, initResult);
        correlatorDefinition = ResourceSchemaFactory.getCompleteSchemaRequired(resource.getObjectable())
                .findObjectClassDefinitionRequired(ACCOUNT_OBJECT_CLASS)
                .findSimpleAttributeDefinitionRequired(ATTR_CORRELATOR_NAME);

        resource.addAccount("1")
                .addAttributeValue(ATTR_DELINEATOR_NAME, "")
                .addAttributeValue(ATTR_CORRELATOR_NAME, "smith1")
                .addAttributeValue(ATTR_GIVEN_NAME, "John")
                .addAttributeValue(ATTR_FAMILY_NAME, "Smith");
        resource.addAccount("2")
                .addAttributeValue(ATTR_DELINEATOR_NAME, "")
                .addAttributeValue(ATTR_CORRELATOR_NAME, "smith2")
                .addAttributeValue(ATTR_GIVEN_NAME, "Mark")
                .addAttributeValue(ATTR_FAMILY_NAME, "Smith");
        resource.addAccount("3")
                .addAttributeValue(ATTR_DELINEATOR_NAME, "")
                .addAttributeValue(ATTR_CORRELATOR_NAME, "cena")
                .addAttributeValue(ATTR_GIVEN_NAME, "John")
                .addAttributeValue(ATTR_FAMILY_NAME, "Cena");
        resource.addAccount("4")
                .addAttributeValue(ATTR_DELINEATOR_NAME, "")
                .addAttributeValue(ATTR_CORRELATOR_NAME, "snow")
                .addAttributeValue(ATTR_GIVEN_NAME, "John")
                .addAttributeValue(ATTR_FAMILY_NAME, "Snow")
                .addAttributeValue(ATTR_EMPLOYEE_NUMBER_NAME, "3456");
        resource.addAccount("5")
                .addAttributeValue(ATTR_DELINEATOR_NAME, "")
                .addAttributeValue(ATTR_CORRELATOR_NAME, "rambo")
                .addAttributeValue(ATTR_GIVEN_NAME, "John")
                .addAttributeValue(ATTR_FAMILY_NAME, "Rambo")
                .addAttributeValue(ATTR_EMPLOYEE_NUMBER_NAME, "8888");
        resource.addAccount("6")
                .addAttributeValue(ATTR_DELINEATOR_NAME, "")
                .addAttributeValue(ATTR_CORRELATOR_NAME, "connor")
                .addAttributeValue(ATTR_GIVEN_NAME, "John")
                .addAttributeValue(ATTR_FAMILY_NAME, "Connor")
                .addAttributeValue(ATTR_EMPLOYEE_NUMBER_NAME, "5678");

        repoAddObjectsFromFile(USERS, UserType.class, initResult);
    }

    @Test
    public void outboundSingleMatchingFocusIsTheOnlyUserProcessed() throws Exception {
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_NAME).eqPoly("smith1")
                .build();

        TestTask task = runOutboundSimulation(query, null);

        assertProcessedOutboundFocusNames(task, "smith1");
    }

    @Test
    public void outboundMultipleMatchingFocusesAreTheOnlyUsersProcessed() throws Exception {
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_ORGANIZATIONAL_UNIT).eqPoly("With projection")
                .build();

        TestTask task = runOutboundSimulation(query, null);

        assertProcessedOutboundFocusNames(task, "smith1", "smith2");
    }

    @Test
    public void outboundNoMatchingFocusMeansNoUserIsProcessed() throws Exception {
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_NAME).eqPoly("does-not-exist")
                .build();

        TestTask task = runOutboundSimulation(query, null);

        assertProcessedOutboundFocusNames(task);
    }

    @Test
    public void outboundSampleSizeLimitsTheMatchingFocusesThatAreProcessed() throws Exception {
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_ORGANIZATIONAL_UNIT).eqPoly("Marking")
                .build();

        TestTask task = runOutboundSimulation(query, 2);

        assertThat(getProcessedOutboundFocusNames(task))
                .hasSize(2)
                .doesNotHaveDuplicates()
                .allMatch(MARKING_USERS::contains);
    }

    @Test
    public void inboundSingleMatchingShadowIsTheOnlyAccountProcessed() throws Exception {
        ObjectQuery query = prismContext.queryFor(ShadowType.class)
                .itemWithDef(correlatorDefinition, ShadowType.F_ATTRIBUTES, correlatorDefinition.getItemName())
                .eq("smith1")
                .build();

        TestTask task = runInboundSimulation(query, null);

        assertProcessedInboundFocusNames(task, "smith1");
    }

    @Test
    public void inboundMultipleMatchingShadowsAreTheOnlyAccountsProcessed() throws Exception {
        ObjectQuery query = prismContext.queryFor(ShadowType.class)
                .itemWithDef(correlatorDefinition, ShadowType.F_ATTRIBUTES, correlatorDefinition.getItemName())
                .eq("smith1")
                .or().itemWithDef(correlatorDefinition, ShadowType.F_ATTRIBUTES, correlatorDefinition.getItemName())
                .eq("smith2")
                .build();

        TestTask task = runInboundSimulation(query, null);

        assertProcessedInboundFocusNames(task, "smith1", "smith2");
    }

    @Test
    public void inboundNoMatchingShadowMeansNoAccountIsProcessed() throws Exception {
        ObjectQuery query = prismContext.queryFor(ShadowType.class)
                .itemWithDef(
                        correlatorDefinition, ShadowType.F_ATTRIBUTES, correlatorDefinition.getItemName())
                .eq("does-not-exist")
                .build();

        TestTask task = runInboundSimulation(query, null);

        assertProcessedInboundFocusNames(task);
    }

    @Test
    public void inboundSampleSizeLimitsTheMatchingShadowsThatAreProcessed() throws Exception {
        ObjectQuery query = prismContext.queryFor(ShadowType.class)
                .itemWithDef(correlatorDefinition, ShadowType.F_ATTRIBUTES, correlatorDefinition.getItemName())
                .eq("cena")
                .or().itemWithDef(correlatorDefinition, ShadowType.F_ATTRIBUTES, correlatorDefinition.getItemName())
                .eq("snow")
                .or().itemWithDef(correlatorDefinition, ShadowType.F_ATTRIBUTES, correlatorDefinition.getItemName())
                .eq("rambo")
                .or().itemWithDef(correlatorDefinition, ShadowType.F_ATTRIBUTES, correlatorDefinition.getItemName())
                .eq("connor")
                .build();

        TestTask task = runInboundSimulation(query, 2);

        assertThat(getProcessedInboundFocusNames(task))
                .hasSize(2)
                .doesNotHaveDuplicates()
                .allMatch(MARKING_USERS::contains);
    }

    private TestTask runOutboundSimulation(ObjectQuery focusQuery, Integer sampleSize) throws Exception {
        if (sampleSize != null) {
            focusQuery.setPaging(prismContext.queryFactory().createPaging(0, sampleSize));
        }

        ResourceObjectSetType popupSelection = new ResourceObjectSetType()
                .resourceRef(RESOURCE_OID, ResourceType.COMPLEX_TYPE)
                .kind(ShadowKindType.ACCOUNT)
                .intent("default")
                .objectclass(ACCOUNT_OBJECT_CLASS)
                .query(prismContext.getQueryConverter().createQueryType(focusQuery));

        InlineOutboundMappingsDefinitionType mapping = new InlineOutboundMappingsDefinitionType()
                .ref(ItemPath.create(ATTR_EMPLOYEE_NUMBER).toBean())
                .outbound(new OutboundMappingType()
                        .name("Personal number to employee number")
                        .source(new VariableBindingDefinitionType()
                                .path(ItemPath.create(UserType.F_PERSONAL_NUMBER).toBean())));

        return runSimulation(ResourceTaskFlavors.OUTBOUND_MAPPING_PREVIEW_ACTIVITY.createWorkDefinitions(popupSelection, mapping));
    }

    private TestTask runInboundSimulation(ObjectQuery shadowQuery, Integer sampleSize) throws Exception {
        if (sampleSize != null) {
            shadowQuery.setPaging(prismContext.queryFactory().createPaging(0, sampleSize));
        }

        ResourceObjectSetType popupSelection = new ResourceObjectSetType()
                .resourceRef(RESOURCE_OID, ResourceType.COMPLEX_TYPE)
                .kind(ShadowKindType.ACCOUNT)
                .intent("default")
                .query(prismContext.getQueryConverter().createQueryType(shadowQuery));

        InlineInboundMappingsDefinitionType mapping = new InlineInboundMappingsDefinitionType()
                .ref(ItemPath.create(ATTR_EMPLOYEE_NUMBER).toBean())
                .inbound(new InboundMappingType()
                        .name("Employee number to personal number")
                        .target(new VariableBindingDefinitionType()
                                .path(ItemPath.create(UserType.F_PERSONAL_NUMBER).toBean())));

        return runSimulation(ResourceTaskFlavors.INBOUND_MAPPING_PREVIEW_ACTIVITY.createWorkDefinitions(popupSelection, mapping));
    }

    private TestTask runSimulation(WorkDefinitionsType workDefinitions) throws Exception {
        TaskType taskBean = new TaskType()
                .oid(TASK_OID)
                .name("Mapping simulation sampling")
                .taskIdentifier(TASK_OID)
                .ownerRef(SystemObjectsType.USER_ADMINISTRATOR.value(), UserType.COMPLEX_TYPE)
                .executionState(TaskExecutionStateType.CLOSED)
                .activity(ActivityDefinitionBuilder.create(workDefinitions)
                        .withExecutionMode(ExecutionModeType.PREVIEW)
                        .build());

        TestTask task = TestTask.of(taskBean);
        task.initWithOverwrite(this, getTestTask(), getTestOperationResult());
        task.rerun(getTestOperationResult());
        return task;
    }

    private void assertProcessedOutboundFocusNames(TestTask task, String... expectedNames) throws Exception {
        assertThat(getProcessedOutboundFocusNames(task))
                .containsExactlyInAnyOrder(expectedNames);
    }

    private List<String> getProcessedOutboundFocusNames(TestTask task) throws Exception {
        return getSuccessfulProcessedObjects(task, ShadowType.class).stream()
                .map(ProcessedObject::getBeforeOrAfterRequired)
                .map(ShadowType.class::cast)
                .map(shadow -> ShadowUtil.getSingleStringAttributeValue(shadow, ATTR_CORRELATOR))
                .toList();
    }

    private void assertProcessedInboundFocusNames(TestTask task, String... expectedNames) throws Exception {
        assertThat(getProcessedInboundFocusNames(task))
                .containsExactlyInAnyOrder(expectedNames);
    }

    private List<String> getProcessedInboundFocusNames(TestTask task) throws Exception {
        return getSuccessfulProcessedObjects(task, UserType.class).stream()
                .map(ProcessedObject::getBeforeOrAfterRequired)
                .map(UserType.class::cast)
                .map(user -> user.getName().getOrig())
                .toList();
    }

    private List<? extends ProcessedObject<?>> getSuccessfulProcessedObjects(TestTask task, Class<? extends ObjectType> expectedType) throws Exception {
        List<? extends ProcessedObject<?>> processedObjects = assertProcessedObjects(
                task.oid, "objects selected for mapping simulation")
                .getProcessedObjects().stream()
                .toList();
        assertThat(processedObjects)
                .allSatisfy(processedObject -> {
                    assertThat(processedObject.getType()).isEqualTo(expectedType);
                    assertThat(processedObject.getResultStatus()).isEqualTo(OperationResultStatus.SUCCESS);
                });
        return processedObjects;
    }
}
