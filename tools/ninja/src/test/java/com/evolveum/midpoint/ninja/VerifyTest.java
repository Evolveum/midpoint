package com.evolveum.midpoint.ninja;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.evolveum.midpoint.ninja.action.*;

import com.evolveum.midpoint.schema.validator.UpgradePriority;

import org.assertj.core.api.Assertions;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class VerifyTest implements NinjaTestMixin {

    @BeforeClass
    public void beforeClass() throws IOException {
        setupMidpointHome();
    }

    @Test
    public void verifyFilesTest() throws Exception {
        BaseOptions baseOptions = new BaseOptions();
        baseOptions.setVerbose(true);

        ConnectionOptions connectionOptions = new ConnectionOptions();
        connectionOptions.setMidpointHome(getMidpointHome());

        VerifyOptions verifyOptions = new VerifyOptions();
        verifyOptions.setFiles(List.of(new File("./src/test/resources/upgrade/objects")));
        verifyOptions.setReportStyle(VerifyOptions.ReportStyle.CSV);

        List<Object> options = List.of(baseOptions, connectionOptions, verifyOptions);

        VerifyResult result = executeAction(VerifyAction.class, verifyOptions, options);

        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result.getItemPriorityCount(UpgradePriority.OPTIONAL)).isEqualTo(2L);
    }

    @Test(enabled = false)
    public void verifyObjectsTest() {
        // todo implement
    }
}
