/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.tools.testng.UnusedTestElement;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

/**
 * Created by Viliam Repan (lazyman).
 */
@UnusedTestElement("3 of 5 fails")
public class ImportRepositoryTest extends BaseTest {

    @BeforeMethod
    public void initMidpointHome() throws Exception {
        setupMidpointHome();
    }

    @Test
    public void test100ImportByOid() {
        String[] input = new String[]{"-m", getMidpointHome(), "import", "-o", "00000000-8888-6666-0000-100000000001",
                "-i", RESOURCES_FOLDER + "/org-monkey-island-simple.xml.zip", "-z"};

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

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
        String[] input = new String[]{"-m", getMidpointHome(), "import", "-f", "<equal><path>name</path><value>F0002</value></equal>",
                "-i", RESOURCES_FOLDER + "/org-monkey-island-simple.xml.zip", "-z"};

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

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
        String[] input = new String[]{"-m", getMidpointHome(), "import", "-f", "@src/test/resources/filter.xml",
                "-i", RESOURCES_FOLDER + "/org-monkey-island-simple.xml.zip", "-z"};

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

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
    public void test130ImportRaw() throws Exception {
        // todo implement
    }

    @Test
    public void test140ImportFromZipFileByFilterAllowOverwrite() throws Exception {
        // todo implement
    }
}
