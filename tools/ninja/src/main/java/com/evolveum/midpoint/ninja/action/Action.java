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

    protected LogTarget getInfoLogTarget() {
        return LogTarget.SYSTEM_OUT;
    }

    protected void handleResultOnFinish(OperationStatus operation, String finishMessage) {
        operation.finish();

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
