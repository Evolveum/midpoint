/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.fusesource.jansi.AnsiConsole;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.init.StartupConfiguration;
import com.evolveum.midpoint.ninja.action.Action;
import com.evolveum.midpoint.ninja.action.ActionResult;
import com.evolveum.midpoint.ninja.action.BaseOptions;
import com.evolveum.midpoint.ninja.action.ConnectionOptions;
import com.evolveum.midpoint.ninja.impl.Command;
import com.evolveum.midpoint.ninja.impl.LogLevel;
import com.evolveum.midpoint.ninja.impl.NinjaApplicationContextLevel;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.util.ConsoleFormat;
import com.evolveum.midpoint.ninja.util.InputParameterException;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.util.exception.SystemException;

public class Main {

    public static void main(String[] args) {
        MainResult<?> result = new Main().run(args);

        int exitCode = result.exitCode();
        if (exitCode != 0) {
            String exitMessage = result.exitMessage();
            if (exitMessage != null) {
                System.err.println(exitMessage);
            }
            System.exit(exitCode);
        }
    }

    private PrintStream out = System.out;

    private PrintStream err = System.err;

    public PrintStream getOut() {
        return out;
    }

    public void setOut(@NotNull PrintStream out) {
        this.out = out;
    }

    @SuppressWarnings("unused")
    public PrintStream getErr() {
        return err;
    }

    public void setErr(@NotNull PrintStream err) {
        this.err = err;
    }

    protected @NotNull MainResult<?> run(String[] args) {
        AnsiConsole.systemInstall();

        try {
            return runInternal(args);
        } finally {
            out.flush();
            err.flush();

            AnsiConsole.systemUninstall();
        }
    }

    private <T> @NotNull MainResult<?> runInternal(String[] args) {
        JCommander jc = NinjaUtils.setupCommandLineParser();

        try {
            jc.parse(args);
        } catch (ParameterException ex) {
            err.println(ConsoleFormat.formatLogMessage(LogLevel.ERROR, ex.getMessage()));

            String parsedCommand = ex.getJCommander().getParsedCommand();
            BaseOptions base = NinjaUtils.getOptions(jc.getObjects(), BaseOptions.class);
            if (base != null && base.isVerbose()) {
                printHelp(jc, parsedCommand);
            }

            return MainResult.EMPTY_ERROR;
        }

        String parsedCommand = jc.getParsedCommand();

        BaseOptions base = Objects.requireNonNull(NinjaUtils.getOptions(jc.getObjects(), BaseOptions.class));

        ConsoleFormat.setBatchMode(base.isBatchMode());

        if (base.isVerbose() && base.isSilent()) {
            err.println(ConsoleFormat.formatLogMessage(
                    LogLevel.ERROR, "Can't use " + BaseOptions.P_VERBOSE + " and " + BaseOptions.P_SILENT + " together (verbose and silent)"));

            printHelp(jc, parsedCommand);

            return MainResult.EMPTY_ERROR;
        }

        if (BooleanUtils.isTrue(base.isVersion())) {
            printVersion(base.isVerbose());
            return MainResult.EMPTY_SUCCESS;
        }

        if (base.isHelp() || parsedCommand == null) {
            printHelp(jc, parsedCommand);
            return MainResult.EMPTY_SUCCESS;
        }

        NinjaContext context = null;
        try {
            Action<T, ?> action = Command.createAction(parsedCommand);

            if (action == null) {
                err.println(ConsoleFormat.formatLogMessage(LogLevel.ERROR, "Action for command '" + parsedCommand + "' not found"));
                return MainResult.EMPTY_ERROR;
            }

            //noinspection unchecked
            T options = (T) jc.getCommands().get(parsedCommand).getObjects().get(0);

            List<Object> allOptions = new ArrayList<>(jc.getObjects());
            allOptions.add(options);

            context = new NinjaContext(out, err, allOptions, action.getApplicationContextLevel(allOptions));

            try {
                action.init(context, options);

                String startMessage = ConsoleFormat.formatActionStartMessage(action);
                if (startMessage != null) {
                    context.getLog().info("");
                    context.getLog().info(startMessage);
                    context.getLog().info("");
                }

                NinjaApplicationContextLevel contextLevel = action.getApplicationContextLevel(allOptions);
                if (contextLevel != NinjaApplicationContextLevel.NONE) {
                    ConnectionOptions connectionOptions =
                            Objects.requireNonNullElse(context.getOptions(ConnectionOptions.class), new ConnectionOptions());
                    checkAndWarnMidpointHome(connectionOptions.getMidpointHome());
                }

                Object result = action.execute();
                if (result instanceof ActionResult<?> actionResult) {
                    return new MainResult<>(actionResult.result(), actionResult.exitCode(), actionResult.exitMessage());
                }

                return new MainResult<>(result);
            } finally {
                action.destroy();
            }
        } catch (InputParameterException ex) {
            err.println(ConsoleFormat.formatLogMessage(LogLevel.ERROR, ex.getMessage()));

            int exitCode = ex.getExitCode() != null ? ex.getExitCode() : MainResult.DEFAULT_EXIT_CODE_ERROR;
            return new MainResult<>(ex.getMessage(), exitCode);
        } catch (Exception ex) {
            handleException(base, ex);

            return MainResult.EMPTY_ERROR;
        } finally {
            cleanupResources(base, context);
        }
    }

