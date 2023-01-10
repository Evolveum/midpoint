/*
 * Copyright (c) 2016-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import static org.testng.AssertJUnit.*;

import java.io.File;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.CsvResource;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMachineIntelligence extends AbstractStoryTest {

    @Autowired ApplicationContext applicationContext;

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "machineintelligence");

    private static final CsvResource RESOURCE_HR = new CsvResource(TEST_DIR, "resource-csv-HR.xml", "10000000-0000-0000-0000-000000000001", "HR.csv");

    private static final File SHADOW_RUR_FILE = new File(TEST_DIR, "shadow-rur.xml");
    private static final String SHADOW_RUR_OID = "77777700-0000-0000-0000-111111111111";

    private static final File SHADOW_CHAPPIE_FILE = new File(TEST_DIR, "shadow-chappie.xml");
    private static final String SHADOW_CHAPPIE_OID = "77777700-0000-0000-0000-111111111112";

    @Override
    protected File getSystemConfigurationFile() {
        return super.getSystemConfigurationFile();
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        RESOURCE_HR.initAndTest(this, initTask, initResult);
    }

    @Test
    public void test000Sanity() throws Exception {
        SystemConfigurationType systemConfiguration = getSystemConfiguration();
        assertNotNull("No system configuration", systemConfiguration);
        display("System config", systemConfiguration);
    }

    /**
     * WHEN: Create account in the HR, import this account to the midPoint
     *
     * THEN: User is imported to midPoint, new Organization is created,
     * user is assigned to the organization, assignment is active/inactive
     * according to the setting in the resource
     */
    @Test
    public void test010importActiveUserRUR() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        PrismObject<ShadowType> shadow = addObject(SHADOW_RUR_FILE, task, result);
        assertEquals(SHADOW_RUR_OID, shadow.getOid());

        //AND
        modelService.importFromResource(SHADOW_RUR_OID, task, result);

        //THEN

        //assert created organization
        SearchResultList<PrismObject<OrgType>> orgs = modelService.searchObjects(
                OrgType.class, prismContext.queryFor(OrgType.class).item(OrgType.F_NAME)
                        .eq("Universe").matching(PrismConstants.POLY_STRING_NORM_MATCHING_RULE_NAME).build(),
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
        Task task = getTestTask();
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
                        OrgType.class, prismContext.queryFor(OrgType.class).item(OrgType.F_NAME)
                                .eq("Earth").matching(PrismConstants.POLY_STRING_NORM_MATCHING_RULE_NAME).build(),
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

    private PrismObject<UserType> assertShadowOwner(
            String shadowOid, String userName, String userGivenName, String userFamilyName,
            String userFullName, Task task, OperationResult result) throws Exception {
        //noinspection unchecked
        PrismObject<UserType> userRur = (PrismObject<UserType>) modelService.searchShadowOwner(shadowOid, null, task, result);
        assertNotNull("Owner must not be null", userRur);

        UserType userType = userRur.asObjectable();
        assertEquals("Unexpected name in the user", PrismTestUtil.createPolyStringType(userName), userType.getName());
        assertEquals("Unexpected givenName in the user", PrismTestUtil.createPolyStringType(userGivenName), userType.getGivenName());
        assertEquals("Unexpected familyName in the user", PrismTestUtil.createPolyStringType(userFamilyName), userType.getFamilyName());
        assertEquals("Unexpected fullName in the user", PrismTestUtil.createPolyStringType(userFullName), userType.getFullName());

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
