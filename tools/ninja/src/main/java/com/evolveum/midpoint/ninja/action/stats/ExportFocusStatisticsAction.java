package com.evolveum.midpoint.ninja.action.stats;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

import com.evolveum.midpoint.ninja.action.AbstractRepositorySearchAction;
import com.evolveum.midpoint.ninja.action.ExportOptions;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class ExportFocusStatisticsAction extends AbstractRepositorySearchAction<ExportOptions, Void> {
    @Override
    protected Callable<Void> createConsumer(BlockingQueue<ObjectType> queue, OperationStatus operation) {
        return () -> {
            new ExportFocusStatisticsWorker(context, options, queue, operation, new StatsCounter()).run();
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
