/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja;

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

import com.evolveum.midpoint.ninja.action.Action;
import com.evolveum.midpoint.ninja.action.ActionResult;
import com.evolveum.midpoint.ninja.action.BaseOptions;
import com.evolveum.midpoint.ninja.impl.Command;
import com.evolveum.midpoint.ninja.impl.LogLevel;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.util.ConsoleFormat;
import com.evolveum.midpoint.ninja.util.InputParameterException;
import com.evolveum.midpoint.ninja.util.NinjaUtils;

public class Main {

    public static void main(String[] args) {
        MainResult<?> result = new Main().run(args);

        int exitCode = result.exitCode();
        if (exitCode != 0) {
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

    public PrintStream getErr() {
        return err;
    }

    public void setErr(@NotNull PrintStream err) {
        this.err = err;
    }

    protected <T> @NotNull MainResult<?> run(String[] args) {
        AnsiConsole.systemInstall();

        JCommander jc = NinjaUtils.setupCommandLineParser();

        try {
            jc.parse(args);
        } catch (ParameterException ex) {
            err.println(ConsoleFormat.formatLogMessage(LogLevel.ERROR, ex.getMessage()));

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

                context.getLog().info("");
                String startMessage = ConsoleFormat.formatActionStartMessage(action);
                if (startMessage != null) {
                    context.getLog().info(startMessage);
                    context.getLog().info("");
                }

                Object result = action.execute();
                if (result instanceof ActionResult) {
                    ActionResult<?> actionResult = (ActionResult<?>) result;
                    return new MainResult<>(actionResult.result(), actionResult.exitCode());
                }

                return new MainResult<>(result);
            } finally {
                action.destroy();
            }
        } catch (InputParameterException ex) {
            err.println(ConsoleFormat.formatLogMessage(LogLevel.ERROR, ex.getMessage()));

            return MainResult.EMPTY_ERROR;
        } catch (Exception ex) {
            handleException(base, ex);

            return MainResult.EMPTY_ERROR;
        } finally {
            cleanupResources(base, context);

            AnsiConsole.systemUninstall();
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

    private void handleException(BaseOptions opts, Exception ex) {
        if (!opts.isSilent()) {
            err.println(ConsoleFormat.formatLogMessage(
                    LogLevel.ERROR, "Unexpected exception occurred (" + ex.getClass() + "), reason: " + ex.getMessage()));
        }

        if (opts.isVerbose()) {
            String stack = NinjaUtils.printStackToString(ex);

            err.println(ConsoleFormat.formatLogMessage(LogLevel.DEBUG, "Exception stack trace:\n" + stack));
        }
    }

    private void printVersion(boolean verbose) {
        URL url = Main.class.getResource("/version");
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
