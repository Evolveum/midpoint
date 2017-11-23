package com.evolveum.midpoint.ninja;

import com.beust.jcommander.JCommander;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.opts.ConnectionOptions;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Created by Viliam Repan (lazyman).
 */
public class BaseTest {

    private static final Logger LOG = LoggerFactory.getLogger(BaseTest.class);

    private static final File TARGET_HOME = new File("./target/home");

    @BeforeMethod
    public final void beforeMethod(Method method) throws Exception {
        LOG.info(">>>>>>>>>>>>>>>> Start " + method.getDeclaringClass().getSimpleName() + "."
                + method.getName() + "<<<<<<<<<<<<<<<<<<<");

        beforeMethodInternal(method);
    }

    @AfterMethod
    public final void afterMethod(Method method) throws Exception {
        afterMethodInternal(method);
        LOG.info(">>>>>>>>>>>>>>>> Finished " + method.getDeclaringClass().getSimpleName() + "."
                + method.getName() + "<<<<<<<<<<<<<<<<<<<");
    }

    protected void beforeMethodInternal(Method method) throws Exception {

    }

    protected void afterMethodInternal(Method method) throws Exception {

    }

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

        Main main = new Main() {

            @Override
            protected void preExecute(NinjaContext context) {
                try {
                    if (preExecutionValidator != null) {
                        LOG.info(">>>>>>>>>>>>>>>> Starting pre execution validation");
                        preExecutionValidator.validate(context);
                    }
                } catch (Exception ex) {
                    logTestExecutionFail("Pre execution test failed with exception", ex);
                }
            }

            @Override
            protected void postExecute(NinjaContext context) {
                try {
                    if (postExecutionValidator != null) {
                        LOG.info(">>>>>>>>>>>>>>>> Starting post execution validation");
                        postExecutionValidator.validate(context);
                    }
                } catch (Exception ex) {
                    logTestExecutionFail("Post execution test failed with exception", ex);
                }
            }
        };

        main.run(args);
    }

    private void logTestExecutionFail(String message, Exception ex) {
        LOG.error(message, ex);

        AssertJUnit.fail(message + ": " + ex.getMessage());
    }
}
