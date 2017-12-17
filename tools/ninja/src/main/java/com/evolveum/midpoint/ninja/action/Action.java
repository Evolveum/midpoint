package com.evolveum.midpoint.ninja.action;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.Encoder;
import com.evolveum.midpoint.ninja.impl.LogTarget;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.opts.BaseOptions;
import com.evolveum.midpoint.ninja.opts.ConnectionOptions;
import com.evolveum.midpoint.ninja.util.CountStatus;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.schema.result.OperationResult;
import org.slf4j.LoggerFactory;

/**
 * Created by Viliam Repan (lazyman).
 */
public abstract class Action<T> {

    private static final String LOGGER_SYS_OUT = "SYSOUT";

    private static final String LOGGER_SYS_ERR = "SYSERR";

    protected NinjaContext context;

    protected T options;

    private Logger infoLogger;
    private Logger errorLogger;

    public void init(NinjaContext context, T options) {
        this.context = context;
        this.options = options;

        LogTarget target = getInfoLogTarget();
        setupLogging(target);

        ConnectionOptions connection = NinjaUtils.getOptions(context.getJc(), ConnectionOptions.class);
        context.init(connection);
    }

    protected LogTarget getInfoLogTarget() {
        return LogTarget.SYSTEM_OUT;
    }

    private void setupLogging(LogTarget target) {
        BaseOptions opts = NinjaUtils.getOptions(context.getJc(), BaseOptions.class);

        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        PatternLayoutEncoder ple = new PatternLayoutEncoder();

        if (opts.isVerbose()) {
            ple.setPattern("%date [%thread] %-5level \\(%logger{46}\\): %message%n<");
        } else {
            ple.setPattern("%msg%n");
        }

        ple.setContext(lc);
        ple.start();

        ConsoleAppender out = setupAppender("System.out", lc, ple);
        ConsoleAppender err = setupAppender("System.err", lc, ple);

        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        if (LogTarget.SYSTEM_OUT.equals(target)) {
            root.addAppender(out);
        } else {
            root.addAppender(err);
        }

        root.setLevel(Level.OFF);

        infoLogger = setupLogger(LOGGER_SYS_OUT, opts);

        errorLogger = setupLogger(LOGGER_SYS_ERR, opts);
        errorLogger.setAdditive(false);
        errorLogger.addAppender(err);
    }

    private Logger setupLogger(String name, BaseOptions opts) {
        Logger logger = (Logger) LoggerFactory.getLogger(name);

        if (opts.isSilent()) {
            logger.setLevel(Level.OFF);
        } else if (opts.isVerbose()) {
            logger.setLevel(Level.DEBUG);
        } else {
            logger.setLevel(Level.INFO);
        }

        return logger;
    }

    private ConsoleAppender setupAppender(String target, LoggerContext ctx, Encoder enc) {
        ConsoleAppender appender = new ConsoleAppender();
        appender.setTarget(target);
        appender.setContext(ctx);
        appender.setEncoder(enc);

        appender.start();

        return appender;
    }

    protected void logError(String message, Object... args) {
        logError(message, null, args);
    }

    protected void logError(String message, Exception ex, Object... args) {
        errorLogger.error(message, args);

        BaseOptions opts = NinjaUtils.getOptions(context.getJc(), BaseOptions.class);
        if (opts.isVerbose()) {
            errorLogger.error("Exception details", ex);
        }
    }

    protected void logDebug(String message, Object... args) {
        infoLogger.debug(message, args);
    }

    protected void logInfo(String message, Object... args) {
        infoLogger.info(message, args);
    }

    protected void handleResultOnFinish(OperationResult result, CountStatus status, String finishMessage) {
        result.recomputeStatus();

        if (result.isAcceptable()) {
            if (status == null) {
                logInfo("{}", finishMessage);
            } else {
                logInfo("{}. Processed: {} objects, avg. {}ms",
                        finishMessage, status.getCount(), NinjaUtils.DECIMAL_FORMAT.format(status.getAvg()));
            }
        } else {
            if (status == null) {
                logError("{}", finishMessage);
            } else {
                logError("{} with some problems, reason: {}. Processed: {}, avg. {}ms", finishMessage,
                        result.getMessage(), status.getCount(), NinjaUtils.DECIMAL_FORMAT.format(status.getAvg()));
            }

            if (context.isVerbose()) {
                logError("Full result\n{}", result.debugDumpLazily());
            }
        }
    }

    protected void logCountProgress(CountStatus status) {
        if (status.getLastPrintout() + NinjaUtils.COUNT_STATUS_LOG_INTERVAL > System.currentTimeMillis()) {
            return;
        }

        logInfo("Processed: {}, skipped: {}, avg: {}ms",
                status.getCount(), status.getSkipped(), NinjaUtils.DECIMAL_FORMAT.format(status.getAvg()));

        status.lastPrintoutNow();
    }

    public abstract void execute() throws Exception;
}
