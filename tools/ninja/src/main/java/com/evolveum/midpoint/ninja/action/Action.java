package com.evolveum.midpoint.ninja.action;

import com.evolveum.midpoint.ninja.impl.LogTarget;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.opts.ConnectionOptions;
import com.evolveum.midpoint.ninja.util.CountStatus;
import com.evolveum.midpoint.ninja.util.Log;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
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

    protected void handleResultOnFinish(OperationResult result, CountStatus status, String finishMessage) {
        result.recomputeStatus();

        if (result.isAcceptable()) {
            if (status == null) {
                log.info("{}", finishMessage);
            } else {
                log.info("{}. Processed: {} objects, avg. {}ms",
                        finishMessage, status.getCount(), NinjaUtils.DECIMAL_FORMAT.format(status.getAvg()));
            }
        } else {
            if (status == null) {
                log.error("{}", finishMessage);
            } else {
                log.error("{} with some problems, reason: {}. Processed: {}, avg. {}ms", finishMessage,
                        result.getMessage(), status.getCount(), NinjaUtils.DECIMAL_FORMAT.format(status.getAvg()));
            }

            if (context.isVerbose()) {
                log.error("Full result\n{}", result.debugDumpLazily());
            }
        }
    }

    protected void logCountProgress(CountStatus status) {
        if (status.getLastPrintout() + NinjaUtils.COUNT_STATUS_LOG_INTERVAL > System.currentTimeMillis()) {
            return;
        }

        log.info("Processed: {}, skipped: {}, avg: {}ms",
                status.getCount(), status.getSkipped(), NinjaUtils.DECIMAL_FORMAT.format(status.getAvg()));

        status.lastPrintoutNow();
    }

    public abstract void execute() throws Exception;
}
