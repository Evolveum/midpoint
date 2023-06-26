package com.evolveum.midpoint.ninja;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.ninja.action.BaseOptions;
import com.evolveum.midpoint.ninja.action.ConnectionOptions;
import com.evolveum.midpoint.ninja.action.VerifyAction;
import com.evolveum.midpoint.ninja.action.VerifyOptions;

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

        executeAction(VerifyAction.class, verifyOptions, options);
    }

    @Test(enabled = false)
    public void verifyObjectsTest() {
        // todo implement
    }
}
