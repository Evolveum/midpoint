package com.evolveum.midpoint.ninja;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.evolveum.midpoint.ninja.action.Action;
import com.evolveum.midpoint.ninja.impl.Command;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.opts.BaseOptions;
import com.evolveum.midpoint.ninja.opts.ConnectionOptions;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import org.apache.commons.io.FileUtils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) {
        new Main().run(args);
    }

    protected void run(String[] args) {
        JCommander jc = NinjaUtils.setupCommandLineParser();

        try {
            jc.parseWithoutValidation(args);
        } catch (ParameterException ex) {
            System.err.println(ex.getMessage());
            return;
        }

        String parsedCommand = jc.getParsedCommand();

        BaseOptions base = NinjaUtils.getOptions(jc, BaseOptions.class);

        if (base.isVersion()) {
            try {
                Path path = Paths.get(Main.class.getResource("/version").toURI());
                String version = FileUtils.readFileToString(path.toFile(), StandardCharsets.UTF_8);
                System.out.println(version);
            } catch (Exception ex) {
            }
            return;
        }

        if (base.isHelp() || parsedCommand == null) {
            printHelp(jc, parsedCommand);
            return;
        }

        if (base.isVerbose() && base.isSilent()) {
            System.err.println("Cant' use " + BaseOptions.P_VERBOSE + " and " + BaseOptions.P_SILENT
                    + " together (verbose and silent)");
            printHelp(jc, parsedCommand);
            return;
        }

        jc = NinjaUtils.setupCommandLineParser();
        try {
            jc.parse(args);
        } catch (ParameterException ex) {
            System.err.println(ex.getMessage());
            return;
        }

        NinjaContext context = null;
        try {
            ConnectionOptions connection = NinjaUtils.getOptions(jc, ConnectionOptions.class);
            Action action;
            if (connection.isUseWebservice()) {
                action = Command.createRestAction(parsedCommand);
            } else {
                action = Command.createRepositoryAction(parsedCommand);
            }

            if (action == null) {
                String strConnection = connection.isUseWebservice() ? "webservice" : "repository";
                System.err.println("Action for command '" + parsedCommand + "' not found (connection: '"
                        + strConnection + "')");
                return;
            }

            Object options = jc.getCommands().get(parsedCommand).getObjects().get(0);

            context = new NinjaContext(jc);

            action.init(context, options);

            preExecute(context);

            action.execute();

            postExecute(context);
        } catch (Exception ex) {
            handleException(base, ex);
        } finally {
            cleanupResources(base, context);
        }
    }

    protected void preExecute(NinjaContext context) {
        // intentionally left out empty
    }

    protected void postExecute(NinjaContext context) {
        // intentionally left out empty
    }

    private void cleanupResources(BaseOptions opts, NinjaContext context) {
        try {
            if (context != null) {
                context.destroy();
            }
        } catch (Exception ex) {
            if (opts.isVerbose()) {
                String stack = NinjaUtils.printStackToString(ex);

                System.err.print("Unexpected exception occurred (" + ex.getClass()
                        + ") during destroying context. Exception stack trace:\n" + stack);
            }
        }
    }

    private void handleException(BaseOptions opts, Exception ex) {
        if (!opts.isSilent()) {
            System.err.println("Unexpected exception occurred (" + ex.getClass() + "), reason: " + ex.getMessage());
        }

        if (opts.isVerbose()) {
            String stack = NinjaUtils.printStackToString(ex);

            System.err.print("Exception stack trace:\n" + stack);
        }
    }

    private void printHelp(JCommander jc, String parsedCommand) {
        if (parsedCommand == null) {
            jc.usage();
        } else {
            jc.usage(parsedCommand);
        }
    }
}
