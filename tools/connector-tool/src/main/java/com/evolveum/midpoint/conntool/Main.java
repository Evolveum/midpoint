/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.conntool;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

public class Main {

    public static void main(String[] args) {
        new Main().run(args);
    }

    private void run(String[] args) {
        JCommander jc = setupCommandLineParser();

        try {
            jc.parse(args);
        } catch (ParameterException ex) {
            System.err.println(ex.getMessage());
            printHelp(jc, null);
            return;
        }

        String parsedCommand = jc.getParsedCommand();
        CommonOptions commonOptions = getOptions(jc, CommonOptions.class);

        if (commonOptions.isVersion()) {
            printVersion(commonOptions);
            return;
        }

        if (commonOptions.isHelp() || parsedCommand == null) {
            printHelp(jc, parsedCommand);
            return;
        }

        if (commonOptions.isVerbose() && commonOptions.isSilent()) {
            System.err.println("Cant' use " + CommonOptions.P_VERBOSE + " and " + CommonOptions.P_SILENT
                    + " together (verbose and silent)");
            printHelp(jc, parsedCommand);
            return;
        }

        Context context = new Context(commonOptions);
        try {
            context.init();
            Object commandOptions = jc.getCommands().get(parsedCommand).getObjects().get(0);
            if (commandOptions instanceof GenerateDocumentationOptions) {
                new DocumentationGenerator(context, (GenerateDocumentationOptions) commandOptions)
                        .generate();
            } else {
                System.err.println("Unknown command: " + parsedCommand);
                printHelp(jc, parsedCommand);
            }
        } catch (Exception ex) {
            handleException(commonOptions, ex);
        } finally {
            cleanupResources(commonOptions, context);
        }
    }

    private void printVersion(CommonOptions options) {
        try {
            URL resource = requireNonNull(Main.class.getResource("/version"));
            try (InputStream inputStream = resource.openStream()) {
                IOUtils.readLines(inputStream, Charset.defaultCharset())
                        .forEach(System.out::println);
            }
        } catch (Exception ex) {
            handleException(options, ex);
        }
    }

    private void cleanupResources(CommonOptions opts, Context context) {
        try {
            if (context != null) {
                context.destroy();
            }
        } catch (Exception ex) {
            if (opts.isVerbose()) {
                System.err.print("Unexpected exception occurred (" + ex.getClass()
                        + ") during destroying context. Exception stack trace:\n" + printStackToString(ex));
            }
        }
    }

    private void handleException(CommonOptions opts, Exception ex) {
        if (!opts.isSilent()) {
            System.err.println("Unexpected exception occurred (" + ex.getClass() + "), reason: " + ex.getMessage());
        }

        if (opts.isVerbose()) {
            System.err.print("Exception stack trace:\n" + printStackToString(ex));
        }
    }

    private void printHelp(JCommander jc, String parsedCommand) {
        if (parsedCommand != null) {
            jc.getUsageFormatter().usage(parsedCommand);
        }
        jc.usage();
    }

    private static JCommander setupCommandLineParser() {
        CommonOptions base = new CommonOptions();

        JCommander.Builder builder = JCommander.newBuilder()
                .expandAtSign(false)
                .addObject(base);

        builder.addCommand("generate-documentation", new GenerateDocumentationOptions());

        JCommander jc = builder.build();
        jc.setProgramName("java -jar connector-tool.jar");
        jc.setColumnSize(150);

        return jc;
    }

    @NotNull
    private static <T> T getOptions(JCommander jc, Class<T> type) {
        List<Object> objects = jc.getObjects();
        for (Object object : objects) {
            if (type.equals(object.getClass())) {
                //noinspection unchecked
                return (T) object;
            }
        }
        throw new IllegalStateException("No options of type " + type);
    }

    private static String printStackToString(Exception ex) {
        if (ex == null) {
            return null;
        }

        StringWriter writer = new StringWriter();
        ex.printStackTrace(new PrintWriter(writer));

        return writer.toString();
    }
}
