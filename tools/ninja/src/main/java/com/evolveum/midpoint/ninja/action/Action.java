/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.ninja.impl.LogTarget;
import com.evolveum.midpoint.ninja.impl.NinjaApplicationContextLevel;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.impl.Log;
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

        this.log = context.initializeLogging(getLogTarget());
    }

    public void destroy() {

    }

    public LogTarget getLogTarget() {
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

    @NotNull
    public NinjaApplicationContextLevel getApplicationContextLevel(List<Object> allOptions) {
        return NinjaApplicationContextLevel.FULL_REPOSITORY;
    }

    public abstract R execute() throws Exception;
}
