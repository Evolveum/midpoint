/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja;

import java.io.File;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

/**
 * Created by Viliam Repan (lazyman).
 */
@ContextConfiguration(locations = "classpath:ctx-ninja-test.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class ImportRepositoryTest extends NinjaSpringTest {

    private static final File FILE_MONKEY_ISLAND_SIMPLE = new File("./src/test/resources/org-monkey-island-simple.xml");

    private static final File FILE_MONKEY_ISLAND_SIMPLE_ZIP = new File("./target/org-monkey-island-simple.zip");

    @BeforeClass(
            dependsOnMethods = { "springTestContextPrepareTestInstance" }
    )
    @Override
    public void beforeClass() throws Exception {
        TestUtils.zipFile(FILE_MONKEY_ISLAND_SIMPLE, FILE_MONKEY_ISLAND_SIMPLE_ZIP);

        super.beforeClass();
    }

    @Test
    public void test100ImportByOid() throws Exception {
        given();

        OperationResult result = new OperationResult("test100ImportByOid");

        int count = repository.countObjects(OrgType.class, null, null, result);
        Assertions.assertThat(count).isZero();

        when();

        executeTest(
                out -> Assertions.assertThat(out.size()).isEqualTo(8),
                EMPTY_STREAM_VALIDATOR,
                "-m", getMidpointHome(),
                "import",
                "--oid", "00000000-8888-6666-0000-100000000001",
                "-i", FILE_MONKEY_ISLAND_SIMPLE_ZIP.getPath(),
                "-z");

        then();

        count = repository.countObjects(OrgType.class, null, null, result);
        Assertions.assertThat(count).isEqualTo(1);
    }

    @Test
    public void test110ImportByFilterAsOption() throws Exception {
        given();

        OperationResult result = new OperationResult("test110ImportByFilterAsOption");

        int count = repository.countObjects(OrgType.class, null, null, result);
        Assertions.assertThat(count).isEqualTo(1);

        when();

        executeTest(
                out -> Assertions.assertThat(out.size()).isEqualTo(8),
                EMPTY_STREAM_VALIDATOR,
                "-m", getMidpointHome(),
                "import",
                "-f", "<equal><path>name</path><value>F0002</value></equal>",
                "-i", FILE_MONKEY_ISLAND_SIMPLE_ZIP.getPath(),
                "-z");

        then();

        count = repository.countObjects(OrgType.class, null, null, result);
        Assertions.assertThat(count).isEqualTo(2);
    }

    @Test
    public void test120ImportByFilterAsFile() throws Exception {
        given();

        OperationResult result = new OperationResult("test120ImportByFilterAsFile");

        int count = repository.countObjects(OrgType.class, null, null, result);
        Assertions.assertThat(count).isEqualTo(2);

        when();

        executeTest(
                out -> Assertions.assertThat(out.size()).isEqualTo(8),
                EMPTY_STREAM_VALIDATOR,
                "-m", getMidpointHome(),
                "import",
                "-f", "@src/test/resources/filter.xml",
                "-i", FILE_MONKEY_ISLAND_SIMPLE_ZIP.getPath(),
                "-z",
                "-O");

        then();

        count = repository.countObjects(OrgType.class, null, null, result);
        // count has not changed, since one object was imported with override
        Assertions.assertThat(count).isEqualTo(2);
    }

    @Test
    public void test130MID7668Failure() throws Exception {
        given();

        when();

        final String ROLE_1_OID = "daf12492-5387-470f-bbd8-cf21f609367c";
        final String ROLE_2_OID = "3ed7c747-ff1b-4b45-90c6-b158bc844e2b";

        executeTest(
                null,
                null,
                "-m", getMidpointHome(),
                "import",
                "-i", RESOURCES_DIRECTORY.getPath() + "/mid-7668-roles.xml");

        then();

        OperationResult result = new OperationResult("testMid7668Failure");

        PrismObject<RoleType> role = repository.getObject(RoleType.class, ROLE_1_OID, null, result);
        Assertions.assertThat(role).isNotNull();

        try {
            repository.getObject(RoleType.class, ROLE_2_OID, null, result);
            Assertions.fail("This role should not get to repository because of default polystring normalizer (name collision failure)");
        } catch (ObjectNotFoundException ex) {
            // ignored
        }
    }

    @Test
    public void test140MID7668CustomPolyStringNormalizer() throws Exception {
        given();

        when();

        final String ROLE_1_OID = "daf12492-5387-470f-bbd8-cf21f609367c";
        final String ROLE_2_OID = "3ed7c747-ff1b-4b45-90c6-b158bc844e2b";

        executeTest(
                null,
                null,
                "-m", getMidpointHome(),
                "--psn-class-name", "Ascii7PolyStringNormalizer",
                "import",
                "-i", RESOURCES_DIRECTORY.getPath() + "/mid-7668-roles.xml");

        then();

        OperationResult result = new OperationResult("testMid7668CustomPolyStringNormalizer");

        PrismObject<RoleType> role = repository.getObject(RoleType.class, ROLE_1_OID, null, result);
        Assertions.assertThat(role).isNotNull();

        role = repository.getObject(RoleType.class, ROLE_2_OID, null, result);
        Assertions.assertThat(role).isNotNull();
    }

    private void deleteAllRepositoryObjects() throws SchemaException {
        SearchResultList<PrismObject<ObjectType>> objects =
                repository.searchObjects(ObjectType.class, null, null, new OperationResult("search"));

        objects.forEach(o -> {
            try {
                repository.deleteObject(o.asObjectable().getClass(), o.getOid(), new OperationResult("delete"));
            } catch (Exception ex) {
                AssertJUnit.fail("Couldn't delete object " + o + ", reason: " + ex.getMessage());
            }
        });

        long countAfterDelete = repository.countObjects(ObjectType.class, null, null, new OperationResult("count"));
        Assertions.assertThat(countAfterDelete).isZero();
    }

    @Test
    public void test150ExportImportZip() throws Exception {
        File file = new File("target/test150ExportImportZip.zip");

        deleteAllRepositoryObjects();

        List<PrismObject<? extends Objectable>> objects = PrismTestUtil.parseObjects(FILE_MONKEY_ISLAND_SIMPLE);
        for (PrismObject<? extends Objectable> object : objects) {
            repository.addObject(object.asObjectable().asPrismObject(), null, new OperationResult("add"));
        }

        long count = repository.countObjects(ObjectType.class, null, null, new OperationResult("count"));
        Assertions.assertThat(count).isGreaterThan(0);

        executeTest(
                null,
                null,
                "-m", getMidpointHome(),
                "export",
                "-o", file.getPath(),
                "-O",
                "-z",
                "-r",
                "-l", "4");

        Assertions.assertThat(file)
                .exists()
                .size().isNotZero();

        executeTest(
                null,
                null,
                "-m", getMidpointHome(),
                "delete");

        deleteAllRepositoryObjects();

        executeTest(
                null,
                null,
                "-m", getMidpointHome(),
                "import",
                "-i", file.getPath(),
                "-r",
                "-z",
                "-l", "4");

        long countAfterImport = repository.countObjects(ObjectType.class, null, null, new OperationResult("count"));
        Assertions.assertThat(countAfterImport).isEqualTo(count);
    }
}
