/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.archetypes;

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

    private static final File SYSTEM_CONFIGURATION_ARCHETYPES_FILE = new File(TEST_DIR, "system-configuration-archetypes.xml");

    static final File COLLECTION_ACTIVE_USERS_FILE = new File(TEST_DIR, "collection-active-users.xml");
    static final String COLLECTION_ACTIVE_USERS_OID = "9276c3a6-5790-11e9-a931-efe1b34f25f6";

    static final String POLICY_SITUATION_TOO_MANY_INACTIVE_USERS = "http://foo.example.com/policy#tooManyInactiveUsers";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_ARCHETYPES_FILE;
    }

    ObjectCollectionViewAsserter<Void> assertObjectCollectionView(CompiledObjectCollectionView view) {
        ObjectCollectionViewAsserter<Void> asserter = new ObjectCollectionViewAsserter<>(view, null, "view");
        initializeAsserter(asserter);
        return asserter;
    }

    @SuppressWarnings("SameParameterValue")
    void assertPercentage(CollectionStats stats, Integer expectedIntPercentage) {
        Float actualPercentage = stats.computePercentage();
        assertFloat("Wrong percentage in stats", expectedIntPercentage, actualPercentage);
    }

    void assertPercentage(CollectionStats stats, Float expectedIntPercentage) {
        Float actualPercentage = stats.computePercentage();
        assertFloat("Wrong percentage in stats", expectedIntPercentage, actualPercentage);
    }
}
