/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ImportRepositoryTest extends BaseTest {

    @BeforeMethod
    public void initMidpointHome() throws Exception {
        setupMidpointHome();
    }

    @Test
    public void test100ImportByOid() {
        String[] input = new String[] { "-m", getMidpointHome(), "import", "-o", "00000000-8888-6666-0000-100000000001",
                "-i", RESOURCES_FOLDER + "/org-monkey-island-simple.xml.zip", "-z" };

        executeTest(null,
                context -> {
                    RepositoryService repo = context.getRepository();

                    OperationResult result = new OperationResult("count objects");
                    int count = repo.countObjects(ObjectType.class, null, null, result);

                    AssertJUnit.assertEquals(0, count);
                },
                context -> {
                    RepositoryService repo = context.getRepository();

                    OperationResult result = new OperationResult("count");
                    int count = repo.countObjects(ObjectType.class, null, null, result);

                    AssertJUnit.assertEquals(1, count);

                    count = repo.countObjects(OrgType.class, null, null, result);

                    AssertJUnit.assertEquals(1, count);
                },
                true, true, input);

        List<String> out = getSystemOut();
        AssertJUnit.assertEquals(out.toString(), 5, out.size());
        AssertJUnit.assertTrue(getSystemErr().isEmpty());
    }

    @Test
    public void test110ImportByFilterAsOption() throws Exception {
        String[] input = new String[] { "-m", getMidpointHome(), "import", "-f", "<equal><path>name</path><value>F0002</value></equal>",
                "-i", RESOURCES_FOLDER + "/org-monkey-island-simple.xml.zip", "-z" };

        executeTest(null,
                context -> {
                    RepositoryService repo = context.getRepository();

                    OperationResult result = new OperationResult("count objects");
                    int count = repo.countObjects(ObjectType.class, null, null, result);

                    AssertJUnit.assertEquals(0, count);
                },
                context -> {
                    RepositoryService repo = context.getRepository();

                    OperationResult result = new OperationResult("count objects");
                    int count = repo.countObjects(ObjectType.class, null, null, result);

                    AssertJUnit.assertEquals(1, count);
                },
                true, true, input);

        List<String> out = getSystemOut();
        AssertJUnit.assertEquals(out.toString(), 5, out.size());
        AssertJUnit.assertTrue(getSystemErr().isEmpty());
    }

    @Test
    public void test120ImportByFilterAsFile() throws Exception {
        String[] input = new String[] { "-m", getMidpointHome(), "import", "-f", "@src/test/resources/filter.xml",
                "-i", RESOURCES_FOLDER + "/org-monkey-island-simple.xml.zip", "-z" };

        executeTest(null,
                context -> {
                    RepositoryService repo = context.getRepository();

                    OperationResult result = new OperationResult("count objects");
                    int count = repo.countObjects(ObjectType.class, null, null, result);

                    AssertJUnit.assertEquals(0, count);
                },
                context -> {
                    RepositoryService repo = context.getRepository();

                    OperationResult result = new OperationResult("count users");
                    int count = repo.countObjects(ObjectType.class, null, null, result);

                    AssertJUnit.assertEquals(1, count);
                },
                true, true, input);

        List<String> out = getSystemOut();
        AssertJUnit.assertEquals(out.toString(), 5, out.size());
        AssertJUnit.assertTrue(getSystemErr().isEmpty());
    }

    @Test
    public void testMid7668Failure() {
        String[] input = new String[] { "-m", getMidpointHome(), "import",
                "-i", RESOURCES_FOLDER + "/mid-7668-roles.xml" };

        final String ROLE_1_OID = "daf12492-5387-470f-bbd8-cf21f609367c";
        final String ROLE_2_OID = "3ed7c747-ff1b-4b45-90c6-b158bc844e2b";

        executeTest(null,
                context -> {
                },
                context -> {
                    RepositoryService repo = context.getRepository();

                    OperationResult result = new OperationResult("count objects");
                    PrismObject<RoleType> role = repo.getObject(RoleType.class, ROLE_1_OID, null, result);
                    AssertJUnit.assertNotNull(role);

                    try {
                        repo.getObject(RoleType.class, ROLE_2_OID, null, result);
                        AssertJUnit.fail("This role should get to repository because of default polystring normalizer (name collision failure)");
                    } catch (ObjectNotFoundException ex) {
                    }
                },
                true, true, input);
    }

    @Test
    public void testMid7668CustomPolyStringNormalizer() {
        String[] input = new String[] { "-m", getMidpointHome(), "--psn-class-name", "Ascii7PolyStringNormalizer", "import",
                "-i", RESOURCES_FOLDER + "/mid-7668-roles.xml" };

        final String ROLE_1_OID = "daf12492-5387-470f-bbd8-cf21f609367c";
        final String ROLE_2_OID = "3ed7c747-ff1b-4b45-90c6-b158bc844e2b";

        executeTest(null,
                context -> {
                },
                context -> {
                    RepositoryService repo = context.getRepository();

                    OperationResult result = new OperationResult("count objects");
                    PrismObject<RoleType> role = repo.getObject(RoleType.class, ROLE_1_OID, null, result);
                    AssertJUnit.assertNotNull(role);

                    role = repo.getObject(RoleType.class, ROLE_2_OID, null, result);
                    AssertJUnit.assertNotNull(role);
                },
                true, true, input);
    }
}
