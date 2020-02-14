/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.archetypes;

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.model.api.CollectionStats;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.model.test.asserter.ObjectCollectionViewAsserter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

/**
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class AbstractArchetypesTest extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/archetypes");

    public static final File SYSTEM_CONFIGURATION_ARCHETYPES_FILE = new File(TEST_DIR, "system-configuration-archetypes.xml");

    public static final String VIEW_ALL_EMPLOYEES_NAME = "all-employees";
    public static final String VIEW_ACTIVE_EMPLOYEES_IDENTIFIER = "active-employees";
    public static final String VIEW_BUSINESS_ROLES_IDENTIFIER = "business-roles-view";
    public static final String VIEW_BUSINESS_ROLES_LABEL = "Business";

    public static final File ARCHETYPE_TEST_FILE = new File(TEST_DIR, "archetype-test.xml");
    protected static final String ARCHETYPE_TEST_OID = "a8df34a8-f6f0-11e8-b98e-eb03652d943f";

    public static final File ARCHETYPE_BUSINESS_ROLE_FILE = new File(TEST_DIR, "archetype-business-role.xml");
    protected static final String ARCHETYPE_BUSINESS_ROLE_OID = "018e7340-199a-11e9-ad93-2b136d1c7ecf";
    private static final String ARCHETYPE_BUSINESS_ROLE_ICON_CSS_CLASS = "fe fe-business";
    private static final String ARCHETYPE_BUSINESS_ROLE_ICON_COLOR = "green";

    public static final File ROLE_EMPLOYEE_BASE_FILE = new File(TEST_DIR, "role-employee-base.xml");
    protected static final String ROLE_EMPLOYEE_BASE_OID = "e869d6c4-f6ef-11e8-b51f-df3e51bba129";

    public static final File ROLE_USER_ADMINISTRATOR_FILE = new File(TEST_DIR, "role-user-administrator.xml");
    protected static final String ROLE_USER_ADMINISTRATOR_OID = "6ae02e34-f8b0-11e8-9c40-87e142b606fe";

    public static final File ROLE_BUSINESS_CAPTAIN_FILE = new File(TEST_DIR, "role-business-captain.xml");
    protected static final String ROLE_BUSINESS_CAPTAIN_OID = "9f65f4b6-199b-11e9-a4c1-2f6b7eb1ebae";

    public static final File ROLE_BUSINESS_BOSUN_FILE = new File(TEST_DIR, "role-business-bosun.xml");
    protected static final String ROLE_BUSINESS_BOSUN_OID = "186b29c6-199c-11e9-9acc-3f8cc307573b";

    public static final File COLLECTION_ACTIVE_EMPLOYEES_FILE = new File(TEST_DIR, "collection-active-employees.xml");
    protected static final String COLLECTION_ACTIVE_EMPLOYEES_OID = "f61bcb4a-f8ae-11e8-9f5c-c3e7f27ee878";

    protected static final File COLLECTION_ACTIVE_USERS_FILE = new File(TEST_DIR, "collection-active-users.xml");
    protected static final String COLLECTION_ACTIVE_USERS_OID = "9276c3a6-5790-11e9-a931-efe1b34f25f6";

    protected static final String POLICY_SITUATION_TOO_MANY_INACTIVE_USERS = "http://foo.example.com/policy#tooManyInactiveUsers";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_ARCHETYPES_FILE;
    }

    protected ObjectCollectionViewAsserter<Void> assertObjectCollectionView(CompiledObjectCollectionView view) {
        ObjectCollectionViewAsserter<Void> asserter = new ObjectCollectionViewAsserter<>(view, null, "view");
        initializeAsserter(asserter);
        return asserter;
    }

    protected void assertPercentage(CollectionStats stats, Integer expectedIntPercentage) {
        Float actualPercentage = stats.computePercentage();
        assertFloat("Wrong percentage in stats", expectedIntPercentage, actualPercentage);
    }

    protected void assertPercentage(CollectionStats stats, Float expectedIntPercentage) {
        Float actualPercentage = stats.computePercentage();
        assertFloat("Wrong percentage in stats", expectedIntPercentage, actualPercentage);
    }
}
