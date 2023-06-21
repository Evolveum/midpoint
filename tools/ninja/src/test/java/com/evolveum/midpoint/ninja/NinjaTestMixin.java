package com.evolveum.midpoint.ninja;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.ninja.action.Action;
import com.evolveum.midpoint.ninja.impl.ActionStateListener;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public interface NinjaTestMixin {

    @FunctionalInterface
    interface BiFunctionThrowable<T, U, R> {

        R apply(T t, U u) throws Exception;
    }

    default <O, R, A extends Action<O, R>> R executeAction(
            @NotNull Class<A> actionClass, @NotNull O actionOptions, @NotNull List<Object> options)
            throws Exception {

        return executeAction(actionClass, actionOptions, options, null, null);
    }

    default <O, R, A extends Action<O, R>> R executeAction(
            @NotNull Class<A> actionClass, @NotNull O actionOptions, @NotNull List<Object> options,
            @Nullable Consumer<List<String>> validateOut, @Nullable Consumer<List<String>> validateErr)
            throws Exception {

        return executeWithWrappedPrintStreams((out, err) -> {

            A action = actionClass.getConstructor().newInstance();
            try (NinjaContext context = new NinjaContext(out, err, options, action.getApplicationContextLevel(options))) {
                action.init(context, actionOptions);

                return action.execute();
            }
        }, validateOut, validateErr);
    }

    private <R> R executeWithWrappedPrintStreams(
            @NotNull BiFunctionThrowable<PrintStream, PrintStream, R> function, @Nullable Consumer<List<String>> validateOut,
            @Nullable Consumer<List<String>> validateErr)
            throws Exception {

        ByteArrayOutputStream bosOut = new ByteArrayOutputStream();
        ByteArrayOutputStream bosErr = new ByteArrayOutputStream();
        try (
                PrintStream out = new PrintStream(bosOut);
                PrintStream err = new PrintStream(bosErr)
        ) {

            return function.apply(out, err);
        } finally {
            processTestOutputStream(bosOut, validateOut, "OUT");
            processTestOutputStream(bosErr, validateErr, "ERR");
        }
    }

    default void executeTest(
            @NotNull String[] args, @Nullable Consumer<List<String>> validateOut, @Nullable Consumer<List<String>> validateErr,
            @Nullable ActionStateListener actionStateListener)
            throws Exception {

        executeWithWrappedPrintStreams((out, err) -> {

            Main main = new Main();

            main.setActionStateListener(actionStateListener);
            main.setOut(out);
            main.setErr(err);

            main.run(args);

            return null;
        }, validateOut, validateErr);
    }

    private void processTestOutputStream(
            ByteArrayOutputStream bos, Consumer<List<String>> validator, String prefix)
            throws IOException {

        final Trace logger = TraceManager.getTrace(getClass());

        List<String> lines = IOUtils.readLines(new ByteArrayInputStream(bos.toByteArray()), StandardCharsets.UTF_8);
        if (logger.isDebugEnabled()) {
            lines.forEach(line -> logger.debug("{}: {}", prefix, line));
        }

        if (validator != null) {
            validator.accept(lines);
        }
    }
}
