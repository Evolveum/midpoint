package com.evolveum.midpoint.ninja.upgrade;

import java.io.File;
import java.util.List;

import org.testng.annotations.Test;

import com.evolveum.midpoint.ninja.NinjaTestMixin;
import com.evolveum.midpoint.ninja.action.*;

public class VerifyFilesTest implements NinjaTestMixin {

    protected static final File UPGRADE_MIDPOINT_HOME = new File("./target/midpoint-home-upgrade");

    @Test
    public void verifyFilesTest() throws Exception {
        BaseOptions baseOptions = new BaseOptions();
        baseOptions.setVerbose(true);

        ConnectionOptions connectionOptions = new ConnectionOptions();
        connectionOptions.setMidpointHome(UPGRADE_MIDPOINT_HOME.getPath());

        VerifyFilesOptions verifyFilesOptions = new VerifyFilesOptions();
        verifyFilesOptions.setFiles(List.of(new File("./src/test/resources/upgrade/objects")));
        verifyFilesOptions.setReportStyle(VerifyOptions.ReportStyle.CSV);

        List<Object> options = List.of(baseOptions, connectionOptions, verifyFilesOptions);

        executeAction(VerifyFilesAction.class, verifyFilesOptions, options);
    }
}
