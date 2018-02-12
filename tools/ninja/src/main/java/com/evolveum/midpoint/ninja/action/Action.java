package com.evolveum.midpoint.ninja.action;

import com.evolveum.midpoint.ninja.impl.LogTarget;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.opts.ConnectionOptions;
import com.evolveum.midpoint.ninja.util.CountStatus;
import com.evolveum.midpoint.ninja.util.Log;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RelationalValueSearchQuery;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import java.util.Collection;

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

    // deduplicate with WebModelServiceUtils.addIncludeOptionsForExportOrView
    protected static void addIncludeOptionsForExport(Collection<SelectorOptions<GetOperationOptions>> options,
            Class<? extends ObjectType> type) {
        // todo fix this brutal hack (related to checking whether to include particular options)
        boolean all = type == null
                || Objectable.class.equals(type)
                || com.evolveum.prism.xml.ns._public.types_3.ObjectType.class.equals(type)
                || ObjectType.class.equals(type);

        if (all || UserType.class.isAssignableFrom(type)) {
            options.add(SelectorOptions.create(UserType.F_JPEG_PHOTO,
                    GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));
        }
        if (all || LookupTableType.class.isAssignableFrom(type)) {
            options.add(SelectorOptions.create(LookupTableType.F_ROW,
                    GetOperationOptions.createRetrieve(
                            new RelationalValueSearchQuery(
                                    ObjectPaging.createPaging(PrismConstants.T_ID, OrderDirection.ASCENDING)))));
        }
        if (all || AccessCertificationCampaignType.class.isAssignableFrom(type)) {
            options.add(SelectorOptions.create(AccessCertificationCampaignType.F_CASE,
                    GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));
        }
    }

}
