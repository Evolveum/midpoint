package com.evolveum.midpoint.ninja.action;

import com.evolveum.midpoint.ninja.action.worker.ExportConfigurationWorker;
import com.evolveum.midpoint.ninja.action.worker.ExportConsumerWorker;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

public class ExportConfigurationAction extends AbstractRepositorySearchAction<ExportOptions, Void>{

    @Override
    protected Callable<Void> createConsumer(BlockingQueue<ObjectType> queue, OperationStatus operation) {
        return () -> {
            new ExportConfigurationWorker(context, options, queue, operation).run();
            return null;
        };
    }

    @Override
    public String getOperationName() {
        return "export-configuration";
    }

    @Override
    protected Iterable<ObjectTypes> supportedObjectTypes() {
        return List.of(ObjectTypes.SYSTEM_CONFIGURATION,
            ObjectTypes.ARCHETYPE,
            ObjectTypes.RESOURCE,
            ObjectTypes.CONNECTOR
        );
    }
}
