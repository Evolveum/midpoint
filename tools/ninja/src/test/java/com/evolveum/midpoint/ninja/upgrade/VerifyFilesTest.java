package com.evolveum.midpoint.ninja.upgrade;

import java.io.File;
import java.util.List;

import org.testng.annotations.Test;

import com.evolveum.midpoint.ninja.action.BaseOptions;
import com.evolveum.midpoint.ninja.action.ConnectionOptions;
import com.evolveum.midpoint.ninja.action.VerifyFilesAction;
import com.evolveum.midpoint.ninja.action.VerifyFilesOptions;
import com.evolveum.midpoint.ninja.impl.NinjaContext;

public class VerifyFilesTest {

    protected static final File UPGRADE_MIDPOINT_HOME = new File("./target/midpoint-home-upgrade");

    @Test
    public void verifyFilesTest() throws Exception {
        BaseOptions baseOptions = new BaseOptions();
        baseOptions.setVerbose(true);

        ConnectionOptions connectionOptions = new ConnectionOptions();
        connectionOptions.setMidpointHome(UPGRADE_MIDPOINT_HOME.getPath());
        connectionOptions.setOffline(true);

        VerifyFilesOptions verifyFilesOptions = new VerifyFilesOptions();
        verifyFilesOptions.setFiles(List.of(new File("./src/test/resources/upgrade/objects")));
        verifyFilesOptions.setReport(new File("./target/verify-files.csv"));

        try (NinjaContext context = new NinjaContext(List.of(baseOptions, connectionOptions, verifyFilesOptions))) {

            VerifyFilesAction action = new VerifyFilesAction();
            action.init(context, verifyFilesOptions);

            action.execute();
        }
    }
}
