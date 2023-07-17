package com.evolveum.midpoint.ninja;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.DeltaConversionOptions;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.ninja.action.VerifyResult;
import com.evolveum.midpoint.ninja.action.verify.VerificationReporter;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.validator.UpgradePriority;

@ContextConfiguration(locations = "classpath:ctx-ninja-test.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class VerifyUpgradeTest extends NinjaSpringTest {

    private static final File SRC_FILES = new File("../../infra/schema/src/test/resources/validator/processor");

    private static final File SRC_EXPECTED_FILES = new File("../../infra/schema/src/test/resources/validator/expected");

    private static final File TARGET_FILES = new File("./target/upgrade/objects");

    private static final File OUTPUT = new File("./target/verify.csv");

    private static final File OUTPUT_DELTA = new File("./target/verify.csv" + VerificationReporter.DELTA_FILE_NAME_SUFFIX);

    @BeforeTest
    public void beforeTest() throws Exception {
        FileUtils.deleteQuietly(TARGET_FILES);
        FileUtils.copyDirectory(SRC_FILES, TARGET_FILES);
    }

    @Test
    public void test100VerifyFiles() throws Exception {
        given();

        when();

        VerifyResult result = (VerifyResult) executeTest(NOOP_STREAM_VALIDATOR, EMPTY_STREAM_VALIDATOR,
                "-v",
                "-m", getMidpointHome(),
                "verify",
                "--file", TARGET_FILES.getPath(),
                "--report-style", "csv",
                "--overwrite",
                "--output", OUTPUT.getPath());

        then();

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getItemPriorityCount(UpgradePriority.OPTIONAL)).isEqualTo(1L);

        Assertions.assertThat(OUTPUT)
                .exists()
                .isNotEmpty();

        Assertions.assertThat(OUTPUT_DELTA)
                .exists()
                .isNotEmpty();

        // todo more asserts
    }

    // todo remove this mess, just testing ideas out
    @Test(enabled = false)
    public void test150Sample() throws Exception {
        SystemConfigurationType config = new SystemConfigurationType();
        config.setOid(UUID.randomUUID().toString());

        ObjectDelta delta1 = PrismTestUtil.getPrismContext().deltaFor(SystemConfigurationType.class)
                .item(ItemPath.create(
                        SystemConfigurationType.F_ADMIN_GUI_CONFIGURATION,
                        AdminGuiConfigurationType.F_ACCESS_REQUEST,
                        AccessRequestType.F_ROLE_CATALOG,
                        RoleCatalogType.F_COLLECTION))
                .add(new RoleCatalogType()
                        .beginCollection()
                        .identifier("sample")
                        .collectionIdentifier("sample")
                        .<RoleCollectionViewType>end())
                .asObjectDelta(config.getOid());

        ObjectDelta delta2 = PrismTestUtil.getPrismContext().deltaFor(SystemConfigurationType.class)
                .item(ItemPath.create(
                        SystemConfigurationType.F_ADMIN_GUI_CONFIGURATION,
                        AdminGuiConfigurationType.F_ACCESS_REQUEST,
                        AccessRequestType.F_ROLE_CATALOG,
                        RoleCatalogType.F_ROLE_CATALOG_REF))
                .add(new ObjectReferenceType()
                        .oid(UUID.randomUUID().toString())
                        .type(OrgType.COMPLEX_TYPE))
                .asObjectDelta(config.getOid());

        delta1.applyTo(config.asPrismObject());
        delta2.applyTo(config.asPrismObject());

        System.out.println(config);

        config = new SystemConfigurationType();
        config.setOid(UUID.randomUUID().toString());

        AdminGuiConfigurationType guiConfig = new AdminGuiConfigurationType();
        AccessRequestType request = new AccessRequestType();
        guiConfig.setAccessRequest(request);
        RoleCatalogType catalog = new RoleCatalogType();
        request.setRoleCatalog(catalog);
        catalog.getCollection().add(new RoleCollectionViewType()
                .identifier("sample")
                .collectionIdentifier("sample"));

        delta1 = PrismTestUtil.getPrismContext().deltaFor(SystemConfigurationType.class)
                .item(ItemPath.create(
                        SystemConfigurationType.F_ADMIN_GUI_CONFIGURATION))
                .add(guiConfig)
                .asObjectDelta(config.getOid());

        guiConfig = new AdminGuiConfigurationType();
        request = new AccessRequestType();
        guiConfig.setAccessRequest(request);
        catalog = new RoleCatalogType();
        request.setRoleCatalog(catalog);
        catalog.setRoleCatalogRef(new ObjectReferenceType()
                .oid(UUID.randomUUID().toString())
                .type(OrgType.COMPLEX_TYPE));

        delta2 = PrismTestUtil.getPrismContext().deltaFor(SystemConfigurationType.class)
                .item(ItemPath.create(
                        SystemConfigurationType.F_ADMIN_GUI_CONFIGURATION))
                .add(guiConfig)
                .asObjectDelta(config.getOid());

        delta1.applyTo(config.asPrismObject());
        delta2.applyTo(config.asPrismObject());

        System.out.println(config);
    }

    @Test
    public void test200UpgradeFiles() throws Exception {
        given();

        when();

        executeTest(NOOP_STREAM_VALIDATOR, EMPTY_STREAM_VALIDATOR,
                "-v",
                "-m", getMidpointHome(),
                "upgrade-objects",
                "--file", TARGET_FILES.getPath(),
                "--verification-file", OUTPUT.getPath(),
                "--upgrade-phase", "before");

        then();

        Collection<File> files = FileUtils.listFiles(TARGET_FILES, new String[] { "xml" }, true);
        for (File file : files) {
            File expected = new File(SRC_EXPECTED_FILES, file.getName());

            List<PrismObject<? extends Objectable>> objects = PrismTestUtil.parseObjectsCompat(file);
            List<PrismObject<? extends Objectable>> expectedObjects = PrismTestUtil.parseObjectsCompat(expected);

            Assertions.assertThat(objects).hasSize(expectedObjects.size());
            for (int i = 0; i < objects.size(); i++) {
                PrismObject object = objects.get(i);
                PrismObject expectedObject = expectedObjects.get(i);

                ObjectDelta delta = object.diff(expectedObject);

                Assertions.assertThat(object).is(new Condition<>(
                        o -> o.equivalent(expectedObject),
                        "Object %s (%s) doesn't look like expected one.\nExpected:\n%s\nActual:\n%s\nDifference:\n%s",
                        object,
                        file.getName(),
                        PrismTestUtil.serializeToXml(expectedObject.asObjectable()),
                        PrismTestUtil.serializeToXml(object.asObjectable()),
                        DeltaConvertor.serializeDelta(delta, DeltaConversionOptions.createSerializeReferenceNames(), "xml")));
            }
        }
    }

    @Test
    public void test300VerifyObjects() throws Exception {
        given();

        Collection<File> files = FileUtils.listFiles(TARGET_FILES, new String[] { "xml" }, true);
        for (File file : files) {
            List<PrismObject<? extends Objectable>> objects = PrismTestUtil.parseObjectsCompat(file);
            for (PrismObject<? extends Objectable> object : objects) {
                repository.addObject((PrismObject) object, RepoAddOptions.createOverwrite(), new OperationResult("add object"));
            }
        }

        when();

        VerifyResult result = (VerifyResult) executeTest(NOOP_STREAM_VALIDATOR, EMPTY_STREAM_VALIDATOR,
                "-v",
                "-m", getMidpointHome(),
                "verify",
                "--report-style", "csv",
                "--overwrite",
                "--output", OUTPUT.getPath());

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getItemPriorityCount(UpgradePriority.OPTIONAL)).isEqualTo(1L);

        Assertions.assertThat(OUTPUT)
                .exists()
                .isNotEmpty();

        Assertions.assertThat(OUTPUT_DELTA)
                .exists()
                .isNotEmpty();

        // todo more asserts
    }
}
