/*
 * Copyright (c) 2016-2017 Evolveum
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
package com.evolveum.midpoint.testing.story;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.match.PolyStringNormMatchingRule;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.ClassPathUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMachineIntelligence extends AbstractStoryTest {

	public static Trace LOGGER = TraceManager.getTrace(TestMachineIntelligence.class);

	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "machineintelligence");

	private static final File RESOURCE_HR_FILE = new File(TEST_DIR, "resource-csv-HR.xml");
	private static final String RESOURCE_HR_OID = "10000000-0000-0000-0000-000000000001";

	private static final File RESOURCE_CSV_CONTENT_FILE = new File(TEST_DIR, "HR.csv");
	private static String sourceFilePath;

	private static final File SHADOW_RUR_FILE = new File(TEST_DIR, "shadow-rur.xml");
	private static final String SHADOW_RUR_OID = "shadow00-0000-0000-0000-111111111111";

	private static final File SHADOW_CHAPPIE_FILE = new File(TEST_DIR, "shadow-chappie.xml");
	private static final String SHADOW_CHAPPIE_OID = "shadow00-0000-0000-0000-111111111112";

	private static final String NS_RESOURCE_CSV = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/bundle/com.evolveum.polygon.connector-csv/com.evolveum.polygon.connector.csv.CsvConnector";

	@Autowired
	MidpointConfiguration midPointConfig;

	@Override
	protected File getSystemConfigurationFile() {
		return super.getSystemConfigurationFile();
	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		String home = midPointConfig.getMidpointHome();
		File resourceDir = new File(home, "machineintelligence");
		resourceDir.mkdir();

		LOGGER.info("Start copying HR.csv from {} to {}", RESOURCE_CSV_CONTENT_FILE.getAbsolutePath(), resourceDir.getAbsolutePath());
		File desticationFile = new File(resourceDir, "HR.csv");
		ClassPathUtil.copyFile(new FileInputStream(RESOURCE_CSV_CONTENT_FILE), "HR.csv", desticationFile);

		if (!desticationFile.exists()){
			throw new SystemException("Source file for HR resource was not created");
		}

		sourceFilePath = desticationFile.getAbsolutePath();

		super.initSystem(initTask, initResult);

		importObjectFromFile(RESOURCE_HR_FILE);

	}

	@Test
    public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
        displayTestTitle(TEST_NAME);
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
		Object[] newRealValue = { sourceFilePath };

        ObjectDelta<ResourceType> objectDelta = ObjectDelta.createModificationReplaceProperty(ResourceType.class, RESOURCE_HR_OID, new ItemPath(ResourceType.F_CONNECTOR_CONFIGURATION,
						SchemaConstants.ICF_CONFIGURATION_PROPERTIES, new QName(NS_RESOURCE_CSV, "filePath")), prismContext, newRealValue);
        provisioningService.applyDefinition(objectDelta, task, result);
        provisioningService.modifyObject(ResourceType.class, objectDelta.getOid(), objectDelta.getModifications(), null, null, task, result);

        OperationResult hrTestResult = modelService.testResource(RESOURCE_HR_OID, task);
        TestUtil.assertSuccess("HR resource test result", hrTestResult);

        OperationResult testResultOpenDj = modelService.testResource(RESOURCE_HR_OID, task);
        TestUtil.assertSuccess("OpenDJ resource test result", testResultOpenDj);

        SystemConfigurationType systemConfiguration = getSystemConfiguration();
        assertNotNull("No system configuration", systemConfiguration);
        display("System config", systemConfiguration);

	}

	/**
	 *
	 * WHEN: Create account in the HR, import this acount to the midPoint
	 * THEN: User is imported to midPoint, new Organization is created,
	 * 		user is assigned to the organization, assignment is active/inactive
	 * 		according to the setting in the resource
	 */
	@Test
    public void test010importActiveUserRUR() throws Exception {
		final String TEST_NAME = "test010importActiveUserRUR";
        TestUtil.displayTestTitle(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        PrismObject<ShadowType> shadow = addObject(SHADOW_RUR_FILE, task, result);
        assertEquals(SHADOW_RUR_OID, shadow.getOid());

        //AND
        modelService.importFromResource(SHADOW_RUR_OID, task, result);

        //THEN

        //assert created organization
		SearchResultList<PrismObject<OrgType>> orgs = modelService.searchObjects(
				OrgType.class, QueryBuilder.queryFor(OrgType.class, prismContext).item(OrgType.F_NAME)
						.eq("Universe").matching(PolyStringNormMatchingRule.NAME).build(),
				null, task, result);
        assertEquals("Found unexpected number of organizations, expected 1, found " + orgs.size(), 1, orgs.size());

        //assert created owner of shadow
		PrismObject<UserType> userRur = assertShadowOwner(SHADOW_RUR_OID, "R.U.R", "Rossum",
				"Universal Robots", "Rossum's Universal Robots", task, result);
        //assert assignment of org
        assertAssignedOrg(userRur, orgs.iterator().next());
        //assert assignment of or in more depth
        assertAssignment(userRur.asObjectable(), ActivationStatusType.ENABLED, null);

	}

	@Test
    public void test011importInactiveUserChappie() throws Exception {
		final String TEST_NAME = "test020ResourceOpenDjGet";
        TestUtil.displayTestTitle(this, TEST_NAME);
        Task task = taskManager.createTaskInstance(TestTrafo.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        PrismObject<ShadowType> shadow = addObject(SHADOW_CHAPPIE_FILE, task, result);
        assertEquals(SHADOW_CHAPPIE_OID, shadow.getOid());

        //AND
        modelService.importFromResource(SHADOW_CHAPPIE_OID, task, result);

        //THEN

        //assert created organization
		SearchResultList<PrismObject<OrgType>> orgs = modelService
				.searchObjects(
						OrgType.class, QueryBuilder.queryFor(OrgType.class, prismContext).item(OrgType.F_NAME)
								.eq("Earth").matching(PolyStringNormMatchingRule.NAME).build(),
						null, task, result);
        assertEquals("Found unexpected number of organizations, expected 1, found " + orgs.size(), 1, orgs.size());

        //assert created owner of shadow
		PrismObject<UserType> userRur = assertShadowOwner(SHADOW_CHAPPIE_OID, "chappie", "Chappie",
				"von Tetravaal", "Chappie von Tetravaal", task, result);
        //assert assignment of org
        assertAssignedOrg(userRur, orgs.iterator().next());
        //assert assignment of or in more depth

        XMLGregorianCalendar validTo = XmlTypeConverter
				.createXMLGregorianCalendar(2016, 12, 31, 23, 59, 59);
        assertAssignment(userRur.asObjectable(), ActivationStatusType.DISABLED, validTo);

	}

	private PrismObject<UserType> assertShadowOwner(String shadowOid, String userName, String userGivenName, String userFamilyName, String userFullName, Task task, OperationResult result) throws Exception{
		PrismObject<UserType> userRur = (PrismObject<UserType>) modelService.searchShadowOwner(shadowOid, null, task, result);
        assertNotNull("Owner must not be null", userRur);

        UserType userType = userRur.asObjectable();
        assertEquals("Unexpected name in the user", PrismTestUtil.createPolyStringType(userName) , userType.getName());
        assertEquals("Unexpected givenName in the user", PrismTestUtil.createPolyStringType(userGivenName) , userType.getGivenName());
        assertEquals("Unexpected familyName in the user", PrismTestUtil.createPolyStringType(userFamilyName) , userType.getFamilyName());
        assertEquals("Unexpected fullName in the user", PrismTestUtil.createPolyStringType(userFullName) , userType.getFullName());

        return userRur;
	}

	private void assertAssignment(UserType userType, ActivationStatusType administrativeStatus, XMLGregorianCalendar validTo) {
		List<AssignmentType> assignments = userType.getAssignment();
        assertEquals("Unexpected assignment in user, expected 1, found " + assignments.size(), 1, assignments.size());
        AssignmentType assignmentType = assignments.iterator().next();
        ActivationType assignmentActivation = assignmentType.getActivation();
        assertEquals(administrativeStatus, assignmentActivation.getAdministrativeStatus());
        assertEquals(administrativeStatus, assignmentActivation.getEffectiveStatus());
        if (validTo == null) {
        	assertNull("Unexpected validTo value: " + assignmentActivation.getValidTo(), assignmentActivation.getValidTo());
        } else {
        	//TODO:
        }
	}

}