    private void checkAndWarnMidpointHome(String midpointHome) {
        if (StringUtils.isEmpty(midpointHome)) {
            throw new InputParameterException("Midpoint home " + ConnectionOptions.P_MIDPOINT_HOME + " option expected, but not defined");
        }

        File file = new File(midpointHome);
        if (!file.exists() || !file.isDirectory()) {
            throw new InputParameterException("Midpoint home directory '" + midpointHome + "' doesn't exist or is not a directory");
        }

        String configFile = StartupConfiguration.DEFAULT_CONFIG_FILE_NAME;
        if (System.getProperty(MidpointConfiguration.MIDPOINT_CONFIG_FILE_PROPERTY) != null) {
            configFile = System.getProperty(MidpointConfiguration.MIDPOINT_CONFIG_FILE_PROPERTY);
        }

        File config = new File(file, configFile);
        if (!config.exists() || config.isDirectory()) {
            throw new InputParameterException("Midpoint home config xml file '" + config.getAbsolutePath() + "' doesn't exist");
        }
    }

    private void cleanupResources(BaseOptions opts, NinjaContext context) {
        try {
            if (context != null) {
                context.close();
            }
        } catch (Exception ex) {
            printException("Unexpected exception occurred (" + ex.getClass() + ") during destroying context", ex, opts.isVerbose());
        }
    }

    private Exception checkAndUnwrapException(Exception ex) {
        Throwable throwable = ex;
        while (throwable != null && !Objects.equals(throwable.getCause(), throwable)) {
            throwable = throwable.getCause();
            if (throwable instanceof SystemException) {
                break;
            }
        }
        if (throwable instanceof SystemException && throwable.getMessage().contains("repository context")) {
            ex = (Exception) throwable;
        }

        return ex;
    }

    private void handleException(BaseOptions opts, Exception ex) {
        if (!opts.isSilent()) {
            ex = checkAndUnwrapException(ex);

            err.println(ConsoleFormat.formatLogMessage(
                    LogLevel.ERROR, "Unexpected exception occurred (" + ex.getClass() + "), reason: " + ex.getMessage()));
        }

        if (opts.isVerbose()) {
            String stack = NinjaUtils.printStackToString(ex);

            err.println(ConsoleFormat.formatLogMessage(LogLevel.DEBUG, "Exception stack trace:\n" + stack));
        }
    }

    private void printVersion(boolean verbose) {
        URL url = Main.class.getResource("/version-long");
        if (url == null) {
            err.println(ConsoleFormat.formatLogMessage(LogLevel.ERROR, "Couldn't obtain version. Version file not available."));
            return;
        }

        try (InputStream is = url.openStream()) {
            String version = IOUtils.toString(is, StandardCharsets.UTF_8).trim();
            out.println(version);
        } catch (Exception ex) {
            printException("Couldn't obtain version", ex, verbose);
        }
    }

    private void printException(String message, Exception ex, boolean verbose) {
        String msg = StringUtils.isNotEmpty(message) ? message + ", reason: " : "";
        msg += ex.getMessage();

        err.println(ConsoleFormat.formatLogMessage(LogLevel.ERROR, msg));
        if (verbose) {
            String stack = NinjaUtils.printStackToString(ex);

            err.println(ConsoleFormat.formatLogMessage(LogLevel.DEBUG, "Exception stack trace:\n" + stack));
        }
    }

    private void printHelp(JCommander jc, String parsedCommand) {
        String help = NinjaUtils.createHelp(jc, parsedCommand);

        out.println(help);
    }
}
