/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.JCommander;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.AssertJUnit;

import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.opts.ConnectionOptions;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;

/**
 * Created by Viliam Repan (lazyman).
 */
public class BaseTest extends AbstractUnitTest {

    private static final Logger LOG = LoggerFactory.getLogger(BaseTest.class);

    private static final File TARGET_HOME = new File("./target/home");

    public static final String RESOURCES_FOLDER = "./target/test-classes";

    private List<String> systemOut;
    private List<String> systemErr;

    // add @BeforeMethod calling this into test classes that need this
    protected void setupMidpointHome() throws IOException {
        FileUtils.deleteDirectory(TARGET_HOME);

        File baseHome = new File("./src/test/resources/midpoint-home");

        FileUtils.copyDirectory(baseHome, TARGET_HOME);
    }

    protected String getMidpointHome() {
        return TARGET_HOME.getAbsolutePath();
    }

    protected NinjaContext setupNinjaContext(String[] input) {
        JCommander jc = NinjaUtils.setupCommandLineParser();
        jc.parse(input);

        NinjaContext context = new NinjaContext(jc);

        ConnectionOptions connection = NinjaUtils.getOptions(jc, ConnectionOptions.class);
        context.init(connection);

        return context;
    }

    protected void executeTest(String... args) {
        executeTest(null, null, args);
    }

    protected void executeTest(ExecutionValidator preExecutionValidator,
            ExecutionValidator postExecutionValidator, String... args) {
        executeTest(null, preExecutionValidator, postExecutionValidator, false, false, args);
    }

    protected void executeTest(ExecutionValidator preInit,
            ExecutionValidator preExecution,
            ExecutionValidator postExecution,
            boolean saveOut, boolean saveErr, String... args) {

        systemOut = new ArrayList<>();
        systemErr = new ArrayList<>();

        ByteArrayOutputStream bosOut = new ByteArrayOutputStream();
        ByteArrayOutputStream bosErr = new ByteArrayOutputStream();

        if (saveOut) {
            System.setOut(new PrintStream(bosOut));
        }

        if (saveErr) {
            System.setErr(new PrintStream(bosErr));
        }

        try {
            Main main = new Main() {

                @Override
                protected void preInit(NinjaContext context) {
                    validate(preInit, context, "pre init");
                }

                @Override
                protected void preExecute(NinjaContext context) {
                    validate(preExecution, context, "pre execution");
                }

                @Override
                protected void postExecute(NinjaContext context) {
                    validate(postExecution, context, "post execution");
                }
            };

            main.run(args);
        } finally {
            try {
                if (saveOut) {
                    System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
                    systemOut = IOUtils.readLines(new ByteArrayInputStream(bosOut.toByteArray()), StandardCharsets.UTF_8);
                }

                if (saveErr) {
                    System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));
                    systemErr = IOUtils.readLines(new ByteArrayInputStream(bosErr.toByteArray()), StandardCharsets.UTF_8);
                }
            } catch (IOException ex) {
            }
            systemOut.forEach(s -> System.out.println(s));
            systemErr.forEach(s -> System.err.println(s));
        }
    }

    protected List<String> getSystemOut() {
        return systemOut;
    }

    protected List<String> getSystemErr() {
        return systemErr;
    }

    private void validate(ExecutionValidator validator, NinjaContext context, String message) {
        if (validator == null) {
            return;
        }

        try {
            LOG.info("Starting {}", message);
            validator.validate(context);
        } catch (Exception ex) {
            logTestExecutionFail("Validation '" + message + "' failed with exception", ex);
        }
    }

    private void logTestExecutionFail(String message, Exception ex) {
        LOG.error(message, ex);

        AssertJUnit.fail(message + ": " + ex.getMessage());
    }
}
