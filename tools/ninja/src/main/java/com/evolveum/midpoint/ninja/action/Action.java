/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action;

import com.evolveum.midpoint.ninja.impl.LogTarget;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.opts.BaseOptions;
import com.evolveum.midpoint.ninja.util.Log;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * Base implementation class for action, that is Ninja command.
 *
 * @param <O> options class
 */
public abstract class Action<O, R> {

    protected Log log;

    protected NinjaContext context;

    protected O options;

    public void init(NinjaContext context, O options) {
        this.context = context;
        this.options = options;

        LogTarget target = getInfoLogTarget();
        Log.LogLevel level = getLogLevel(context);
        log = new Log(target, level);

        this.context.setLog(log);
    }

    private Log.LogLevel getLogLevel(NinjaContext context) {
        BaseOptions base = context.getOptions(BaseOptions.class);
        if (base == null) {
            return Log.LogLevel.DEFAULT;
        }

        if (base.isVerbose()) {
            return Log.LogLevel.VERBOSE;
        }

        if (base.isSilent()) {
            return Log.LogLevel.SILENT;
        }

        return Log.LogLevel.DEFAULT;
    }

    public LogTarget getInfoLogTarget() {
        return LogTarget.SYSTEM_OUT;
    }

    protected void handleResultOnFinish(OperationStatus operation, String finishMessage) {
        OperationResult result = operation.getResult();
        result.recomputeStatus();

        if (result.isAcceptable()) {
            log.info("{} in {}s. {}", finishMessage, NinjaUtils.DECIMAL_FORMAT.format(operation.getTotalTime()),
                    operation.print());
        } else {
            log.error("{} in {}s with some problems, reason: {}. {}", finishMessage,
                    NinjaUtils.DECIMAL_FORMAT.format(operation.getTotalTime()), result.getMessage(), operation.print());

            if (context.isVerbose()) {
                log.error("Full result\n{}", result.debugDumpLazily());
            }
        }
    }

    public abstract R execute() throws Exception;
}
