package com.evolveum.midpoint.ninja;

import java.io.File;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.repo.api.RepoAddOptions;

import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.ninja.action.VerifyResult;
import com.evolveum.midpoint.ninja.action.verify.VerificationReporter;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.validator.UpgradePriority;

@ContextConfiguration(locations = "classpath:ctx-ninja-test.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class VerifyTest extends NinjaSpringTest {

    private static final File FILES = new File("./src/test/resources/upgrade/objects");

    private static final File OUTPUT = new File("./target/verify.csv");

    private static final File OUTPUT_DELTA = new File("./target/verify.csv" + VerificationReporter.DELTA_FILE_NAME_SUFFIX);

    @BeforeTest
    public void beforeTest() {
        FileUtils.deleteQuietly(OUTPUT);
        FileUtils.deleteQuietly(OUTPUT_DELTA);
    }

    @Test
    public void test100VerifyFiles() throws Exception {
        given();

        when();

        VerifyResult result = (VerifyResult) executeTest(NOOP_STREAM_VALIDATOR, EMPTY_STREAM_VALIDATOR,
                "-v",
                "-m", getMidpointHome(),
                "verify",
                "--files", FILES.getPath(),
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

    @Test
    public void test200VerifyObjects() throws Exception {
        given();

        Collection<File> files = FileUtils.listFiles(FILES, new String[] { "xml" }, true);
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
