package com.evolveum.midpoint.ninja.action;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.ConsoleAppender;
import com.evolveum.midpoint.ninja.impl.LogTarget;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.opts.BaseOptions;
import com.evolveum.midpoint.ninja.opts.ConnectionOptions;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
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

        ConsoleAppender out = new ConsoleAppender();
        out.setTarget("System.out");
        out.setEncoder(ple);

        ConsoleAppender err = new ConsoleAppender();
        err.setTarget("System.err");
        err.setEncoder(ple);

        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        if (opts.isVerbose()) {
            if (LogTarget.SYSTEM_OUT.equals(target)) {
                root.addAppender(out);
            } else {
                root.addAppender(err);
            }
        }

        infoLogger = (Logger) LoggerFactory.getLogger(LOGGER_SYS_OUT);
        infoLogger.setAdditive(false);
        if (LogTarget.SYSTEM_OUT.equals(target)) {
            infoLogger.addAppender(out);
        } else {
            infoLogger.addAppender(err);
        }

        errorLogger = (Logger) LoggerFactory.getLogger(LOGGER_SYS_ERR);
        errorLogger.setAdditive(false);
        errorLogger.addAppender(err);

        if (opts.isSilent()) {
            root.setLevel(Level.OFF);
        } else {
            root.setLevel(Level.INFO);
        }
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

    protected void logInfo(String message, Object... args) {
        infoLogger.info(message, args);
    }

    public abstract void execute() throws Exception;
}
