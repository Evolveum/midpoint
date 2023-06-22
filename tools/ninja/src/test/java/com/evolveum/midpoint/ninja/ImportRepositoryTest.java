/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja;

import java.io.File;
import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.ninja.impl.ActionStateListener;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ImportRepositoryTest extends BaseTest {

    private static final String PATH_MONKEY_ISLAND_SIMPLE_ZIP = "./target/org-monkey-island-simple.zip";

    @BeforeClass
    public void beforeClass() throws IOException {
        TestUtils.zipFile(new File("./src/test/resources/org-monkey-island-simple.xml"), new File(PATH_MONKEY_ISLAND_SIMPLE_ZIP));
    }

    @BeforeMethod
    public void initMidpointHome() throws Exception {
        setupMidpointHome();
    }

    @Test
    public void test100ImportByOid() throws Exception {
        String[] args = new String[] { "-m", getMidpointHome(), "import", "--oid", "00000000-8888-6666-0000-100000000001",
                "-i", PATH_MONKEY_ISLAND_SIMPLE_ZIP, "-z" };

        ActionStateListener listener = new ActionStateListener() {

            @Override
            public void onBeforeExecution(NinjaContext context) {
                clearDb(context);

                OperationResult result = new OperationResult("count objects");
                RepositoryService repository = context.getRepository();
                try {
                    int count = repository.countObjects(ObjectType.class, null, null, result);

                    Assertions.assertThat(count).isZero();
                } catch (Exception ex) {
                    Assertions.fail("Failed", ex);
                }
            }

            @Override
            public void onAfterExecution(NinjaContext context) {
                RepositoryService repository = context.getRepository();

                OperationResult result = new OperationResult("count");
                try {
                    int count = repository.countObjects(ObjectType.class, null, null, result);

                    Assertions.assertThat(count).isEqualTo(1);

                    count = repository.countObjects(OrgType.class, null, null, result);

                    Assertions.assertThat(count).isEqualTo(1);
                } catch (Exception ex) {
                    Assertions.fail("Failed", ex);
                }
            }
        };

        executeTest(
                args,
                out -> Assertions.assertThat(out.size()).isEqualTo(5),
                err -> Assertions.assertThat(err.size()).isZero(),
                listener);
    }

    @Test
    public void test110ImportByFilterAsOption() throws Exception {
        String[] args = new String[] { "-m", getMidpointHome(), "import", "-f", "<equal><path>name</path><value>F0002</value></equal>",
                "-i", PATH_MONKEY_ISLAND_SIMPLE_ZIP, "-z" };

        ActionStateListener listener = new ActionStateListener() {

            @Override
            public void onBeforeExecution(NinjaContext context) {
                clearDb(context);

                OperationResult result = new OperationResult("count objects");
                RepositoryService repository = context.getRepository();
                try {
                    int count = repository.countObjects(ObjectType.class, null, null, result);

                    Assertions.assertThat(count).isZero();
                } catch (Exception ex) {
                    Assertions.fail("Failed", ex);
                }
            }

            @Override
            public void onAfterExecution(NinjaContext context) {
                RepositoryService repository = context.getRepository();

                OperationResult result = new OperationResult("count objects");
                try {
                    int count = repository.countObjects(ObjectType.class, null, null, result);

                    Assertions.assertThat(count).isEqualTo(1);
                } catch (Exception ex) {
                    Assertions.fail("Failed", ex);
                }
            }
        };

        executeTest(
                args,
                out -> Assertions.assertThat(out.size()).isEqualTo(5),
                err -> Assertions.assertThat(err.size()).isZero(),
                listener);
    }

    @Test
    public void test120ImportByFilterAsFile() throws Exception {
        String[] args = new String[] { "-m", getMidpointHome(), "import", "-f", "@src/test/resources/filter.xml",
                "-i", PATH_MONKEY_ISLAND_SIMPLE_ZIP, "-z" };

        ActionStateListener listener = new ActionStateListener() {

            @Override
            public void onBeforeExecution(NinjaContext context) {
                clearDb(context);

                OperationResult result = new OperationResult("count objects");
                RepositoryService repository = context.getRepository();
                try {
                    int count = repository.countObjects(ObjectType.class, null, null, result);

                    Assertions.assertThat(count).isZero();
                } catch (Exception ex) {
                    Assertions.fail("Failed", ex);
                }
            }

            @Override
            public void onAfterExecution(NinjaContext context) {
                RepositoryService repository = context.getRepository();

                OperationResult result = new OperationResult("count users");
                try {
                    int count = repository.countObjects(ObjectType.class, null, null, result);

                    Assertions.assertThat(count).isEqualTo(1);
                } catch (Exception ex) {
                    Assertions.fail("Failed", ex);
                }
            }
        };

        executeTest(
                args,
                out -> Assertions.assertThat(out.size()).isEqualTo(5),
                err -> Assertions.assertThat(err.size()).isZero(),
                listener);
    }

    @Test
    public void testMid7668Failure() throws Exception {
        String[] args = new String[] { "-m", getMidpointHome(), "import", "-i", RESOURCES_DIRECTORY_PATH + "/mid-7668-roles.xml" };

        final String ROLE_1_OID = "daf12492-5387-470f-bbd8-cf21f609367c";
        final String ROLE_2_OID = "3ed7c747-ff1b-4b45-90c6-b158bc844e2b";

        ActionStateListener listener = new ActionStateListener() {

            @Override
            public void onBeforeExecution(NinjaContext context) {
                clearDb(context);
            }

            @Override
            public void onAfterExecution(NinjaContext context) {
                RepositoryService repository = context.getRepository();

                OperationResult result = new OperationResult("count objects");

                try {
                    PrismObject<RoleType> role = repository.getObject(RoleType.class, ROLE_1_OID, null, result);
                    Assertions.assertThat(role).isNotNull();
                } catch (Exception ex) {
                    Assertions.fail("Failed", ex);
                }

                try {
                    repository.getObject(RoleType.class, ROLE_2_OID, null, result);
                    Assertions.fail("This role should not get to repository because of default polystring normalizer (name collision failure)");
                } catch (ObjectNotFoundException ex) {
                    // ignored
                } catch (Exception ex) {
                    Assertions.fail("Failed", ex);
                }
            }
        };

        executeTest(args, null, null, listener);
    }

    @Test
    public void testMid7668CustomPolyStringNormalizer() throws Exception {
        String[] args = new String[] { "-m", getMidpointHome(), "--psn-class-name", "Ascii7PolyStringNormalizer", "import",
                "-i", RESOURCES_DIRECTORY_PATH + "/mid-7668-roles.xml" };

        final String ROLE_1_OID = "daf12492-5387-470f-bbd8-cf21f609367c";
        final String ROLE_2_OID = "3ed7c747-ff1b-4b45-90c6-b158bc844e2b";

        ActionStateListener listener = new ActionStateListener() {

            @Override
            public void onBeforeExecution(NinjaContext context) {
                clearDb(context);
            }

            @Override
            public void onAfterExecution(NinjaContext context) {
                RepositoryService repository = context.getRepository();

                OperationResult result = new OperationResult("count objects");
                try {
                    PrismObject<RoleType> role = repository.getObject(RoleType.class, ROLE_1_OID, null, result);
                    Assertions.assertThat(role).isNotNull();

                    role = repository.getObject(RoleType.class, ROLE_2_OID, null, result);
                    Assertions.assertThat(role).isNotNull();
                } catch (Exception ex) {
                    Assertions.fail("Failed", ex);
                }
            }
        };

        executeTest(args, null, null, listener);
    }
}
