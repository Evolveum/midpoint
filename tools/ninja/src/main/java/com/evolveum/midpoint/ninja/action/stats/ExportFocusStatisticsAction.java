package com.evolveum.midpoint.ninja.action;

import com.evolveum.midpoint.ninja.action.worker.ExportConfigurationWorker;
import com.evolveum.midpoint.ninja.action.worker.ExportFocusStatisticsWorker;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

public class ExportFocusStatisticsAction extends AbstractRepositorySearchAction<ExportOptions, Void>{
    @Override
    protected Callable<Void> createConsumer(BlockingQueue<ObjectType> queue, OperationStatus operation) {
        // FIXME: Here should be statistic counting consumer

        return () -> {
            new ExportFocusStatisticsWorker(context, options, queue, operation).run();
            return null;
        };
    }

    @Override
    protected Iterable<ObjectTypes> supportedObjectTypes() {
        // FIXME: Add types you are interested in
        return List.of(ObjectTypes.USER,
                ObjectTypes.ROLE,
                ObjectTypes.SERVICE,
                ObjectTypes.ORG

        );
    }

    @Override
    public String getOperationName() {
        return "";
    }
}
