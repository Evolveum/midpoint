package com.evolveum.midpoint.ninja;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.ninja.NinjaTestMixin;
import com.evolveum.midpoint.ninja.action.*;

public class VerifyFilesTest implements NinjaTestMixin {

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

        VerifyFilesOptions verifyFilesOptions = new VerifyFilesOptions();
        verifyFilesOptions.setFiles(List.of(new File("./src/test/resources/upgrade/objects")));
        verifyFilesOptions.setReportStyle(VerifyOptions.ReportStyle.CSV);

        List<Object> options = List.of(baseOptions, connectionOptions, verifyFilesOptions);

        executeAction(VerifyFilesAction.class, verifyFilesOptions, options);
    }
}
