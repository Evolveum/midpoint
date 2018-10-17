/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.ninja.action;

import com.evolveum.midpoint.ninja.impl.LogTarget;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.opts.ConnectionOptions;
import com.evolveum.midpoint.ninja.util.Log;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * Created by Viliam Repan (lazyman).
 */
public abstract class Action<T> {

    protected Log log;

    protected NinjaContext context;

    protected T options;

    public void init(NinjaContext context, T options) {
        this.context = context;
        this.options = options;

        LogTarget target = getInfoLogTarget();
        log = new Log(target, this.context);

        this.context.setLog(log);

        ConnectionOptions connection = NinjaUtils.getOptions(this.context.getJc(), ConnectionOptions.class);
        this.context.init(connection);
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

    public abstract void execute() throws Exception;
}
